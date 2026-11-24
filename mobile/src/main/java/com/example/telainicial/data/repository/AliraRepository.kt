package com.example.telainicial.data.repository

import android.content.Context
import com.example.telainicial.data.db.AliraDatabase
import com.example.telainicial.data.db.entities.CuidadorEntity
import com.example.telainicial.data.db.entities.MonitoramentoEntity
import com.example.telainicial.data.db.entities.UsuarioEntity
import com.example.telainicial.data.sync.enviarUsuarioParaRelogio
import kotlinx.coroutines.flow.Flow

/**
 * Dados do Perfil que vão para o banco e para o relógio. A tela de Perfil
 * monta este objeto com o que foi digitado; quem decide como salvar é o
 * repositório.
 */
data class DadosPerfil(
    val nome: String,
    val email: String,
    val telefone: String?,
    val dataNascimento: Long?,
    val deficiencia: String?,      // códigos, ex.: "AUDITIVA,VISUAL"
    val cuidadorNome: String,
    val cuidadorTelefone: String,  // o campo "Telefone do cuidador" do Perfil
    val cuidadorEmail: String?,
    val cuidadorParentesco: String?,
)

/**
 * Mesma ideia do AliraRepository do relógio: junta o banco local (Room) e a
 * conversa com o relógio (Data Layer) atrás de funções com nome de negócio.
 * As telas nunca falam direto com o banco nem com o relógio.
 *
 * Singleton (getInstance): todas as telas usam a MESMA instância e o mesmo
 * banco por baixo.
 */
class AliraRepository private constructor(context: Context, database: AliraDatabase) {

    private val appContext = context.applicationContext
    private val usuarioDao = database.usuarioDao()
    private val cuidadorDao = database.cuidadorDao()
    private val monitoramentoDao = database.monitoramentoDao()

    // ===================== PERFIL (usuário + cuidador) =====================

    /**
     * Salva o Perfil no banco e manda uma cópia ao relógio.
     *
     * O celular tem UM usuário só (o dono): se já existe, atualiza; se não,
     * cria. O mesmo vale para o cuidador (um por usuário). Sem nome e sem
     * telefone, o cuidador é apagado (a pessoa limpou os campos).
     *
     * Devolve true se a cópia foi enviada ao relógio. Só envia com e-mail
     * preenchido, porque é pelo e-mail que o relógio reconhece o usuário.
     */
    suspend fun salvarPerfil(dados: DadosPerfil): Boolean {
        val existente = usuarioDao.obterMaisRecente()
        val usuario = if (existente != null) {
            existente.copy(
                nome = dados.nome,
                email = dados.email,
                telefone = dados.telefone,
                dataNascimento = dados.dataNascimento,
                deficiencia = dados.deficiencia,
            ).also { usuarioDao.atualizar(it) }
        } else {
            val novo = UsuarioEntity(
                nome = dados.nome,
                email = dados.email,
                telefone = dados.telefone,
                dataNascimento = dados.dataNascimento,
                deficiencia = dados.deficiencia,
            )
            novo.copy(idUsuario = usuarioDao.inserir(novo))
        }

        val cuidador = salvarCuidador(usuario.idUsuario, dados)

        if (usuario.email.isBlank()) return false
        enviarUsuarioParaRelogio(appContext, usuario, cuidador)
        return true
    }

    private suspend fun salvarCuidador(usuarioId: Long, dados: DadosPerfil): CuidadorEntity? {
        val atuais = cuidadorDao.obterCuidadores(usuarioId)
        val vazio = dados.cuidadorNome.isBlank() && dados.cuidadorTelefone.isBlank()
        if (vazio) {
            atuais.forEach { cuidadorDao.remover(it) }
            return null
        }
        val primeiro = atuais.firstOrNull()
        val cuidador = CuidadorEntity(
            idCuidador = primeiro?.idCuidador ?: 0L,
            nome = dados.cuidadorNome,
            telefone = dados.cuidadorTelefone,
            parentesco = dados.cuidadorParentesco,
            email = dados.cuidadorEmail,
            usuarioId = usuarioId,
        )
        return if (primeiro != null) {
            cuidadorDao.atualizar(cuidador)
            cuidador
        } else {
            cuidador.copy(idCuidador = cuidadorDao.inserir(cuidador))
        }
    }

    /**
     * Contatos de emergência (cuidadores) de quem está usando o relógio.
     *
     * O relógio manda o e-mail do usuário junto com a emergência; é por ele
     * que achamos o cadastro aqui. Se não achar (ex.: o relógio ainda está
     * com o usuário provisório "usuario@local"), usa o usuário deste
     * celular — num celular pessoal, é o dono dele.
     */
    suspend fun contatosDeEmergencia(emailUsuario: String): List<CuidadorEntity> {
        val usuario = usuarioDao.obterPorEmail(emailUsuario) ?: usuarioDao.obterMaisRecente()
            ?: return emptyList()
        return cuidadorDao.obterCuidadores(usuario.idUsuario)
    }

    // ===================== LEITURAS DO RELÓGIO =====================

    /** Grava as leituras recebidas do relógio. Devolve quantas eram novas. */
    suspend fun salvarLeituras(leituras: List<MonitoramentoEntity>): Int =
        monitoramentoDao.inserirTodas(leituras).count { it != -1L }

    /** Monitoramento.coletarBiometria() visto do celular: média dos batimentos no período. */
    suspend fun mediaBatimentos(inicio: Long, fim: Long): Double? =
        monitoramentoDao.mediaBatimentos(inicio, fim)

    fun observarUltimosBatimentos(limite: Int = 20): Flow<List<MonitoramentoEntity>> =
        monitoramentoDao.observarUltimosBatimentos(limite)

    companion object {
        @Volatile
        private var instancia: AliraRepository? = null

        fun getInstance(context: Context): AliraRepository =
            instancia ?: synchronized(this) {
                instancia ?: AliraRepository(context, AliraDatabase.getInstance(context)).also {
                    instancia = it
                }
            }

        /** Só para os testes instrumentados: instância com um banco em memória. */
        fun paraTestes(context: Context, database: AliraDatabase): AliraRepository =
            AliraRepository(context, database)
    }
}
