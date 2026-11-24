package com.example.telainicial.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Teste unitário (roda sem emulador): confere a "tradução" entre o que
 * aparece no Perfil ("Auditiva, Visual") e o código que vai para o relógio
 * ("AUDITIVA,VISUAL").
 */
class DeficienciasTest {

    @Test
    fun juntar_e_separar_sao_o_caminho_de_ida_e_volta() {
        val codigos = listOf(Deficiencias.AUDITIVA, Deficiencias.VISUAL)
        val texto = Deficiencias.juntar(codigos)
        assertEquals("AUDITIVA,VISUAL", texto)
        assertEquals(codigos, Deficiencias.separar(texto))
    }

    @Test
    fun nenhuma_marcada_vira_null() {
        assertNull(Deficiencias.juntar(emptyList()))
    }

    @Test
    fun nomes_mostra_o_texto_bonito_e_ignora_codigo_desconhecido() {
        assertEquals("Auditiva, Física", Deficiencias.nomes("AUDITIVA, FISICA,XYZ"))
        assertEquals("", Deficiencias.nomes(null))
    }
}
