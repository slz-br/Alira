package me.slz_br.alira.data.repository

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.slz_br.alira.acessibilidade.ConfigAcessibilidade
import me.slz_br.alira.acessibilidade.GerenciadorAcessibilidade
import me.slz_br.alira.data.db.AliraDatabase
import me.slz_br.alira.data.db.Hashing
import me.slz_br.alira.data.db.entities.AcessibilidadeEntity
import me.slz_br.alira.data.db.entities.AlertaEntity
import me.slz_br.alira.data.db.entities.CuidadorEntity
import me.slz_br.alira.data.db.entities.MonitoramentoEntity
import me.slz_br.alira.data.db.entities.RelatorioEntity
import me.slz_br.alira.data.db.entities.UsuarioEntity
import me.slz_br.alira.data.sync.enviarAlertaParaCelular
import me.slz_br.alira.data.sync.enviarLeiturasParaCelular
import me.slz_br.alira.emergencia.PerfilDeAlerta
import me.slz_br.alira.monitoramento.PreferenciasMonitoramento

/** Nomes dos 3 recursos de acessibilidade já existentes na tela de configurações. */
object RecursosAcessibilidade {
    const val FONTE_ALTO_CONTRASTE = "Fonte de alto contraste"
    const val SINAIS_VIBRACAO = "Sinais de vibração"
    const val TEXTO_PARA_VOZ = "Texto para Voz"

    val PADRAO = listOf(FONTE_ALTO_CONTRASTE, SINAIS_VIBRACAO, TEXTO_PARA_VOZ)

    /** Texto curto que aparece embaixo de cada botão na tela de Acessibilidade. */
    fun descricao(nome: String): String = when (nome) {
        FONTE_ALTO_CONTRASTE -> "Fundo preto, letras maiores e amarelas"
        SINAIS_VIBRACAO -> "Vibra ao tocar e vibra forte nos alertas"
        TEXTO_PARA_VOZ -> "Fala o botão tocado e lê os valores"
        else -> ""
    }

    /**
     * Linhas iniciais da tabela acessibilidade para um usuário novo. Com
     * [deficiencia] (o texto do cadastro, ex.: "AUDITIVA,VISUAL"), já liga
     * o que combina com ela: visual -> alto contraste + voz; auditiva ->
     * vibração. A pessoa pode desligar depois na tela de Acessibilidade.
     */
    fun linhasIniciais(usuarioId: Long, deficiencia: String? = null): List<AcessibilidadeEntity> {
        val perfil = PerfilDeAlerta.de(deficiencia)
        return PADRAO.map { nome ->
            val ligado = when (nome) {
                FONTE_ALTO_CONTRASTE, TEXTO_PARA_VOZ -> perfil.visual
                SINAIS_VIBRACAO -> perfil.auditiva
                else -> false
            }
            AcessibilidadeEntity(
                nomeAcessibilidade = nome,
                descricao = descricao(nome),
                ativo = ligado,
                usuarioId = usuarioId,
            )
        }
    }
}

/**
 * Resultado de enviarDados(): quantas leituras foram para o celular e se
 * ele estava conectado (false = nem tudo foi; o resto vai na próxima vez).
 */
data class ResultadoEnvio(val enviadas: Int, val celularConectado: Boolean)

/**
 * Implementação em código do diagrama de classes do TCC (Imagem 6).
 *
 * Room só permite entidades como dados puros, então os métodos desenhados
 * em cada classe do diagrama (autenticar(), coletarBiometria(),
 * gerarAlerta() etc.) foram implementados aqui, um por um, com o mesmo
 * nome do diagrama sempre que possível — comentado caso a caso quando o
 * app ainda não tem a peça necessária (sensor, tela, backend) pra
 * cumprir o método de verdade.
 */
class AliraRepository(private val context: Context) {

    private val db = AliraDatabase.getInstance(context)

    private val usuarioDao = db.usuarioDao()
    private val acessibilidadeDao = db.acessibilidadeDao()
    private val monitoramentoDao = db.monitoramentoDao()
    private val alertaDao = db.alertaDao()
    private val relatorioDao = db.relatorioDao()
    private val cuidadorDao = db.cuidadorDao()

    private val travaDoUsuario = Mutex()

    // ================= Usuario =================

    /**
     * Devolve o id do USUÁRIO ATIVO (quem está usando o relógio): o
     * cadastro que veio do celular, se já chegou; senão, o usuário
     * provisório "usuario@local" (criado aqui na primeira vez).
     * Também garante que ele tem as 3 linhas de acessibilidade.
     *
     * Antes devolvia sempre o PRIMEIRO usuário do banco — então, depois que
     * o cadastro do celular chegava, as leituras e a acessibilidade
     * continuavam indo para o provisório.
     */
    suspend fun garantirUsuarioLocal(): Long = travaDoUsuario.withLock {
        // withLock: se a tela e um serviço chamarem isto ao mesmo tempo na
        // primeira vez, um espera o outro — assim não nascem dois usuários
        // provisórios.
        val ativo = usuarioDao.obterUsuarioAtivo()
        val id = ativo?.idUsuario ?: usuarioDao.inserir(
            UsuarioEntity(nome = "Usuário", email = "usuario@local", senha = Hashing.sha256(""))
        )
        if (acessibilidadeDao.contarPorUsuario(id) == 0) {
            acessibilidadeDao.inserir(RecursosAcessibilidade.linhasIniciais(id, ativo?.deficiencia))
        }
        id
    }

    fun observarUsuarioAtual(): Flow<UsuarioEntity?> = usuarioDao.observarUsuarioAtivo()

    /**
     * Recebe, via WearDataLayerListenerService, os dados de um usuário
     * cadastrado no celular e grava (ou atualiza) a cópia local do
     * relógio. Casa por e-mail: se já existir um usuário local com esse
     * e-mail, atualiza os campos; senão, insere um registro novo — assim
     * o mesmo cadastro pode chegar mais de uma vez (o celular reenviando)
     * sem duplicar linha.
     *
     * Junto vêm os dados do cuidador (preenchidos no Perfil do celular):
     * ver sincronizarCuidador() logo abaixo.
     */
    suspend fun sincronizarUsuarioDoCelular(
        nome: String,
        dataNascimento: Long?,
        telefone: String?,
        email: String,
        deficiencia: String?,
        nivelAutonomia: String?,
        senhaHash: String,
        cuidadorNome: String = "",
        cuidadorTelefone: String = "",
        cuidadorEmail: String? = null,
        cuidadorParentesco: String? = null,
    ): Long {
        val existente = usuarioDao.obterPorEmail(email)
        val id = if (existente != null) {
            val atualizado = existente.copy(
                nome = nome,
                dataNascimento = dataNascimento,
                telefone = telefone,
                deficiencia = deficiencia,
                nivelAutonomia = nivelAutonomia,
                senha = senhaHash,
            )
            usuarioDao.atualizar(atualizado)
            existente.idUsuario
        } else {
            val novoId = usuarioDao.inserir(
                UsuarioEntity(
                    nome = nome,
                    dataNascimento = dataNascimento,
                    telefone = telefone,
                    email = email,
                    deficiencia = deficiencia,
                    nivelAutonomia = nivelAutonomia,
                    senha = senhaHash,
                )
            )
            // Usuário novo: a acessibilidade já começa ligada de acordo com
            // a deficiência marcada no cadastro do celular.
            acessibilidadeDao.inserir(RecursosAcessibilidade.linhasIniciais(novoId, deficiencia))
            novoId
        }
        sincronizarCuidador(id, cuidadorNome, cuidadorTelefone, cuidadorEmail, cuidadorParentesco)
        return id
    }

    /**
     * Deixa o cuidador do relógio IGUAL ao do celular. O celular tem um
     * cuidador por usuário: se já existe aqui, atualiza; se não, cria. Se
     * o celular mandou nome e telefone vazios (a pessoa apagou o cuidador),
     * apaga aqui também.
     */
    private suspend fun sincronizarCuidador(
        usuarioId: Long,
        nome: String,
        telefone: String,
        email: String?,
        parentesco: String?,
    ) {
        val atuais = cuidadorDao.obterCuidadores(usuarioId)
        if (nome.isBlank() && telefone.isBlank()) {
            atuais.forEach { cuidadorDao.remover(it) }
            return
        }
        val primeiro = atuais.firstOrNull()
        val cuidador = CuidadorEntity(
            idCuidador = primeiro?.idCuidador ?: 0L,
            nome = nome,
            telefone = telefone,
            parentesco = parentesco,
            email = email,
            usuarioId = usuarioId,
        )
        if (primeiro != null) cuidadorDao.atualizar(cuidador) else cuidadorDao.inserir(cuidador)
    }

    /** Cuidador do usuário ativo (o que aparece em Configurações > Cuidador), ou null. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observarCuidadorDoUsuarioAtivo(): Flow<CuidadorEntity?> =
        usuarioDao.observarUsuarioAtivo().flatMapLatest { usuario ->
            if (usuario == null) flowOf(null)
            else cuidadorDao.observarCuidadores(usuario.idUsuario).map { it.firstOrNull() }
        }

    /** Usuario.autenticar() — compara o hash da senha informada com o hash salvo. */
    suspend fun autenticar(email: String, senha: String): Boolean {
        val usuario = usuarioDao.obterPorEmail(email) ?: return false
        return usuario.senha == Hashing.sha256(senha)
    }

    /** Usuario.atualizarPerfil() */
    suspend fun atualizarPerfil(usuario: UsuarioEntity) {
        usuarioDao.atualizar(usuario)
    }

    /**
     * Usuario.solicitarAjuda() — SOS manual (RF04): cria um alerta de
     * urgência alta sem depender de nenhuma leitura de Monitoramento.
     */
    suspend fun solicitarAjuda(descricao: String = "Solicitação manual de ajuda") {
        gerarAlerta(
            tipoAlerta = "MANUAL",
            descricao = descricao,
            nivelUrgencia = "ALTA",
            monitoramentoId = null,
        )
    }

    // ================= Smartwatch =================
    // "Smartwatch" não tem tabela própria (ver comentário em AliraDatabase.kt) —
    // é o próprio app rodando, não um dado a se guardar sobre si mesmo. Os
    // métodos abaixo continuam existindo porque descrevem comportamento, não dado.

    /**
     * Smartwatch.coletarBiometria() — grava uma leitura de batimentos.
     *
     * Quem decide QUANDO gravar e se o batimento está fora do normal é o
     * MonitorCardiaco (pacote monitoramento); aqui é só a gravação.
     */
    suspend fun coletarBiometria(usuarioId: Long, frequenciaCardiaca: Float, dataHora: Long = System.currentTimeMillis()) {
        if (frequenciaCardiaca <= 0f) return // sensor sem contato/ainda estabilizando
        monitoramentoDao.inserir(
            MonitoramentoEntity(
                frequenciaCardiaca = frequenciaCardiaca.toDouble(),
                dataHora = dataHora,
                usuarioId = usuarioId,
            )
        )
    }

    /**
     * Grava a atividade física do dia (totais desde a meia-noite) numa
     * linha da tabela monitoramento, nas colunas novas da versão 2 do
     * banco. Chamado pelo MonitoramentoBackgroundService quando o Health
     * Services manda números novos.
     */
    suspend fun coletarAtividadeDiaria(
        usuarioId: Long,
        calorias: Double?,
        passos: Long?,
        distanciaMetros: Double?,
        dataHora: Long = System.currentTimeMillis(),
    ) {
        if (calorias == null && passos == null && distanciaMetros == null) return
        monitoramentoDao.inserir(
            MonitoramentoEntity(
                calorias = calorias,
                passos = passos,
                distanciaMetros = distanciaMetros,
                dataHora = dataHora,
                usuarioId = usuarioId,
            )
        )
    }

    /**
     * Smartwatch.detectarQueda() — a detecção de verdade fica em
     * monitoramento/DetectorDeQueda.kt, que lê o acelerômetro dentro do
     * MonitoramentoContinuoService (ligado pelo botão "Monitoramento
     * contínuo" em Configurações). Esta função só responde se esse
     * monitoramento está ligado, isto é, se o relógio está vigiando quedas.
     */
    fun detectarQueda(): Boolean = PreferenciasMonitoramento.estaLigado(context)

    /**
     * Smartwatch.enviarDados() — manda ao CELULAR as leituras que ele
     * ainda não tem (batimento, passos, calorias, distância), para o app do
     * celular montar os gráficos. É chamado quando a pessoa toca em
     * "Gerar relatório" (GenerateReportScreen).
     *
     * Como funciona:
     *  1. O relógio lembra o número (id) da última leitura que já mandou
     *     (SharedPreferences "alira_envio_celular").
     *  2. Pega as leituras depois dela, em lotes de [TAMANHO_LOTE], porque
     *     uma mensagem da Data Layer tem limite de tamanho (~100 KB).
     *  3. Cada lote vira texto, uma leitura por linha, e vai para o
     *     celular. Só depois que o lote sai com sucesso o "último enviado"
     *     anda: se o celular sumir no meio, o resto vai na próxima vez.
     *
     * A sincronização com a nuvem (Firebase, capítulo 6.2.2) continua fora
     * do escopo: os dados ficam no SQLite do relógio e do celular (RNF02).
     */
    suspend fun enviarDados(usuarioId: Long): ResultadoEnvio = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("alira_envio_celular", Context.MODE_PRIVATE)
        var ultimoId = prefs.getLong("ultimo_id_enviado", 0L)
        var enviadas = 0
        while (true) {
            val lote = monitoramentoDao.obterDepoisDe(usuarioId, ultimoId, TAMANHO_LOTE)
            if (lote.isEmpty()) break
            val texto = lote.joinToString("\n") { linhaDeLeitura(it) }
            if (!enviarLeiturasParaCelular(context, texto)) {
                return@withContext ResultadoEnvio(enviadas, celularConectado = false)
            }
            ultimoId = lote.last().idMonitoramento
            prefs.edit().putLong("ultimo_id_enviado", ultimoId).apply()
            enviadas += lote.size
            if (lote.size < TAMANHO_LOTE) break
        }
        ResultadoEnvio(enviadas, celularConectado = true)
    }

    /**
     * Uma leitura em uma linha de texto, campos separados por ";":
     * número ; data/hora ; bpm ; passos ; calorias ; distância.
     * Valor que não existe vira campo vazio. O celular lê na mesma ordem
     * (decodificarLeituras em MobileDataLayerService.kt).
     */
    private fun linhaDeLeitura(m: MonitoramentoEntity): String = listOf(
        m.idMonitoramento.toString(),
        m.dataHora.toString(),
        m.frequenciaCardiaca?.toString() ?: "",
        m.passos?.toString() ?: "",
        m.calorias?.toString() ?: "",
        m.distanciaMetros?.toString() ?: "",
    ).joinToString(";")

    /** Smartwatch.emitirAlerta() — atalho para gerarAlerta() com vibração/som local (RF03). */
    suspend fun emitirAlerta(tipoAlerta: String, nivelUrgencia: String, monitoramentoId: Long?) {
        gerarAlerta(tipoAlerta, descricao = null, nivelUrgencia = nivelUrgencia, monitoramentoId = monitoramentoId)
    }

    // ================= Monitoramento =================

    fun observarUltimoMonitoramento(usuarioId: Long): Flow<MonitoramentoEntity?> =
        monitoramentoDao.observarUltimo(usuarioId)

    fun observarHistoricoMonitoramento(usuarioId: Long, limite: Int = 50): Flow<List<MonitoramentoEntity>> =
        monitoramentoDao.observarHistorico(usuarioId, limite)

    suspend fun mediaFrequenciaCardiaca(usuarioId: Long, inicio: Long, fim: Long): Double? =
        monitoramentoDao.mediaFrequenciaCardiaca(usuarioId, inicio, fim)

    // ================= Alerta =================

    /** Alerta.gerarAlerta() */
    suspend fun gerarAlerta(
        tipoAlerta: String,
        descricao: String?,
        nivelUrgencia: String,
        monitoramentoId: Long?,
    ): Long {
        val dataHora = System.currentTimeMillis()
        val id = alertaDao.inserir(
            AlertaEntity(
                tipoAlerta = tipoAlerta,
                descricao = descricao,
                nivelUrgencia = nivelUrgencia,
                dataHora = dataHora,
                status = "PENDENTE",
                monitoramentoId = monitoramentoId,
            )
        )
        vibrar()
        // Avisa o celular pareado (melhor esforço — ver WearDataLayerListenerService.kt).
        // É essa chamada que "conecta" o alerta gerado no relógio ao app do celular.
        enviarAlertaParaCelular(context, tipoAlerta, descricao, nivelUrgencia, dataHora)
        return id
    }

    /** Alerta.enviarNotificacao() — vibra o relógio e associa o alerta a um cuidador. */
    suspend fun enviarNotificacao(idAlerta: Long, cuidadorId: Long) {
        alertaDao.definirCuidadorNotificado(idAlerta, cuidadorId, status = "ENVIADO_AO_CUIDADOR")
        vibrar()
        // TODO: sem backend/FCM ainda, isto só grava no banco local; a
        // notificação de verdade para o app do cuidador depende do
        // Firebase, que ainda não existe no projeto.
    }

    /** Alerta.cancelarAlerta() */
    suspend fun cancelarAlerta(idAlerta: Long) {
        alertaDao.atualizarStatus(idAlerta, status = "CANCELADO")
    }

    /**
     * Registra uma emergência (queda / crise de ansiedade) na tabela alerta,
     * com status PENDENTE, enquanto o relógio pergunta "Você está bem?".
     *
     * Diferente de gerarAlerta(): NÃO vibra e NÃO avisa o celular na hora —
     * quem cuida disso é a CentralDeEmergencia, que só avisa o contato se a
     * pessoa pedir ajuda ou não responder.
     */
    suspend fun registrarEmergencia(tipoAlerta: String, descricao: String?): Long =
        alertaDao.inserir(
            AlertaEntity(
                tipoAlerta = tipoAlerta,
                descricao = descricao,
                nivelUrgencia = "ALTA",
                dataHora = System.currentTimeMillis(),
                status = "PENDENTE",
            )
        )

    /** Muda o status de um alerta (ex.: CANCELADO, ENVIADO_AO_CUIDADOR, FALHA_AO_AVISAR). */
    suspend fun atualizarStatusAlerta(idAlerta: Long, status: String) =
        alertaDao.atualizarStatus(idAlerta, status)

    /**
     * Usuário usado na emergência: o que veio do cadastro do celular, se
     * existir; senão, o usuário local provisório.
     */
    suspend fun obterUsuarioParaEmergencia(): UsuarioEntity? = usuarioDao.obterUsuarioAtivo()

    fun observarAlertasPendentes(): Flow<List<AlertaEntity>> = alertaDao.observarPendentes()

    fun observarHistoricoAlertas(): Flow<List<AlertaEntity>> = alertaDao.observarHistorico()

    private fun vibrar() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        // Com "Sinais de vibração" ligado, o alerta vibra 3 vezes na força
        // máxima (255); desligado, uma vibração normal de 0,4 s.
        val efeito = if (GerenciadorAcessibilidade.config.value.vibracao) {
            VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500, 200, 500), intArrayOf(0, 255, 0, 255, 0, 255), -1)
        } else {
            VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        vibrator.vibrate(efeito)
    }

    // ================= Relatorio =================

    /** Relatorio.gerarPDF() — gera um PDF simples com o histórico de monitoramento. */
    suspend fun gerarPDF(usuarioId: Long, inicio: Long, fim: Long): RelatorioEntity {
        val registros = monitoramentoDao.obterEntrePeriodo(usuarioId, inicio, fim)

        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
        val documento = PdfDocument()
        val pagina = documento.startPage(PdfDocument.PageInfo.Builder(360, 300 + registros.size * 20, 1).create())
        val canvas = pagina.canvas
        val paint = android.graphics.Paint().apply { textSize = 12f }
        canvas.drawText("Relatório ALIRA", 16f, 24f, paint)
        var y = 48f
        if (registros.isEmpty()) {
            canvas.drawText("Sem registros no período.", 16f, y, paint)
        } else {
            registros.forEach { registro ->
                canvas.drawText("${formato.format(Date(registro.dataHora))}  ${descreverRegistro(registro)}", 16f, y, paint)
                y += 20f
            }
        }
        documento.finishPage(pagina)

        val pasta = File(context.filesDir, "relatorios").apply { mkdirs() }
        val arquivo = File(pasta, "relatorio_${System.currentTimeMillis()}.pdf")
        FileOutputStream(arquivo).use { documento.writeTo(it) }
        documento.close()

        val relatorio = RelatorioEntity(
            tipo = "PDF",
            dataGeracao = System.currentTimeMillis(),
            caminhoArquivo = arquivo.absolutePath,
            usuarioId = usuarioId,
        )
        val id = relatorioDao.inserir(relatorio)
        return relatorio.copy(idRelatorio = id)
    }

    /** Relatorio.exportarDados() — exporta o histórico de monitoramento como CSV. */
    suspend fun exportarDados(usuarioId: Long, inicio: Long, fim: Long): RelatorioEntity {
        val registros = monitoramentoDao.obterEntrePeriodo(usuarioId, inicio, fim)
        val pasta = File(context.filesDir, "relatorios").apply { mkdirs() }
        val arquivo = File(pasta, "dados_${System.currentTimeMillis()}.csv")
        arquivo.bufferedWriter().use { escritor ->
            escritor.appendLine(
                "data_hora,frequencia_cardiaca,saturacao_oxigenio,temperatura_corporal,calorias,passos,distancia_metros"
            )
            registros.forEach { registro ->
                escritor.appendLine(
                    "${registro.dataHora},${registro.frequenciaCardiaca ?: ""},${registro.saturacaoOxigenio ?: ""}," +
                        "${registro.temperaturaCorporal ?: ""},${registro.calorias ?: ""},${registro.passos ?: ""}," +
                        "${registro.distanciaMetros ?: ""}"
                )
            }
        }
        val relatorio = RelatorioEntity(
            tipo = "CSV",
            dataGeracao = System.currentTimeMillis(),
            caminhoArquivo = arquivo.absolutePath,
            usuarioId = usuarioId,
        )
        val id = relatorioDao.inserir(relatorio)
        return relatorio.copy(idRelatorio = id)
    }

    fun observarHistoricoRelatorios(usuarioId: Long): Flow<List<RelatorioEntity>> =
        relatorioDao.observarHistorico(usuarioId)

    /** Histórico de monitoramento do usuário ativo (muda sozinho se o usuário ativo mudar). */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observarHistoricoDoUsuarioAtivo(limite: Int = 20): Flow<List<MonitoramentoEntity>> =
        usuarioDao.observarUsuarioAtivo().flatMapLatest { usuario ->
            if (usuario == null) flowOf(emptyList())
            else monitoramentoDao.observarHistorico(usuario.idUsuario, limite)
        }

    // ================= Acessibilidade =================

    /**
     * Linhas de acessibilidade do usuário ativo. "flatMapLatest": sempre
     * que o usuário ativo muda (ex.: chegou o cadastro do celular), larga a
     * consulta antiga e passa a observar as linhas do usuário novo.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observarRecursosDoUsuarioAtivo(): Flow<List<AcessibilidadeEntity>> =
        usuarioDao.observarUsuarioAtivo().flatMapLatest { usuario ->
            if (usuario == null) flowOf(emptyList())
            else acessibilidadeDao.observarRecursos(usuario.idUsuario)
        }

    /** As 3 chaves de acessibilidade do usuário ativo, já como liga/desliga. */
    fun observarConfigAcessibilidade(): Flow<ConfigAcessibilidade> =
        observarRecursosDoUsuarioAtivo().map { recursos ->
            fun ligado(nome: String) = recursos.any { it.nomeAcessibilidade == nome && it.ativo }
            ConfigAcessibilidade(
                altoContraste = ligado(RecursosAcessibilidade.FONTE_ALTO_CONTRASTE),
                vibracao = ligado(RecursosAcessibilidade.SINAIS_VIBRACAO),
                voz = ligado(RecursosAcessibilidade.TEXTO_PARA_VOZ),
            )
        }

    fun observarRecursosAcessibilidade(usuarioId: Long): Flow<List<AcessibilidadeEntity>> =
        acessibilidadeDao.observarRecursos(usuarioId)

    /** Acessibilidade.ativar() */
    suspend fun ativar(usuarioId: Long, nomeRecurso: String) =
        acessibilidadeDao.definirAtivo(usuarioId, nomeRecurso, ativo = true)

    /** Acessibilidade.desativar() */
    suspend fun desativar(usuarioId: Long, nomeRecurso: String) =
        acessibilidadeDao.definirAtivo(usuarioId, nomeRecurso, ativo = false)

    /** Acessibilidade.personalizar() — liga/desliga conforme o valor desejado. */
    suspend fun personalizar(usuarioId: Long, nomeRecurso: String, ativo: Boolean) =
        acessibilidadeDao.definirAtivo(usuarioId, nomeRecurso, ativo)

    // ================= Cuidador =================

    suspend fun cadastrarCuidador(cuidador: CuidadorEntity): Long = cuidadorDao.inserir(cuidador)

    fun observarCuidadores(usuarioId: Long): Flow<List<CuidadorEntity>> = cuidadorDao.observarCuidadores(usuarioId)

    /**
     * Cuidado.receberAlerta() — por natureza roda do LADO do cuidador (no
     * celular dele, via push/Firebase). Aqui fica a consulta que um app
     * do cuidador chamaria para ler os alertas endereçados a ele.
     */
    fun receberAlerta(cuidadorId: Long): Flow<List<AlertaEntity>> = alertaDao.observarParaCuidador(cuidadorId)

    /**
     * Cuidado.acompanharUsuario() — mesma observação acima: hoje só existe
     * o app do próprio usuário (este smartwatch), então esta função expõe
     * a consulta que o futuro app do cuidador usaria para acompanhar os
     * sinais vitais do usuário monitorado.
     */
    fun acompanharUsuario(usuarioId: Long): Flow<MonitoramentoEntity?> = monitoramentoDao.observarUltimo(usuarioId)

    companion object {
        /** Quantas leituras vão em cada mensagem para o celular (fica bem abaixo do limite de ~100 KB). */
        const val TAMANHO_LOTE = 1000

        /** Texto de uma linha do histórico, ex.: "78 BPM" ou "3820 passos · 412 kcal · 2,7 km". */
        fun descreverRegistro(registro: MonitoramentoEntity): String {
            registro.frequenciaCardiaca?.let { return "${it.toInt()} BPM" }
            val partes = buildList {
                registro.passos?.let { add("$it passos") }
                registro.calorias?.let { add("${it.toInt()} kcal") }
                registro.distanciaMetros?.let { add("%.1f km".format(Locale("pt", "BR"), it / 1000.0)) }
            }
            return partes.joinToString(" · ").ifEmpty { "-" }
        }

        @Volatile
        private var instancia: AliraRepository? = null

        fun getInstance(context: Context): AliraRepository =
            instancia ?: synchronized(this) {
                instancia ?: AliraRepository(context.applicationContext).also { instancia = it }
            }
    }
}
