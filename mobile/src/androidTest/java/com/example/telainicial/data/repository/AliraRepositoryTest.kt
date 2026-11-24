package com.example.telainicial.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.telainicial.data.db.AliraDatabase
import com.example.telainicial.data.db.entities.MonitoramentoEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Teste instrumentado (precisa de emulador ou celular): usa o
 * AliraRepository de verdade, só que com um banco em memória, para não
 * mexer nos dados reais do app.
 *
 * No Android Studio: botão direito nesta classe -> Run 'AliraRepositoryTest'.
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

    private fun perfil(email: String = "ana@alira.com", cuidadorNome: String = "Maria") = DadosPerfil(
        nome = "Ana",
        email = email,
        telefone = "11987654321",
        dataNascimento = null,
        deficiencia = "AUDITIVA",
        cuidadorNome = cuidadorNome,
        cuidadorTelefone = if (cuidadorNome.isBlank()) "" else "11912345678",
        cuidadorEmail = "maria@alira.com",
        cuidadorParentesco = "Mãe",
    )

    /** Salvar o Perfil grava o usuário e o cuidador, que vira o contato do SMS. */
    @Test
    fun salvar_perfil_grava_usuario_e_cuidador() = runBlocking {
        assertTrue(repository.salvarPerfil(perfil()))

        val contatos = repository.contatosDeEmergencia("ana@alira.com")
        assertEquals(1, contatos.size)
        assertEquals("Maria", contatos[0].nome)
        assertEquals("maria@alira.com", contatos[0].email)
    }

    /** Salvar de novo ATUALIZA (o celular tem um usuário e um cuidador só). */
    @Test
    fun salvar_perfil_duas_vezes_nao_duplica() = runBlocking {
        repository.salvarPerfil(perfil())
        repository.salvarPerfil(perfil(cuidadorNome = "Joana"))

        val contatos = repository.contatosDeEmergencia("ana@alira.com")
        assertEquals(1, contatos.size)
        assertEquals("Joana", contatos[0].nome)
    }

    /** Sem e-mail o perfil é salvo, mas não vai para o relógio. */
    @Test
    fun perfil_sem_email_nao_vai_para_o_relogio() = runBlocking {
        assertFalse(repository.salvarPerfil(perfil(email = "")))
    }

    /** A mesma leitura chegando duas vezes não duplica (índice único). */
    @Test
    fun leitura_repetida_e_ignorada_e_media_e_calculada() = runBlocking {
        val leituras = listOf(
            MonitoramentoEntity(idNoRelogio = 1, dataHora = 1_000L, frequenciaCardiaca = 70.0),
            MonitoramentoEntity(idNoRelogio = 2, dataHora = 2_000L, frequenciaCardiaca = 90.0),
        )
        assertEquals(2, repository.salvarLeituras(leituras))
        assertEquals(0, repository.salvarLeituras(leituras))

        assertEquals(80.0, repository.mediaBatimentos(0L, 10_000L)!!, 0.001)
        assertNull(repository.mediaBatimentos(50_000L, 60_000L))
    }
}
