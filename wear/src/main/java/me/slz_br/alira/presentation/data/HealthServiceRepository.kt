package me.slz_br.alira.presentation.data

import android.content.Context
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.data.IntervalDataPoint
import androidx.health.services.client.unregisterMeasureCallback
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking

class HealthServiceRepository(context: Context) {
    private val healthServiceClient = HealthServices.getClient(context)
    private val measureClient = healthServiceClient.measureClient

    fun caloriesCallbackFlow() = callbackFlow {
        val caloriesCallBack = object : MeasureCallback {
            override fun onAvailabilityChanged(
                dataType: DeltaDataType<*, *>,
                availability: Availability
            ) {
                if (availability is DataTypeAvailability) {
                    trySendBlocking(MeasureMessage.MeasureAvailability(availability))
                }
            }

            override fun onDataReceived(data: DataPointContainer) {
                val caloriesBurned = data.getData(DataType.CALORIES)
                trySendBlocking(MeasureMessage.MeasureData(caloriesBurned))
            }

        }

        measureClient.registerMeasureCallback(DataType.CALORIES, caloriesCallBack)

        awaitClose {
            runBlocking {
                measureClient.unregisterMeasureCallback(DataType.CALORIES, caloriesCallBack)
            }
        }
    }

    sealed class MeasureMessage {
        class MeasureAvailability(val availability: DataTypeAvailability) : MeasureMessage()
        class MeasureData(val data: List<IntervalDataPoint<Double>>) : MeasureMessage()
    }
}