package me.slz_br.alira.monitoramento

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Os números que decidem o que é uma queda. Existem dois jogos:
 *  - NORMAL: para o dia a dia (mais rigoroso, evita alarme falso);
 *  - DEMONSTRACAO: mais sensível, para cair num colchão na apresentação
 *    (o colchão amortece o impacto e a queda é mais "suave").
 *
 * Forças em "g" (1 g = a gravidade, 9,8 m/s²) e tempos em milissegundos.
 */
data class LimitesQueda(
    val quedaLivreG: Float,
    val impactoG: Float,
    val janelaQuedaLivreMs: Long,
    val esperaAposImpactoMs: Long,
    val janelaImobilidadeMs: Long,
    val variacaoMaximaParadoG: Float,
) {
    companion object {
        val NORMAL = LimitesQueda(
            quedaLivreG = 0.6f,
            impactoG = 2.5f,
            janelaQuedaLivreMs = 1_000L,
            esperaAposImpactoMs = 1_500L,
            janelaImobilidadeMs = 2_500L,
            variacaoMaximaParadoG = 0.35f,
        )
        val DEMONSTRACAO = LimitesQueda(
            quedaLivreG = 0.8f,
            impactoG = 1.8f,
            janelaQuedaLivreMs = 1_500L,
            esperaAposImpactoMs = 1_000L,
            janelaImobilidadeMs = 2_000L,
            variacaoMaximaParadoG = 0.5f,
        )
    }
}

/**
 * Detecta uma possível QUEDA pelo acelerômetro do relógio.
 *
 * O acelerômetro mede a aceleração em 3 eixos (x, y, z) em m/s². Parado,
 * ele mede só a gravidade: cerca de 9,8 m/s², que chamamos de "1 g".
 * Juntando os 3 eixos numa só força (a "magnitude", raiz de x²+y²+z²),
 * uma queda tem uma "assinatura" em 3 fases:
 *
 *  1. QUEDA LIVRE: enquanto o corpo cai, a força medida diminui
 *     (abaixo de 0,6 g no modo normal) — a sensação de "frio na barriga".
 *  2. IMPACTO: logo depois, um pico forte ao bater no chão (acima de 2,5 g
 *     no modo normal).
 *  3. IMOBILIDADE: espera a pessoa "assentar" e então observa alguns
 *     segundos. Se o braço ficou praticamente parado, a pessoa pode estar
 *     caída -> dispara. Se ela se mexeu bastante (levantou, continuou
 *     andando), provavelmente está bem -> não dispara.
 *
 * Exigir as 3 fases evita alarme falso com movimentos comuns, como bater
 * a mão na mesa (tem impacto, mas não tem queda livre antes) ou sentar
 * rápido no sofá (a pessoa continua se mexendo depois).
 *
 * É uma classe Kotlin pura (não usa nada do Android): recebe as amostras
 * e chama [aoDetectar] quando reconhece uma queda. O painel de
 * monitoramento lê [fase] e [progresso] para mostrar o que está acontecendo.
 */
class DetectorDeQueda(private val aoDetectar: () -> Unit) {

    companion object {
        const val G = 9.80665f
    }

    /** Em que ponto o detector está (o painel ao vivo mostra isto). */
    enum class Fase { VIGIANDO, ASSENTANDO, CONFERINDO_IMOBILIDADE }

    /** Os limites em uso; o serviço troca para DEMONSTRACAO quando o modo é ligado. */
    var limites: LimitesQueda = LimitesQueda.NORMAL

    var fase = Fase.VIGIANDO
        private set

    private var ultimaQuedaLivre = Long.MIN_VALUE
    private var momentoDoImpacto = 0L
    private var menorForca = Float.MAX_VALUE
    private var maiorForca = 0f

    /**
     * Uma amostra do acelerômetro.
     * @param tempoMs momento da amostra em milissegundos (sempre crescente).
     */
    fun novaAmostra(x: Float, y: Float, z: Float, tempoMs: Long) {
        val forca = sqrt(x * x + y * y + z * z)
        val l = limites

        when (fase) {
            Fase.VIGIANDO -> {
                if (forca < l.quedaLivreG * G) ultimaQuedaLivre = tempoMs

                val teveQuedaLivreAntes = ultimaQuedaLivre != Long.MIN_VALUE &&
                    tempoMs - ultimaQuedaLivre <= l.janelaQuedaLivreMs
                if (forca > l.impactoG * G && teveQuedaLivreAntes) {
                    fase = Fase.ASSENTANDO
                    momentoDoImpacto = tempoMs
                    menorForca = Float.MAX_VALUE
                    maiorForca = 0f
                }
            }

            Fase.ASSENTANDO, Fase.CONFERINDO_IMOBILIDADE -> {
                val desdeOImpacto = tempoMs - momentoDoImpacto
                if (desdeOImpacto < l.esperaAposImpactoMs) return
                fase = Fase.CONFERINDO_IMOBILIDADE

                menorForca = min(menorForca, forca)
                maiorForca = max(maiorForca, forca)
                if (maiorForca - menorForca > l.variacaoMaximaParadoG * G) {
                    // Se mexeu bastante: não é uma pessoa caída.
                    reiniciar()
                    return
                }
                if (desdeOImpacto >= l.esperaAposImpactoMs + l.janelaImobilidadeMs) {
                    reiniciar()
                    aoDetectar()
                }
            }
        }
    }

    /**
     * De 0 a 1: quanto falta para confirmar a queda depois do impacto
     * (0 quando está só vigiando). O painel usa para encher o anel.
     */
    fun progresso(tempoMs: Long): Float {
        if (fase == Fase.VIGIANDO) return 0f
        val total = limites.esperaAposImpactoMs + limites.janelaImobilidadeMs
        return ((tempoMs - momentoDoImpacto).toFloat() / total).coerceIn(0f, 1f)
    }

    private fun reiniciar() {
        fase = Fase.VIGIANDO
        ultimaQuedaLivre = Long.MIN_VALUE
    }
}
