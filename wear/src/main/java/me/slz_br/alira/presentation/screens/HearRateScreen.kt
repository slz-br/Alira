package me.slz_br.alira.presentation.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.delay
import me.slz_br.alira.data.repository.AliraRepository
import me.slz_br.alira.presentation.theme.AliraTheme
import androidx.compose.ui.res.painterResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.material3.Icon
import me.slz_br.alira.R

// Intervalo entre leituras salvas no banco. O sensor dispara com frequência
// bem maior (SENSOR_DELAY_FASTEST) só para atualizar o texto na tela; salvar
// cada evento individualmente encheria o banco sem necessidade.
private const val INTERVALO_REGISTRO_MS = 5_000L

object HeartRate : SensorEventListener {

    val currentBPM = mutableFloatStateOf(0f)

    @Composable
    fun HeartRateScreen() {
        val ctx = LocalContext.current
        val repository = remember { AliraRepository.getInstance(ctx) }
        var usuarioId by remember { mutableLongStateOf(0L) }

        DisposableEffect(Unit) {
            ActivityResultContracts.RequestPermission()
            val sensorManager = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val hSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
            sensorManager.registerListener(this@HeartRate, hSensor, SensorManager.SENSOR_DELAY_FASTEST)
            onDispose { sensorManager.unregisterListener(this@HeartRate) }
        }

        // Garante que existe um usuário local e, a partir daí, grava a
        // frequência cardíaca atual no banco a cada INTERVALO_REGISTRO_MS,
        // alimentando o histórico usado nas telas de relatório (RF01, RF12).
        LaunchedEffect(Unit) {
            usuarioId = repository.garantirUsuarioLocal()
            while (true) {
                delay(INTERVALO_REGISTRO_MS)
                repository.coletarBiometria(usuarioId, currentBPM.floatValue)
            }
        }

        AliraTheme {
            AppScaffold {
                val listState = rememberTransformingLazyColumnState()
                ScreenScaffold(
                    scrollState = listState,
                ) { contentPadding ->
                    TransformingLazyColumn(
                        verticalArrangement = Arrangement.Center,
                        contentPadding = contentPadding
                    ) {
                        item {
                            Row() {
                                Icon(
                                    painter = painterResource(R.drawable.heart_rate),
                                    contentDescription = "Vital signs icon"
                                )
                                Text("BPM: ${currentBPM.floatValue}")

                            }
                        }
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_HEART_RATE) return

        val bpm = event.values[0]
        Log.d("health", "bpm: $bpm")

        currentBPM.floatValue = bpm
    }
}