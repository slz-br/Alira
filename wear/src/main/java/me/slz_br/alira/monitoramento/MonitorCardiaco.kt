package me.slz_br.alira.monitoramento

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.slz_br.alira.data.repository.AliraRepository
import me.slz_br.alira.emergencia.TipoEmergencia
import kotlin.math.abs

/**
 * Por onde passa TODA leitura de batimento do app, venha de onde vier:
 * tela de Sinais Vitais, painel de Monitoramento, monitoramento em
 * segundo plano (Health Services) ou monitoramento contínuo.
 *
 * Para cada leitura ele decide duas coisas:
 *  1. GRAVAR no banco? No máximo uma leitura a cada 10 s, para o banco
 *     não encher com dezenas de linhas iguais por minuto.
 *  2. É uma EMERGÊNCIA? Abaixo de 40 ou acima de 160 bpm durante pelo
 *     menos 15 s seguidos -> pergunta "Você está bem?". Os 15 s evitam
 *     disparar por uma leitura errada isolada (ex.: relógio mexendo no
 *     pulso).
 */
object MonitorCardiaco {

    const val LIMITE_BAIXO = 40f
    const val LIMITE_ALTO = 160f

    private const val INTERVALO_GRAVACAO_MS = 10_000L
    private const val TEMPO_ANORMAL_PARA_ALERTAR_MS = 15_000L

    // Leituras antigas (ex.: um lote que o Health Services entrega
    // atrasado) são gravadas, mas não disparam emergência: não adianta
    // perguntar agora sobre algo de vários minutos atrás.
    private const val LEITURA_RECENTE_MS = 2 * 60_000L

    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var ultimaGravacao = 0L
    private var inicioDoAnormal: Long? = null

    /**
     * @param bpm      batimentos por minuto (0 ou menos = sensor sem contato, é ignorado).
     * @param dataHora quando a leitura foi feita (milissegundos, como System.currentTimeMillis()).
     */
    fun novaLeitura(context: Context, bpm: Float, dataHora: Long = System.currentTimeMillis()) {
        if (bpm <= 0f) return
        val app = context.applicationContext
        val agora = System.currentTimeMillis()

        var gravar = false
        var disparar = false

        // synchronized: as leituras podem chegar de várias threads ao mesmo
        // tempo (tela + serviço). Assim uma não atrapalha a conta da outra.
        synchronized(this) {
            gravar = abs(dataHora - ultimaGravacao) >= INTERVALO_GRAVACAO_MS
            if (gravar) ultimaGravacao = dataHora

            val anormal = bpm < LIMITE_BAIXO || bpm > LIMITE_ALTO
            val recente = agora - dataHora <= LEITURA_RECENTE_MS
            if (!anormal) {
                inicioDoAnormal = null
            } else if (recente) {
                val inicio = inicioDoAnormal ?: dataHora.also { inicioDoAnormal = it }
                if (dataHora - inicio >= TEMPO_ANORMAL_PARA_ALERTAR_MS) {
                    disparar = true
                    inicioDoAnormal = null
                }
            }
        }

        if (gravar) {
            escopo.launch {
                val repository = AliraRepository.getInstance(app)
                repository.coletarBiometria(repository.garantirUsuarioLocal(), bpm, dataHora)
            }
        }
        if (disparar) {
            GatilhoDeEmergencia.disparar(app, TipoEmergencia.FREQUENCIA_ANORMAL)
        }
    }
}
