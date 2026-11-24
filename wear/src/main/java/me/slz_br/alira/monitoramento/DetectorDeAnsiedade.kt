package me.slz_br.alira.monitoramento

import kotlin.math.max

/**
 * Detecta uma possível CRISE DE ANSIEDADE.
 *
 * Regra (combinada com o grupo):
 *   batimento acima do LIMITE, com a pessoa PARADA, por 2 minutos seguidos.
 *
 * LIMITE = o maior entre 100 bpm e "média da pessoa + 40%".
 *   Ex.: média 70 -> 70 x 1,4 = 98 -> vale 100 (o mínimo).
 *        média 80 -> 80 x 1,4 = 112 -> vale 112.
 * A média da pessoa vem do histórico do banco (últimos 7 dias). Sem
 * histórico ainda, usa 75 bpm, um valor típico de adulto em repouso.
 *
 * "Parada" vem do DetectorDeMovimento (acelerômetro). Se em algum momento o
 * batimento cair abaixo do limite ou a pessoa se mexer, a contagem dos
 * 2 minutos recomeça do zero.
 */
class DetectorDeAnsiedade(private val aoDetectar: () -> Unit) {

    companion object {
        const val MINIMO_BPM = 100.0
        const val FATOR_ACIMA_DA_MEDIA = 1.4
        const val MEDIA_PADRAO = 75.0
        const val DURACAO_MS = 2 * 60_000L
    }

    /** Média de batimentos da pessoa; atualizada pelo serviço a cada 10 minutos. */
    var mediaDaPessoa: Double? = null

    private var inicio: Long? = null

    fun limiteAtual(): Double = max(MINIMO_BPM, (mediaDaPessoa ?: MEDIA_PADRAO) * FATOR_ACIMA_DA_MEDIA)

    /**
     * Chamado a cada poucos segundos com o batimento mais recente.
     * @param tempoMs momento da leitura em milissegundos (sempre crescente).
     */
    fun novaLeitura(bpm: Float, pessoaParada: Boolean, tempoMs: Long) {
        if (bpm > limiteAtual() && pessoaParada) {
            val comeco = inicio ?: tempoMs.also { inicio = it }
            if (tempoMs - comeco >= DURACAO_MS) {
                inicio = null
                aoDetectar()
            }
        } else {
            inicio = null
        }
    }
}
