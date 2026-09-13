package me.slz_br.alira.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import me.slz_br.alira.data.db.entities.UsuarioEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Teste de INTEGRAÇÃO do capítulo 8 (item 8.2.2): "tela de cadastro
 * salvando dados no banco". Aqui a gente prova isso sem depender de tela
 * nenhuma — chama o UsuarioDao direto contra um banco Room de verdade,
 * só que em memória (Room.inMemoryDatabaseBuilder), que existe apenas
 * durante o teste e some no final. Isso é o que dá pra rodar em qualquer
 * emulador/dispositivo, sem sujar o alira.db real do app.
 *
 * Como rodar: com o módulo :mobile selecionado, botão direito nesta
 * classe -> Run 'UsuarioDaoTest' (precisa de emulador ligado, porque é um
 * teste instrumentado — roda dentro do Android de verdade, não na JVM
 * comum, já que o Room usa SQLite do sistema Android).
 */
@RunWith(AndroidJUnit4::class)
class UsuarioDaoTest {

    private lateinit var db: AliraDatabase
    private lateinit var dao: me.slz_br.alira.data.db.dao.UsuarioDao

    @Before
    fun criarBancoEmMemoria() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AliraDatabase::class.java)
            // Só em teste: simplifica esperar o resultado sem precisar de
            // coroutine/Flow em alguns pontos. Nunca usar isso no app real
            // (travaria a tela enquanto o banco responde).
            .allowMainThreadQueries()
            .build()
        dao = db.usuarioDao()
    }

    @After
    fun fecharBanco() {
        db.close()
    }

    @Test
    fun inserir_usuario_e_buscar_por_email_retorna_o_mesmo_registro() = runBlocking {
        val id = dao.inserir(
            UsuarioEntity(
                nome = "Usuária Teste",
                email = "teste@alira.com",
                senha = "hash-fake-de-teste",
            )
        )

        val encontrado = dao.obterPorEmail("teste@alira.com")

        assertEquals(id, encontrado?.idUsuario)
        assertEquals("Usuária Teste", encontrado?.nome)
    }

    @Test
    fun buscar_email_que_nao_existe_retorna_null() = runBlocking {
        val encontrado = dao.obterPorEmail("ninguem@alira.com")
        assertNull(encontrado)
    }

    @Test
    fun atualizar_usuario_reflete_na_busca_seguinte() = runBlocking {
        val id = dao.inserir(
            UsuarioEntity(nome = "Nome Antigo", email = "editar@alira.com", senha = "hash1")
        )
        val original = dao.obterPorEmail("editar@alira.com")!!

        dao.atualizar(original.copy(idUsuario = id, nome = "Nome Novo"))

        val atualizado = dao.obterPorEmail("editar@alira.com")
        assertEquals("Nome Novo", atualizado?.nome)
    }
}
