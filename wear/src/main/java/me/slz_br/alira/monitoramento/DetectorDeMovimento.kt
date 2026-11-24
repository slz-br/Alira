package me.slz_br.alira.monitoramento

import kotlin.math.sqrt

/**
 * Diz se a pessoa está PARADA (sentada, deitada, em pé sem andar) ou se
 * mexendo (andando, correndo, fazendo exercício), usando o acelerômetro.
 *
 * Serve para o detector de ansiedade: coração acelerado durante uma
 * corrida é normal; coração acelerado com a pessoa parada é suspeito.
 *
 * Como funciona: a cada 1 segundo, calcula o quanto a força medida pelo
 * acelerômetro "balançou" naquele segundo (desvio padrão). Parado, o
 * balanço é quase zero; andando, ele é grande. Guarda os últimos 30
 * segundos e considera a pessoa parada se a média do balanço ficou abaixo
 * de 0,1 g.
 */
class DetectorDeMovimento {

    companion object {
        const val LIMITE_BALANCO_PARADO = 0.1f * DetectorDeQueda.G
        const val SEGUNDOS_OBSERVADOS = 30
        const val MINIMO_DE_SEGUNDOS = 20
        const val DADO_VELHO_MS = 5_000L
    }

    private val balancoPorSegundo = ArrayDeque<Float>()

    private var inicioDoSegundo = -1L
    private var ultimoSegundoFechado = -1L
    private var soma = 0.0
    private var somaDosQuadrados = 0.0
    private var quantidade = 0

    fun novaAmostra(x: Float, y: Float, z: Float, tempoMs: Long) {
        val forca = sqrt(x * x + y * y + z * z).toDouble()
        if (inicioDoSegundo < 0) inicioDoSegundo = tempoMs

        if (tempoMs - inicioDoSegundo >= 1_000L) {
            fecharSegundo(tempoMs)
            inicioDoSegundo = tempoMs
        }
        soma += forca
        somaDosQuadrados += forca * forca
        quantidade++
    }

    private fun fecharSegundo(tempoMs: Long) {
        if (quantidade >= 2) {
            val media = soma / quantidade
            val variancia = (somaDosQuadrados / quantidade - media * media).coerceAtLeast(0.0)
            balancoPorSegundo.addLast(sqrt(variancia).toFloat())
            while (balancoPorSegundo.size > SEGUNDOS_OBSERVADOS) balancoPorSegundo.removeFirst()
            ultimoSegundoFechado = tempoMs
        }
        soma = 0.0
        somaDosQuadrados = 0.0
        quantidade = 0
    }

    /**
     * true se a pessoa está parada. Responde false (na dúvida, "não está
     * parada") quando ainda não há dados suficientes ou quando o
     * acelerômetro parou de mandar amostras há mais de 5 s.
     */
    fun estaParado(agoraMs: Long): Boolean {
        if (balancoPorSegundo.size < MINIMO_DE_SEGUNDOS) return false
        if (agoraMs - ultimoSegundoFechado > DADO_VELHO_MS) return false
        return balancoPorSegundo.average() < LIMITE_BALANCO_PARADO
    }
}
