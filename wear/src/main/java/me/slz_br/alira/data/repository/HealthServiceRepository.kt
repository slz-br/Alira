package me.slz_br.alira.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.PassiveListenerCallback
import androidx.health.services.client.awaitWithException
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.data.PassiveListenerConfig
import me.slz_br.alira.data.sync.MonitoramentoBackgroundService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val TAG = "ALIRA_SAUDE"

sealed interface LeituraCardiaca {
    data class Bpm(val valor: Double) : LeituraCardiaca
    data class Disponibilidade(val estado: DataTypeAvailability) : LeituraCardiaca
    data class Falha(val motivo: String) : LeituraCardiaca
    object NaoSuportado : LeituraCardiaca
}

sealed interface LeituraDiaria {
    data class Dados(
        val calorias: Double? = null,
        val passos: Long? = null,
        val distanciaMetros: Double? = null,
    ) : LeituraDiaria

    data class Falha(val motivo: String) : LeituraDiaria
    object SemPermissao : LeituraDiaria
}

class HealthServiceRepository(context: Context) {

    private val appContext = context.applicationContext
    private val healthServiceClient = HealthServices.getClient(appContext)
    private val measureClient = healthServiceClient.measureClient
    private val passiveMonitoringClient = healthServiceClient.passiveMonitoringClient

    suspend fun suportaFrequenciaCardiaca(): Boolean =
        suportaMedicaoAoVivo(DataType.HEART_RATE_BPM)

    private suspend fun suportaMedicaoAoVivo(tipo: DeltaDataType<*, *>): Boolean = try {
        val capacidades = measureClient.getCapabilitiesAsync().awaitWithException()
        tipo in capacidades.supportedDataTypesMeasure
    } catch (erro: Throwable) {
        Log.w(TAG, "Não foi possível consultar as capacidades de medição", erro)
        false
    }

    fun fluxoFrequenciaCardiaca(): Flow<LeituraCardiaca> =
        fluxoDeMedicao(DataType.HEART_RATE_BPM) { dados ->
            dados.getData(DataType.HEART_RATE_BPM).lastOrNull()?.value
        }

    private fun fluxoDeMedicao(
        tipo: DeltaDataType<*, *>,
        extrair: (DataPointContainer) -> Double?,
    ): Flow<LeituraCardiaca> = callbackFlow {
        if (!suportaMedicaoAoVivo(tipo)) {
            Log.i(TAG, "Medição ao vivo de $tipo não suportada neste aparelho")
            trySend(LeituraCardiaca.NaoSuportado)
            awaitClose { }
            return@callbackFlow
        }

        val callback = object : MeasureCallback {
            override fun onRegistered() {
                Log.d(TAG, "Medição de $tipo registrada")
            }

            override fun onRegistrationFailed(throwable: Throwable) {
                Log.w(TAG, "Registro de medição recusado", throwable)
                trySend(LeituraCardiaca.Falha(throwable.message ?: "registro recusado"))
            }

            override fun onAvailabilityChanged(
                dataType: DeltaDataType<*, *>,
                availability: Availability,
            ) {
                if (availability is DataTypeAvailability) {
                    trySend(LeituraCardiaca.Disponibilidade(availability))
                }
            }

            override fun onDataReceived(data: DataPointContainer) {
                val valor = extrair(data) ?: return
                trySend(LeituraCardiaca.Bpm(valor))
            }
        }

        try {
            measureClient.registerMeasureCallback(tipo, callback)
        } catch (erro: Throwable) {
            Log.w(TAG, "Falha ao registrar o callback de medição", erro)
            trySend(LeituraCardiaca.Falha(erro.message ?: "falha ao iniciar a medição"))
        }

        awaitClose {
            runCatching { measureClient.unregisterMeasureCallbackAsync(tipo, callback) }
                .onFailure { Log.w(TAG, "Falha ao desregistrar a medição", it) }
        }
    }

    fun ligarMonitoramentoEmSegundoPlano() {
        // Batimentos sempre; atividade do dia (calorias, passos, distância)
        // só se a permissão de atividade física foi dada — pedir um dado
        // sem a permissão faz o Health Services recusar o pedido inteiro.
        val tipos = mutableSetOf<DataType<*, *>>(DataType.HEART_RATE_BPM)
        val temPermissaoDeAtividade = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACTIVITY_RECOGNITION,
        ) == PackageManager.PERMISSION_GRANTED
        if (temPermissaoDeAtividade) {
            tipos += setOf(DataType.CALORIES_DAILY, DataType.STEPS_DAILY, DataType.DISTANCE_DAILY)
        }
        val config = PassiveListenerConfig.builder()
            .setDataTypes(tipos)
            .build()
        runCatching {
            passiveMonitoringClient
                .setPassiveListenerServiceAsync(MonitoramentoBackgroundService::class.java, config)
        }
            .onSuccess { Log.d(TAG, "Monitoramento em segundo plano solicitado") }
            .onFailure { Log.w(TAG, "Não foi possível ligar o monitoramento em segundo plano", it) }
    }

    fun desligarMonitoramentoEmSegundoPlano() {
        runCatching { passiveMonitoringClient.clearPassiveListenerServiceAsync() }
            .onFailure { Log.w(TAG, "Falha ao desligar o monitoramento em segundo plano", it) }
    }

    fun fluxoMetricasDiarias(): Flow<LeituraDiaria> = callbackFlow {
        val config = PassiveListenerConfig.builder()
            .setDataTypes(
                setOf<DataType<*, *>>(
                    DataType.CALORIES_DAILY,
                    DataType.STEPS_DAILY,
                    DataType.DISTANCE_DAILY,
                )
            )
            .build()

        val callback = object : PassiveListenerCallback {
            override fun onRegistered() {
                Log.d(TAG, "Monitoramento diário registrado (calorias/passos/distância)")
            }

            override fun onRegistrationFailed(throwable: Throwable) {
                Log.w(TAG, "Registro do monitoramento diário recusado", throwable)
                trySend(LeituraDiaria.Falha(throwable.message ?: "registro recusado"))
            }

            override fun onPermissionLost() {
                Log.w(TAG, "Permissão de atividade física perdida")
                trySend(LeituraDiaria.SemPermissao)
            }

            override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
                val calorias = dataPoints.getData(DataType.CALORIES_DAILY).lastOrNull()?.value
                val passos = dataPoints.getData(DataType.STEPS_DAILY).lastOrNull()?.value
                val distancia = dataPoints.getData(DataType.DISTANCE_DAILY).lastOrNull()?.value

                if (calorias == null && passos == null && distancia == null) return
                trySend(
                    LeituraDiaria.Dados(
                        calorias = calorias,
                        passos = passos,
                        distanciaMetros = distancia,
                    )
                )
            }
        }

        try {
            passiveMonitoringClient.setPassiveListenerCallback(config, callback)
        } catch (erro: Throwable) {
            Log.w(TAG, "Falha ao registrar o monitoramento diário", erro)
            trySend(LeituraDiaria.Falha(erro.message ?: "falha ao iniciar o monitoramento"))
        }

        awaitClose {
            runCatching { passiveMonitoringClient.clearPassiveListenerCallbackAsync() }
                .onFailure { Log.w(TAG, "Falha ao desregistrar o monitoramento diário", it) }
        }
    }
}
