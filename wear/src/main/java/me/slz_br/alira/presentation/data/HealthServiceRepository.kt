package me.slz_br.alira.presentation.data

import android.content.Context
import android.util.Log
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.awaitWithException
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.data.IntervalDataPoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking

class HealthServiceRepository(context: Context) {
    private val healthServiceClient = HealthServices.getClient(context)
    private val measureClient = healthServiceClient.measureClient

    fun caloriesCallbackFlow() = callbackFlow {
        val callback = object : MeasureCallback {
            override fun onAvailabilityChanged(
                dataType: DeltaDataType<*, *>,
                availability: Availability
            ) {
                if (availability is DataTypeAvailability) {
                    Log.d("ALIRA" ,"updating availability")
                    trySendBlocking(MeasureMessage.MeasureAvailability(availability))
                }
            }

            override fun onDataReceived(data: DataPointContainer) {
                val caloriesBurned = data.getData(DataType.CALORIES)
                Log.d("ALIRA" ,"updating calories")
                trySendBlocking(MeasureMessage.MeasureData(caloriesBurned))
            }
        }

        Log.d("ALIRA", "Registando monitoramento de calorias")
        measureClient.registerMeasureCallback(DataType.CALORIES, callback)

        awaitClose {
            Log.d("ALIRA", "desregistando monitoramento de calorias")
            runBlocking {
                measureClient.unregisterMeasureCallbackAsync(DataType.CALORIES, callback)
                    .awaitWithException()
            }
        }
    }

    sealed class MeasureMessage {
        class MeasureAvailability(val availability: DataTypeAvailability) : MeasureMessage()
        class MeasureData(val data: List<IntervalDataPoint<Double>>) : MeasureMessage()
    }
}