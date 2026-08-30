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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import me.slz_br.alira.presentation.theme.AliraTheme
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import me.slz_br.alira.R

object HeartRate : SensorEventListener {

    val currentBPM = mutableFloatStateOf(0f)

    @Composable
    fun HeartRateScreen() {
        val ctx = LocalContext.current
        ActivityResultContracts.RequestPermission()
        val sensorManager = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val hSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        sensorManager.registerListener(this, hSensor, SensorManager.SENSOR_DELAY_FASTEST)

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
                            ListHeader(modifier =
                                Modifier
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight(800),
                                    text = "Sinais Vitais"
                                )
                            }
                        }
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

@Preview()
@Composable
private fun HeartRateScreenPreview() {
    HeartRate.HeartRateScreen()
}