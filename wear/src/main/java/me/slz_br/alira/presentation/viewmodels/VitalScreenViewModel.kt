package me.slz_br.alira.presentation.viewmodels

import android.util.Log
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import me.slz_br.alira.presentation.data.HealthServiceRepository
import me.slz_br.alira.presentation.data.HealthServiceRepository.MeasureMessage

class VitalScreenViewModel(private val healthServiceRepository: HealthServiceRepository): ViewModel() {

    val calories = mutableDoubleStateOf(0.0)

    init {
        viewModelScope.launch {
            healthServiceRepository.caloriesCallbackFlow()
                .takeWhile { true }
                .collect { measureMessage ->
                    when (measureMessage) {
                        is MeasureMessage.MeasureData -> {
                            val data = measureMessage.data.last().value
                            Log.d("Recebendo dados:", "$data")
                            calories.doubleValue = data
                        }
                        is MeasureMessage.MeasureAvailability -> {

                        }
                    }

                }
        }
    }



}