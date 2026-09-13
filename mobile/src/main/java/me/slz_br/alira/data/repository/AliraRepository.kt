package me.slz_br.alira.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import me.slz_br.alira.data.db.AliraDatabase
import me.slz_br.alira.data.db.Hashing
import me.slz_br.alira.data.db.entities.CuidadorEntity
import me.slz_br.alira.data.db.entities.RelatorioEntity
import me.slz_br.alira.data.db.entities.UsuarioEntity
import me.slz_br.alira.data.sync.enviarUsuarioParaRelogio

/**
 * Mesma ideia do AliraRepository do :wear: junta banco local (Room) +
 * sincronização (Data Layer API) atrás de funções com nome de negócio, para
 * as Activities nunca falarem com DAO/Wearable diretamente.
 *
 * Singleton via getInstance(context), igual ao AliraRepository do :wear —
 * assim LoginActivity e CadastroActivity sempre pegam a MESMA instância
 * (e o mesmo banco por baixo), em vez de abrir um Room.databaseBuilder novo
 * a cada tela.
 *
 * O construtor recebe o AliraDatabase já pronto (em vez de chamar
 * AliraDatabase.getInstance() ele mesmo) só para permitir os testes
 * instrumentados em androidTest/ — lá a gente passa um banco em memória
 * (Room.inMemoryDatabaseBuilder) através de paraTestes(), sem tocar no
 * banco de verdade do app (alira.db) nem no singleton de produção. Quem usa
 * o app normalmente nunca chama esse construtor diretamente: sempre passa
 * por getInstance(context), que resolve o banco de produção sozinho.
 */
class AliraRepository private constructor(context: Context, database: AliraDatabase) {

    private val appContext = context.applicationContext
    private val db = database
    private val usuarioDao = db.usuarioDao()
    private val cuidadorDao = db.cuidadorDao()
    private val relatorioDao = db.relatorioDao()

    /**
     * Confere e-mail + senha contra o hash salvo. Retorna o UsuarioEntity em
     * caso de sucesso, ou null se e-mail não existe ou senha não confere —
     * a Activity decide o que mostrar (não lança exceção para um caso
     * esperado do fluxo normal de login).
     */
    suspend fun autenticar(email: String, senha: String): UsuarioEntity? {
        val usuario = usuarioDao.obterPorEmail(email) ?: return null
        val hashDigitado = Hashing.sha256(senha)
        return if (hashDigitado == usuario.senha) usuario else null
    }

    /**
     * Cadastra um novo Usuario (RF08). Depois de salvar no banco do próprio
     * celular, tenta sincronizar com o relógio pareado — "tenta" porque,
     * como MessageClient não tem fila de retentativa, isso só funciona se
     * o relógio estiver por perto agora (ver comentário em
     * data/sync/MobileDataLayerService.kt). Se falhar, o cadastro no
     * celular continua válido; o usuário só perde o "login automático" no
     * relógio até os dois ficarem próximos de novo.
     *
     * Retorna null se já existe um cadastro com esse e-mail.
     */
    suspend fun cadastrar(
        nome: String,
        dataNascimento: Long?,
        telefone: String?,
        email: String,
        deficiencia: String?,
        nivelAutonomia: String?,
        senha: String,
    ): UsuarioEntity? {
        if (usuarioDao.obterPorEmail(email) != null) return null

        val usuario = UsuarioEntity(
            nome = nome,
            dataNascimento = dataNascimento,
            telefone = telefone,
            email = email,
            deficiencia = deficiencia,
            nivelAutonomia = nivelAutonomia,
            senha = Hashing.sha256(senha),
        )
        val id = usuarioDao.inserir(usuario)
        val usuarioSalvo = usuario.copy(idUsuario = id)

        enviarUsuarioParaRelogio(appContext, usuarioSalvo)

        return usuarioSalvo
    }

    suspend fun atualizarPerfil(usuario: UsuarioEntity) {
        usuarioDao.atualizar(usuario)
        // Reenvia a versão atualizada para o relógio pelo mesmo caminho do
        // cadastro — o WearDataLayerListenerService já sabe fazer upsert
        // por e-mail (ver sincronizarUsuarioDoCelular no :wear).
        enviarUsuarioParaRelogio(appContext, usuario)
    }

    fun observarUsuario(idUsuario: Long): Flow<UsuarioEntity?> =
        usuarioDao.observarPorId(idUsuario)

    suspend fun cadastrarCuidador(
        nome: String,
        telefone: String,
        parentesco: String?,
        email: String?,
        usuarioId: Long,
    ): Long = cuidadorDao.inserir(
        CuidadorEntity(
            nome = nome,
            telefone = telefone,
            parentesco = parentesco,
            email = email,
            usuarioId = usuarioId,
        )
    )

    fun observarCuidadores(usuarioId: Long): Flow<List<CuidadorEntity>> =
        cuidadorDao.observarCuidadores(usuarioId)

    suspend fun removerCuidador(cuidador: CuidadorEntity) = cuidadorDao.remover(cuidador)

    fun observarHistoricoRelatorios(usuarioId: Long): Flow<List<RelatorioEntity>> =
        relatorioDao.observarHistorico(usuarioId)

    suspend fun registrarRelatorio(tipo: String, caminhoArquivo: String?, usuarioId: Long): Long =
        relatorioDao.inserir(
            RelatorioEntity(
                tipo = tipo,
                dataGeracao = System.currentTimeMillis(),
                caminhoArquivo = caminhoArquivo,
                usuarioId = usuarioId,
            )
        )

    companion object {
        @Volatile
        private var instancia: AliraRepository? = null

        fun getInstance(context: Context): AliraRepository =
            instancia ?: synchronized(this) {
                instancia ?: AliraRepository(context, AliraDatabase.getInstance(context)).also {
                    instancia = it
                }
            }

        /**
         * Só para os testes instrumentados (mobile/src/androidTest/...): cria
         * uma instância isolada, com o banco em memória que o teste montou,
         * sem passar pelo singleton "instancia" acima — cada teste começa do
         * zero, sem um teste vazar dado para o outro.
         */
        fun paraTestes(context: Context, database: AliraDatabase): AliraRepository =
            AliraRepository(context, database)
    }
}
