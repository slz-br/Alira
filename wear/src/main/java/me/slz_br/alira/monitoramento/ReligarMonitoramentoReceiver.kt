package me.slz_br.alira.monitoramento

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Quando o relógio é desligado e ligado de novo, todos os serviços param.
 * Este "receptor" é avisado pelo Android assim que o relógio termina de
 * ligar (BOOT_COMPLETED) e, se o botão "Monitoramento contínuo" estava
 * ligado, liga o monitoramento de novo — sem a pessoa precisar abrir o app.
 */
class ReligarMonitoramentoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (PreferenciasMonitoramento.estaLigado(context)) {
            MonitoramentoContinuoService.ligar(context)
        }
    }
}
