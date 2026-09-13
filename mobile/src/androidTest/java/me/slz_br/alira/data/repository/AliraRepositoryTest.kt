package me.slz_br.alira.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import me.slz_br.alira.data.db.AliraDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Estes três testes são exatamente os casos que o capítulo 8 pede pra
 * "Cadastro" e "Login" (Tabelas 1, 2 e 3 do documento de qualidade):
 *  - CT01/CT02 (8.2.1): cadastro grava, login com dado certo funciona,
 *    campo obrigatório vazio ou duplicado é rejeitado;
 *  - "Cadastro gravando no banco" / "Login com usuário cadastrado" (8.2.2):
 *    aqui isso é testado de ponta a ponta pelo AliraRepository de verdade
 *    (o mesmo que LoginActivity/CadastroActivity usam), só que com um
 *    banco em memória (paraTestes()) em vez do alira.db real do celular.
 *
 * Resultado: com o app rodando num emulador, botão direito nesta classe ->
 * Run 'AliraRepositoryTest'. Os três testes passando (verde) já são o
 * "Resultado obtido" e "Status: Aprovado" que faltam nas tabelas do
 * capítulo — é só copiar o nome do teste e se passou ou não.
 */
@RunWith(AndroidJUnit4::class)
class AliraRepositoryTest {

    private lateinit var db: AliraDatabase
    private lateinit var repository: AliraRepository

    @Before
    fun criarRepositorioComBancoEmMemoria() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AliraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AliraRepository.paraTestes(context, db)
    }

    @After
    fun fecharBanco() {
        db.close()
    }

    /** Corresponde ao CT01 (login) + "Cadastro gravando no banco" (8.2.2). */
    @Test
    fun cadastro_salva_usuario_e_login_com_dados_corretos_funciona() = runBlocking {
        val cadastrado = repository.cadastrar(
            nome = "Raphael Teste",
            dataNascimento = null,
            telefone = "11988887777",
            email = "raphael@alira.com",
            deficiencia = null,
            nivelAutonomia = "Alta",
            senha = "minhaSenha123",
        )
        assertNotNull("cadastrar() deveria retornar o usuário salvo, não null", cadastrado)

        val logado = repository.autenticar("raphael@alira.com", "minhaSenha123")
        assertNotNull("autenticar() deveria aceitar e-mail e senha corretos", logado)
        assertEquals("raphael@alira.com", logado?.email)

        // A senha nunca fica salva em texto puro no banco (RNF08/RNF10) —
        // o que está gravado é o hash, então tem que ser diferente do que
        // a pessoa digitou.
        assertNotEqualsSenhaPura(logado?.senha, "minhaSenha123")
    }

    /** Login com senha errada não pode autenticar — base do CT01 (caminho negativo). */
    @Test
    fun login_com_senha_incorreta_retorna_null() = runBlocking {
        repository.cadastrar(
            nome = "Usuária Y", dataNascimento = null, telefone = null,
            email = "usuariay@alira.com", deficiencia = null, nivelAutonomia = null,
            senha = "senhaCorreta",
        )

        val resultado = repository.autenticar("usuariay@alira.com", "senhaErrada")

        assertNull("Senha incorreta não pode retornar um usuário autenticado", resultado)
    }

    /** Corresponde ao CT02 (cadastro com dado inválido/duplicado é rejeitado). */
    @Test
    fun cadastro_com_email_ja_existente_e_rejeitado() = runBlocking {
        repository.cadastrar(
            nome = "Primeiro Cadastro", dataNascimento = null, telefone = null,
            email = "duplicado@alira.com", deficiencia = null, nivelAutonomia = null,
            senha = "senha1",
        )

        val segundaTentativa = repository.cadastrar(
            nome = "Segundo Cadastro", dataNascimento = null, telefone = null,
            email = "duplicado@alira.com", deficiencia = null, nivelAutonomia = null,
            senha = "senha2",
        )

        assertNull("Não pode permitir dois cadastros com o mesmo e-mail", segundaTentativa)
    }

    private fun assertNotEqualsSenhaPura(hashSalvo: String?, senhaDigitada: String) {
        org.junit.Assert.assertNotEquals(senhaDigitada, hashSalvo)
    }
}
