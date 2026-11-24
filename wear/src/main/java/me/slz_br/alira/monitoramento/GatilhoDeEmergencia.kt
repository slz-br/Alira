package me.slz_br.alira.monitoramento

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import me.slz_br.alira.emergencia.CentralDeEmergencia
import me.slz_br.alira.emergencia.TipoEmergencia

/**
 * Porta de entrada dos detectores AUTOMÁTICOS para a emergência.
 *
 * Os detectores (queda, ansiedade, batimento fora do normal) não chamam a
 * CentralDeEmergencia direto: passam por aqui, que faz duas coisas:
 *
 *  1. Intervalo de descanso: depois de disparar um tipo de emergência, o
 *     mesmo tipo só pode disparar de novo após 10 minutos. Sem isso, um
 *     batimento que continua alto perguntaria "Você está bem?" a cada
 *     poucos segundos, mesmo depois de a pessoa responder "Estou bem".
 *  2. Thread certa: os sensores podem avisar de outra thread; a
 *     CentralDeEmergencia precisa ser chamada na thread principal.
 *
 * Os botões de teste das Configurações continuam chamando a
 * CentralDeEmergencia direto (sem intervalo de descanso).
 */
object GatilhoDeEmergencia {

    private const val TAG = "ALIRA_MONITORAMENTO"
    private const val DESCANSO_MS = 10 * 60_000L

    // No modo demonstração o descanso é curto: dá para cair no colchão de
    // novo poucos segundos depois (na apresentação, várias vezes seguidas).
    private const val DESCANSO_DEMONSTRACAO_MS = 15_000L

    private val principal = Handler(Looper.getMainLooper())

    // Quando cada tipo disparou pela última vez (relógio que não volta
    // atrás: SystemClock.elapsedRealtime, tempo desde que o relógio ligou).
    private val ultimoDisparo = mutableMapOf<TipoEmergencia, Long>()

    fun disparar(context: Context, tipo: TipoEmergencia) {
        val app = context.applicationContext
        principal.post {
            val agora = SystemClock.elapsedRealtime()
            val ultimo = ultimoDisparo[tipo]
            val descanso = if (PreferenciasMonitoramento.demonstracaoLigada(app)) DESCANSO_DEMONSTRACAO_MS else DESCANSO_MS
            when {
                CentralDeEmergencia.emAndamento ->
                    Log.i(TAG, "Emergência já em andamento; ${tipo.codigo} ignorada")

                ultimo != null && agora - ultimo < descanso ->
                    Log.i(TAG, "${tipo.codigo} disparou há pouco; aguardando o intervalo de descanso")

                else -> {
                    ultimoDisparo[tipo] = agora
                    Log.w(TAG, "Disparando emergência automática: ${tipo.codigo}")
                    CentralDeEmergencia.iniciar(app, tipo)
                }
            }
        }
    }
}
