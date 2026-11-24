package me.slz_br.alira.data.sync

import android.os.SystemClock
import android.util.Log
import androidx.health.services.client.PassiveListenerService
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant
import me.slz_br.alira.data.repository.AliraRepository
import me.slz_br.alira.monitoramento.MonitorCardiaco

private const val TAG = "ALIRA_SAUDE"

class MonitoramentoBackgroundService : PassiveListenerService() {
    // PassiveListenerService -> serviço responsável por receber dados de saúde em segundo plano

    private val escopo = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
        /* permite que uma falha em uma tarefa não cancele automaticamente as demais tarefas
         * do escopo de coroutines, que é utilizado para executar tarefas em segundo plano
         * * obs: não entendi direito o que é esse Dispatchers.IO
         */
    )

    override fun onNewDataPointsReceived( // chamado pelo HS quando há novos dados de saúde
        dataPoints: DataPointContainer // conjunto de dados recebidos pelo Health Services.
    ) {

        val leituraCardiaca = dataPoints.getData(
            DataType.HEART_RATE_BPM
        )
        // obtém os dados de freq. cardíaca

        // Atividade do dia (totais desde a meia-noite). Antes este serviço
        // pedia DataType.CALORIES/STEPS/DISTANCE, que não estavam na
        // configuração do Health Services — então nunca chegavam.
        val calorias = dataPoints.getData(DataType.CALORIES_DAILY).lastOrNull()?.value
        val passos = dataPoints.getData(DataType.STEPS_DAILY).lastOrNull()?.value
        val distancia = dataPoints.getData(DataType.DISTANCE_DAILY).lastOrNull()?.value

        if (leituraCardiaca.isEmpty() && calorias == null && passos == null && distancia == null) {
            return
        }

        // Cada leitura de batimento vem com o horário em que foi medida,
        // contado a partir de quando o relógio ligou. "momentoQueLigou"
        // converte isso para data e hora de verdade.
        val momentoQueLigou = Instant.ofEpochMilli(System.currentTimeMillis() - SystemClock.elapsedRealtime())
        leituraCardiaca.forEach { leitura ->
            MonitorCardiaco.novaLeitura(
                context = applicationContext,
                bpm = leitura.value.toFloat(),
                dataHora = leitura.getTimeInstant(momentoQueLigou).toEpochMilli(),
            )
        }
        // O MonitorCardiaco decide se grava (no máximo 1 a cada 10 s) e se
        // o batimento está fora do normal (abre a pergunta "Você está bem?").

        if (calorias != null || passos != null || distancia != null) {
            escopo.launch {
                val repository = AliraRepository.getInstance(applicationContext)
                repository.coletarAtividadeDiaria(
                    usuarioId = repository.garantirUsuarioLocal(),
                    calorias = calorias,
                    passos = passos,
                    distanciaMetros = distancia,
                )
            }
        }

        Log.d(
            TAG,
            "Dados recebidos: ${leituraCardiaca.size} cardíacos, " +
                "calorias=$calorias, passos=$passos, distância=$distancia"
        )
    }

    override fun onPermissionLost() {
        Log.w(
            TAG,
            "Permissão perdida — o monitoramento em segundo plano parou"
        )
    }
    /*
     * Chamado quando o aplicativo perde a permissão necessária
     * para acessar os dados de saúde.
     *
     * Quando isso acontece, o monitoramento em segundo plano deixa
     * de receber novas medições até que a permissão seja concedida novamente.
     */

    override fun onDestroy() {
        escopo.cancel()
        super.onDestroy()
    }
    /*
     * Chamado quando o serviço está sendo destruído.
     *
     * Cancela as coroutines ainda em execução para evitar vazamentos
     * de memória e operações continuando após o encerramento do serviço.
     */
}