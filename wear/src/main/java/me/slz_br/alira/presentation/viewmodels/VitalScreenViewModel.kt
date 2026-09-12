package me.slz_br.alira.presentation.viewmodels

import android.util.Log
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.slz_br.alira.presentation.data.HealthServiceRepository
import me.slz_br.alira.presentation.data.HealthServiceRepository.MeasureMessage

class VitalScreenViewModel(private val healthServiceRepository: HealthServiceRepository): ViewModel() {

    val calories = mutableDoubleStateOf(0.0)

    init {
        viewModelScope.launch {
            healthServiceRepository.caloriesCallbackFlow()
                .collect { measureMessage ->
                    when (measureMessage) {
                        is MeasureMessage.MeasureData -> {
                            val data = measureMessage.data.last().value
                            calories.doubleValue = data
                        }
                        is MeasureMessage.MeasureAvailability -> {
                            Log.d("ALIRA"," ${measureMessage.availability.name}")
                        }
                    }
                }
        }
    }



}