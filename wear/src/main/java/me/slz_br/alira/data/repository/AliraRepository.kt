package me.slz_br.alira.data.repository

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.flow.Flow
import me.slz_br.alira.data.db.AliraDatabase
import me.slz_br.alira.data.db.Hashing
import me.slz_br.alira.data.db.entities.AcessibilidadeEntity
import me.slz_br.alira.data.db.entities.AlertaEntity
import me.slz_br.alira.data.db.entities.CuidadorEntity
import me.slz_br.alira.data.db.entities.MonitoramentoEntity
import me.slz_br.alira.data.db.entities.RelatorioEntity
import me.slz_br.alira.data.db.entities.UsuarioEntity
import me.slz_br.alira.data.sync.enviarAlertaParaCelular

/** Nomes dos 3 recursos de acessibilidade já existentes na tela de configurações. */
object RecursosAcessibilidade {
    const val FONTE_ALTO_CONTRASTE = "Fonte de alto contraste"
    const val SINAIS_VIBRACAO = "Sinais de vibração"
    const val TEXTO_PARA_VOZ = "Texto para Voz"

    val PADRAO = listOf(FONTE_ALTO_CONTRASTE, SINAIS_VIBRACAO, TEXTO_PARA_VOZ)
}

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

    // ================= Usuario =================

    /** Garante um usuário local enquanto RF08 (cadastro/login) não existe. */
    suspend fun garantirUsuarioLocal(): Long {
        usuarioDao.obterUsuarioAtual()?.let { return it.idUsuario }
        val id = usuarioDao.inserir(
            UsuarioEntity(nome = "Usuário", email = "usuario@local", senha = Hashing.sha256(""))
        )
        acessibilidadeDao.inserir(
            RecursosAcessibilidade.PADRAO.map { nome -> AcessibilidadeEntity(nomeAcessibilidade = nome, usuarioId = id) }
        )
        return id
    }

    fun observarUsuarioAtual(): Flow<UsuarioEntity?> = usuarioDao.observarUsuarioAtual()

    /**
     * Recebe, via WearDataLayerListenerService, os dados de um usuário
     * cadastrado no celular e grava (ou atualiza) a cópia local do
     * relógio. Casa por e-mail: se já existir um usuário local com esse
     * e-mail, atualiza os campos; senão, insere um registro novo — assim
     * o mesmo cadastro pode chegar mais de uma vez (o celular reenviando)
     * sem duplicar linha.
     */
    suspend fun sincronizarUsuarioDoCelular(
        nome: String,
        dataNascimento: Long?,
        telefone: String?,
        email: String,
        deficiencia: String?,
        nivelAutonomia: String?,
        senhaHash: String,
    ): Long {
        val existente = usuarioDao.obterPorEmail(email)
        return if (existente != null) {
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
            val id = usuarioDao.inserir(
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
            acessibilidadeDao.inserir(
                RecursosAcessibilidade.PADRAO.map { nome -> AcessibilidadeEntity(nomeAcessibilidade = nome, usuarioId = id) }
            )
            id
        }
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

    /** Smartwatch.coletarBiometria() — grava a leitura atual do sensor de batimentos. */
    suspend fun coletarBiometria(usuarioId: Long, frequenciaCardiaca: Float, dataHora: Long = System.currentTimeMillis()) {
        if (frequenciaCardiaca <= 0f) return // sensor sem contato/ainda estabilizando
        val idMonitoramento = monitoramentoDao.inserir(
            MonitoramentoEntity(
                frequenciaCardiaca = frequenciaCardiaca.toDouble(),
                dataHora = dataHora,
                usuarioId = usuarioId,
            )
        )
        avaliarFrequenciaAnormal(idMonitoramento, frequenciaCardiaca)
    }

    /**
     * Smartwatch.detectarQueda() — TODO: não implementado. O projeto ainda
     * não lê o acelerômetro (só TYPE_HEART_RATE, em HearRateScreen.kt).
     * Função criada para o comportamento do diagrama existir no código,
     * pronta para ser chamada quando o sensor de queda for implementado.
     */
    fun detectarQueda(): Boolean {
        Log.w("ALIRA", "detectarQueda() ainda não implementado — falta sensor de acelerômetro")
        return false
    }

    /**
     * Smartwatch.enviarDados() — TODO: sincronização com a nuvem
     * (Firebase, arquitetura híbrida do capítulo 6.2.2) ainda não existe.
     * Por enquanto os dados ficam só no SQLite local (RNF02).
     */
    fun enviarDados() {
        Log.i("ALIRA", "enviarDados() ainda não implementado — sem integração com Firebase")
    }

    /** Smartwatch.emitirAlerta() — atalho para gerarAlerta() com vibração/som local (RF03). */
    suspend fun emitirAlerta(tipoAlerta: String, nivelUrgencia: String, monitoramentoId: Long?) {
        gerarAlerta(tipoAlerta, descricao = null, nivelUrgencia = nivelUrgencia, monitoramentoId = monitoramentoId)
    }

    private suspend fun avaliarFrequenciaAnormal(idMonitoramento: Long, bpm: Float) {
        if (bpm < 40f || bpm > 160f) {
            gerarAlerta(
                tipoAlerta = "FREQUENCIA_ANORMAL",
                descricao = "Frequência cardíaca fora do padrão: $bpm BPM",
                nivelUrgencia = "ALTA",
                monitoramentoId = idMonitoramento,
            )
        }
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
        // Firebase (ver AliraRepository.enviarDados()).
    }

    /** Alerta.cancelarAlerta() */
    suspend fun cancelarAlerta(idAlerta: Long) {
        alertaDao.atualizarStatus(idAlerta, status = "CANCELADO")
    }

    fun observarAlertasPendentes(): Flow<List<AlertaEntity>> = alertaDao.observarPendentes()

    fun observarHistoricoAlertas(): Flow<List<AlertaEntity>> = alertaDao.observarHistorico()

    private fun vibrar() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    // ================= Relatorio =================

    /** Relatorio.gerarPDF() — gera um PDF simples com o histórico de monitoramento. */
    suspend fun gerarPDF(usuarioId: Long, inicio: Long, fim: Long): RelatorioEntity {
        val registros = monitoramentoDao.obterEntrePeriodo(usuarioId, inicio, fim)

        val documento = PdfDocument()
        val pagina = documento.startPage(PdfDocument.PageInfo.Builder(300, 300 + registros.size * 20, 1).create())
        val canvas = pagina.canvas
        val paint = android.graphics.Paint().apply { textSize = 12f }
        canvas.drawText("Relatório ALIRA", 16f, 24f, paint)
        var y = 48f
        if (registros.isEmpty()) {
            canvas.drawText("Sem registros no período.", 16f, y, paint)
        } else {
            registros.forEach { registro ->
                canvas.drawText("${registro.dataHora}  ${registro.frequenciaCardiaca ?: "-"} BPM", 16f, y, paint)
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
            escritor.appendLine("data_hora,frequencia_cardiaca,saturacao_oxigenio,temperatura_corporal")
            registros.forEach { registro ->
                escritor.appendLine(
                    "${registro.dataHora},${registro.frequenciaCardiaca ?: ""},${registro.saturacaoOxigenio ?: ""},${registro.temperaturaCorporal ?: ""}"
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

    // ================= Acessibilidade =================

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
        @Volatile
        private var instancia: AliraRepository? = null

        fun getInstance(context: Context): AliraRepository =
            instancia ?: synchronized(this) {
                instancia ?: AliraRepository(context.applicationContext).also { instancia = it }
            }
    }
}
