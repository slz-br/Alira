package me.slz_br.alira.data.sync

import android.util.Log
import androidx.health.services.client.PassiveListenerService
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.slz_br.alira.data.repository.AliraRepository

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

        val leituraCalorias = dataPoints.getData(
            DataType.CALORIES
        )

        val leituraPassos = dataPoints.getData(
            DataType.STEPS
        )

        val leituraDistancia = dataPoints.getData(
            DataType.DISTANCE
        )
        // obtém os dados de freq. cardíaca

        if (
            leituraCardiaca.isEmpty() &&
            leituraCalorias.isEmpty() &&
            leituraPassos.isEmpty() &&
            leituraDistancia.isEmpty()
        ) {
            return
        }

        escopo.launch {

            val repository = AliraRepository.getInstance(
                applicationContext
            )

            // Obs: não entendi direito as partes acima


            val usuarioId = repository.garantirUsuarioLocal()

            leituraCardiaca.forEach { leitura ->
                repository.coletarBiometria(
                    usuarioId = usuarioId,
                    frequenciaCardiaca = leitura.value.toFloat()
                )
            }
            // salva a medição de freq. cardíaca no repo
            // o valor recebido é convertido para float antes de ir ao repo

            leituraCalorias.forEach { leitura ->
                repository.coletarCalorias(
                    usuarioId = usuarioId,
                    calorias = leitura.value.toFloat()
                )
            }

            leituraPassos.forEach { leitura ->
                repository.coletarPassos(
                    usuarioId = usuarioId,
                    passos = leitura.value.toLong()
                )
            }

            leituraDistancia.forEach { leitura ->
                repository.coletarDistancia(
                    usuarioId = usuarioId,
                    distancia = leitura.value.toFloat()
                )
            }

            Log.d(
                TAG,
                "Dados recebidos: " +
                        "${leituraCardiaca.size} cardíacos, " +
                        "${leituraCalorias.size} calorias, " +
                        "${leituraPassos.size} passos, " +
                        "${leituraDistancia.size} distâncias"
            )
            // registra quantas leituras foram armazenadas
        }
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