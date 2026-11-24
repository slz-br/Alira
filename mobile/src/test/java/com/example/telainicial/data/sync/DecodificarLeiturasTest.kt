package com.example.telainicial.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Teste unitário do "tradutor" do pacote de leituras que o relógio manda
 * (uma leitura por linha, campos separados por ";").
 */
class DecodificarLeiturasTest {

    @Test
    fun linha_de_batimento_e_linha_de_atividade_sao_lidas() {
        val texto = "10;1700000000000;78.0;;;\n11;1700000060000;;3820;412.5;2700.0"
        val leituras = decodificarLeituras(texto)

        assertEquals(2, leituras.size)
        assertEquals(10L, leituras[0].idNoRelogio)
        assertEquals(78.0, leituras[0].frequenciaCardiaca!!, 0.001)
        assertNull(leituras[0].passos)
        assertEquals(3820L, leituras[1].passos)
        assertEquals(2700.0, leituras[1].distancia!!, 0.001)
    }

    @Test
    fun linha_estragada_e_pulada_sem_perder_as_outras() {
        val texto = "lixo\n12;1700000120000;80.0;;;\n;;;"
        val leituras = decodificarLeituras(texto)

        assertEquals(1, leituras.size)
        assertEquals(12L, leituras[0].idNoRelogio)
    }
}
