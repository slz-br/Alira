package me.slz_br.alira.emergencia

import android.content.Context
import android.content.Intent
import android.util.Log
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import me.slz_br.alira.data.db.entities.UsuarioEntity
import me.slz_br.alira.data.repository.AliraRepository
import me.slz_br.alira.data.sync.enviarEmergenciaParaCelular
import me.slz_br.alira.presentation.AlertaEmergenciaActivity

private const val TAG = "ALIRA_EMERGENCIA"

private const val SEGUNDOS_PARA_RESPONDER = 30
private const val SEGUNDOS_ENTRE_PERGUNTAS = 60
private const val SEGUNDOS_ESPERANDO_CELULAR = 30

// Mesmos textos usados em MobileDataLayerService.kt no :mobile.
private const val MOTIVO_PEDIU_AJUDA = "PEDIU_AJUDA"
private const val MOTIVO_SEM_RESPOSTA = "SEM_RESPOSTA"

/**
 * Em que ponto a emergência está. A tela de alerta (AlertaEmergenciaActivity)
 * só observa este estado e desenha o que corresponde a ele.
 */
sealed interface EstadoEmergencia {
    data object Inativo : EstadoEmergencia
    data class Perguntando(val tipo: TipoEmergencia, val segundosRestantes: Int, val perfil: PerfilDeAlerta) :
        EstadoEmergencia
    data class EsperandoParaPerguntarDeNovo(val tipo: TipoEmergencia, val segundosRestantes: Int) :
        EstadoEmergencia
    data class AvisandoContato(val tipo: TipoEmergencia) : EstadoEmergencia
    data class ContatoAvisado(val tipo: TipoEmergencia, val contatos: String) : EstadoEmergencia
    data class FalhaAoAvisar(val tipo: TipoEmergencia, val motivo: String) : EstadoEmergencia
    data class PessoaEstaBem(val tipo: TipoEmergencia) : EstadoEmergencia
}

private data class ResultadoDoCelular(val sucesso: Boolean, val detalhe: String)

/**
 * O "coordenador" de uma emergência. Quem detecta o problema (detector de
 * queda, detector de ansiedade, ou o botão de teste nas Configurações) só
 * chama iniciar(); daí em diante tudo acontece aqui, nesta ordem:
 *
 *  1. Registra o alerta no banco (status PENDENTE).
 *  2. Liga os sinais (vibração / alarme / voz, conforme a deficiência) e
 *     mostra a pergunta "Você está bem?" com contagem regressiva de 30 s.
 *     Na crise de ansiedade, se ninguém responder, espera 1 minuto e
 *     pergunta mais uma vez.
 *  3. "Estou bem"       -> encerra e marca o alerta como CANCELADO.
 *     "Preciso de ajuda" -> avisa o contato NA HORA, sem esperar a contagem.
 *     Sem resposta       -> avisa o contato quando a contagem acaba.
 *  4. Para avisar, manda a emergência ao celular, que envia o SMS e
 *     responde se deu certo. O resultado aparece na tela (e o alerta vira
 *     ENVIADO_AO_CUIDADOR ou FALHA_AO_AVISAR).
 *
 * É um "object" (existe um só no app inteiro) para que só uma emergência
 * aconteça por vez e para que a tela, os detectores e o serviço que recebe
 * a resposta do celular falem todos com o mesmo coordenador.
 */
object CentralDeEmergencia {

    // Roda na thread principal: quase tudo aqui é esperar (contagem,
    // resposta da pessoa, resposta do celular), e o TextToSpeech da
    // SinaisDeAlerta precisa ser criado nela. O único trecho que trava a
    // thread (falar com o celular) vai para Dispatchers.IO.
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _estado = MutableStateFlow<EstadoEmergencia>(EstadoEmergencia.Inativo)
    val estado: StateFlow<EstadoEmergencia> = _estado.asStateFlow()

    private var emergencia: Job? = null

    // Dados da emergência que está rodando agora: se é um TESTE (botões
    // das Configurações), o id do alerta no banco e um número que muda a
    // cada emergência nova ("geração"), para uma emergência antiga que foi
    // encerrada não apagar os dados de uma nova.
    @Volatile
    private var emTeste = false

    @Volatile
    private var idAlertaAtual: Long? = null

    private var geracao = 0

    @Volatile
    private var respostaDaPessoa: CompletableDeferred<Boolean>? = null

    @Volatile
    private var respostaDoCelular: CompletableDeferred<ResultadoDoCelular>? = null

    val emAndamento: Boolean
        get() = emergencia?.isActive == true

    // A tela de alerta está aberta na frente da pessoa? Atualizado pela
    // própria tela (telaAbriu / telaSaiu). Serve para não mostrar a
    // notificação POR CIMA da tela — ela só é necessária quando a tela não
    // está visível.
    @Volatile
    private var telaVisivel = false

    /** Chamado pela AlertaEmergenciaActivity quando ela aparece. */
    fun telaAbriu(context: Context) {
        telaVisivel = true
        NotificacaoDeEmergencia.remover(context)
    }

    /**
     * Chamado pela AlertaEmergenciaActivity quando ela sai da frente (ex.: a
     * pessoa deslizou a tela para fechar). Se a pergunta ainda está de pé, a
     * notificação volta, para a pessoa conseguir responder.
     */
    fun telaSaiu(context: Context) {
        telaVisivel = false
        // TESTE: sair da tela encerra o teste na hora (contagem, sinais e
        // espera do celular param), e outro teste já pode ser aberto.
        if (emTeste && emAndamento) {
            encerrarTeste(context.applicationContext)
            return
        }
        val tipoPendente = when (val atual = _estado.value) {
            is EstadoEmergencia.Perguntando -> atual.tipo
            is EstadoEmergencia.EsperandoParaPerguntarDeNovo -> atual.tipo
            else -> null
        }
        if (tipoPendente != null && emAndamento) {
            NotificacaoDeEmergencia.mostrarPergunta(context.applicationContext, tipoPendente)
        }
    }

    /**
     * Começa uma emergência. Se já existe uma em andamento, ignora — não faz
     * sentido perguntar "Você está bem?" duas vezes ao mesmo tempo.
     *
     * @param teste true quando vem do botão "Testar" das Configurações: o
     *              fluxo é o mesmo, mas o SMS sai marcado com [TESTE].
     */
    fun iniciar(context: Context, tipo: TipoEmergencia, teste: Boolean = false) {
        if (emAndamento) {
            Log.i(TAG, "Já existe uma emergência em andamento; ignorando ${tipo.codigo}")
            return
        }
        val app = context.applicationContext
        emTeste = teste
        val minhaGeracao = ++geracao
        emergencia = escopo.launch { executar(app, tipo, teste, minhaGeracao) }
    }

    /** Chamado pelos botões da tela: true = "Estou bem", false = "Preciso de ajuda". */
    fun responder(estouBem: Boolean) {
        respostaDaPessoa?.complete(estouBem)
    }

    /** Chamado pelo WearDataLayerListenerService quando o celular responde. */
    fun receberResultadoDoCelular(sucesso: Boolean, detalhe: String) {
        respostaDoCelular?.complete(ResultadoDoCelular(sucesso, detalhe))
    }

    /** Chamado pela tela quando a pessoa toca em "OK" no final. */
    fun fechar() {
        if (!emAndamento) _estado.value = EstadoEmergencia.Inativo
    }

    /**
     * Encerra um TESTE no meio: cancela a tarefa (o "finally" do executar
     * desliga vibração, alarme e voz), tira a notificação, volta ao estado
     * Inativo e marca o alerta do teste como CANCELADO no banco.
     */
    private fun encerrarTeste(app: Context) {
        // Só vira CANCELADO se o teste ainda não tinha um resultado (a
        // pessoa saiu durante a pergunta ou enquanto avisava o contato).
        val semResultado = when (_estado.value) {
            is EstadoEmergencia.Perguntando,
            is EstadoEmergencia.EsperandoParaPerguntarDeNovo,
            is EstadoEmergencia.AvisandoContato -> true
            else -> false
        }
        emergencia?.cancel()
        emergencia = null
        emTeste = false
        NotificacaoDeEmergencia.remover(app)
        _estado.value = EstadoEmergencia.Inativo
        val id = idAlertaAtual ?: return
        idAlertaAtual = null
        if (!semResultado) return
        escopo.launch {
            AliraRepository.getInstance(app).atualizarStatusAlerta(id, "CANCELADO")
        }
    }

    private suspend fun executar(app: Context, tipo: TipoEmergencia, teste: Boolean, minhaGeracao: Int) {
        val repository = AliraRepository.getInstance(app)
        val usuario = repository.obterUsuarioParaEmergencia()
        // Lê a acessibilidade direto do banco (.first() = o valor atual):
        // com o app fechado, o GerenciadorAcessibilidade pode ainda não ter carregado.
        val acessibilidade = repository.observarConfigAcessibilidade().first()
        val perfil = PerfilDeAlerta.de(usuario?.deficiencia, acessibilidade)
        val descricao = if (teste) "Teste disparado nas Configurações" else null
        val idAlerta = repository.registrarEmergencia(tipo.codigo, descricao)
        idAlertaAtual = idAlerta
        val sinais = SinaisDeAlerta(app, perfil)

        try {
            // ---- 1) Perguntar "Você está bem?" ----
            // true = está bem, false = pediu ajuda, null = não respondeu.
            val resposta = CompletableDeferred<Boolean>()
            respostaDaPessoa = resposta
            var estaBem: Boolean? = null

            for (tentativa in 1..tipo.tentativas) {
                if (tentativa > 1) {
                    // Intervalo entre as perguntas (só na ansiedade). Os
                    // botões continuam na tela, então dá para responder.
                    estaBem = contagemRegressiva(SEGUNDOS_ENTRE_PERGUNTAS, resposta) { s ->
                        EstadoEmergencia.EsperandoParaPerguntarDeNovo(tipo, s)
                    }
                    if (estaBem != null) break
                }

                // O estado precisa estar em "Perguntando" ANTES de abrir a
                // tela, senão ela abre vendo "Inativo" e fecha sozinha.
                _estado.value = EstadoEmergencia.Perguntando(tipo, SEGUNDOS_PARA_RESPONDER, perfil)
                if (!telaVisivel) {
                    NotificacaoDeEmergencia.mostrarPergunta(app, tipo)
                    abrirTela(app)
                }
                sinais.iniciar(tipo.textoFalado)

                estaBem = contagemRegressiva(SEGUNDOS_PARA_RESPONDER, resposta) { s ->
                    if (s % 10 == 0 && s != SEGUNDOS_PARA_RESPONDER) {
                        sinais.falar("Faltam $s segundos para avisar seu contato.")
                    }
                    EstadoEmergencia.Perguntando(tipo, s, perfil)
                }
                sinais.parar()
                if (estaBem != null) break
            }
            respostaDaPessoa = null
            sinais.parar()

            // ---- 2a) A pessoa está bem: encerra e registra ----
            if (estaBem == true) {
                repository.atualizarStatusAlerta(idAlerta, "CANCELADO")
                NotificacaoDeEmergencia.remover(app)
                _estado.value = EstadoEmergencia.PessoaEstaBem(tipo)
                sinais.falar("Que bom que você está bem. O ocorrido foi registrado.")
                if (perfil.usaVoz) delay(4_000) // dá tempo de a voz terminar
                return
            }

            // ---- 2b) Pediu ajuda (false) ou não respondeu (null): avisar ----
            val motivo = if (estaBem == false) MOTIVO_PEDIU_AJUDA else MOTIVO_SEM_RESPOSTA
            _estado.value = EstadoEmergencia.AvisandoContato(tipo)
            NotificacaoDeEmergencia.remover(app)
            sinais.falar("Avisando seu contato de emergência.")

            val resultado = avisarContato(app, tipo, motivo, usuario, teste)

            if (resultado.sucesso) {
                repository.atualizarStatusAlerta(idAlerta, "ENVIADO_AO_CUIDADOR")
                _estado.value = EstadoEmergencia.ContatoAvisado(tipo, resultado.detalhe)
                NotificacaoDeEmergencia.mostrarResultado(app, "Contato avisado", resultado.detalhe)
                sinais.falar("Seu contato foi avisado: ${resultado.detalhe}.")
            } else {
                repository.atualizarStatusAlerta(idAlerta, "FALHA_AO_AVISAR")
                _estado.value = EstadoEmergencia.FalhaAoAvisar(tipo, resultado.detalhe)
                NotificacaoDeEmergencia.mostrarResultado(app, "Não foi possível avisar", resultado.detalhe)
                sinais.falar(
                    "Não consegui avisar seu contato. ${resultado.detalhe} " +
                        "Se precisar, peça ajuda a alguém por perto."
                )
            }
            if (perfil.usaVoz) delay(8_000) // dá tempo de a voz terminar
        } catch (erro: CancellationException) {
            throw erro // cancelamento normal de coroutine: não é erro
        } catch (erro: Exception) {
            // Qualquer erro inesperado aqui não pode derrubar o app no meio
            // de uma emergência: registra e mostra a falha na tela.
            Log.e(TAG, "Erro inesperado durante a emergência", erro)
            repository.atualizarStatusAlerta(idAlerta, "FALHA_AO_AVISAR")
            _estado.value = EstadoEmergencia.FalhaAoAvisar(tipo, "Ocorreu um erro no relógio.")
        } finally {
            // Só limpa os "envelopes" se nenhuma emergência nova começou
            // depois desta (ver "geracao" lá em cima).
            if (minhaGeracao == geracao) {
                respostaDaPessoa = null
                respostaDoCelular = null
                idAlertaAtual = null
            }
            sinais.encerrar()
        }
    }

    /**
     * Conta de [segundos] até 1, atualizando a tela a cada segundo. Para
     * antes do fim se a pessoa responder. Devolve a resposta (true/false)
     * ou null se o tempo acabou sem resposta.
     */
    private suspend fun contagemRegressiva(
        segundos: Int,
        resposta: CompletableDeferred<Boolean>,
        estadoNoSegundo: (Int) -> EstadoEmergencia,
    ): Boolean? {
        for (s in segundos downTo 1) {
            _estado.value = estadoNoSegundo(s)
            val respondeu = withTimeoutOrNull(1_000L) { resposta.await() }
            if (respondeu != null) return respondeu
        }
        return null
    }

    /** Manda a emergência ao celular e espera ele dizer se o SMS saiu. */
    private suspend fun avisarContato(
        app: Context,
        tipo: TipoEmergencia,
        motivo: String,
        usuario: UsuarioEntity?,
        teste: Boolean,
    ): ResultadoDoCelular {
        val aguardando = CompletableDeferred<ResultadoDoCelular>()
        respostaDoCelular = aguardando

        val campos = listOf(
            tipo.codigo,
            motivo,
            usuario?.email.orEmpty(),
            usuario?.nome.orEmpty(),
            System.currentTimeMillis().toString(),
            if (teste) "1" else "0",
        )
        val entregue = withContext(Dispatchers.IO) { enviarEmergenciaParaCelular(app, campos) }
        if (!entregue) {
            return ResultadoDoCelular(false, "O relógio não está conectado ao celular.")
        }

        return withTimeoutOrNull(SEGUNDOS_ESPERANDO_CELULAR * 1_000L) { aguardando.await() }
            ?: ResultadoDoCelular(false, "O celular não respondeu a tempo.")
    }

    private fun abrirTela(app: Context) {
        try {
            app.startActivity(
                Intent(app, AlertaEmergenciaActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (erro: Exception) {
            // Com o app fechado o Android pode bloquear a abertura direta da
            // tela; nesse caso a notificação (full screen intent) faz o papel.
            Log.w(TAG, "Não foi possível abrir a tela de alerta diretamente", erro)
        }
    }
}
