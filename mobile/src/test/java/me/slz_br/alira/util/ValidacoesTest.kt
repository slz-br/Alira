package me.slz_br.alira.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Teste UNITÁRIO (não instrumentado): roda na JVM comum, em segundos, sem
 * precisar de emulador nem dispositivo — porque Validacoes.emailValido()
 * é Kotlin puro. No Android Studio: botão direito nesta classe -> Run
 * 'ValidacoesTest'. É o teste mais rápido de rodar do projeto todo; bom
 * primeiro teste pra confirmar que o ambiente de testes está funcionando
 * antes de partir pros instrumentados (que precisam de emulador).
 */
class ValidacoesTest {

    @Test
    fun email_com_formato_valido_e_aceito() {
        assertTrue(Validacoes.emailValido("raphael@alira.com"))
        assertTrue(Validacoes.emailValido("ana.paula+teste@alira.com.br"))
    }

    @Test
    fun email_vazio_e_rejeitado() {
        assertFalse(Validacoes.emailValido(""))
    }

    @Test
    fun email_sem_arroba_e_rejeitado() {
        assertFalse(Validacoes.emailValido("raphael.alira.com"))
    }

    @Test
    fun email_sem_dominio_e_rejeitado() {
        assertFalse(Validacoes.emailValido("raphael@"))
    }

    @Test
    fun email_com_espaco_e_rejeitado() {
        assertFalse(Validacoes.emailValido("raphael@ali ra.com"))
    }
}
