package me.slz_br.alira.presentation.screens

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import me.slz_br.alira.R
import me.slz_br.alira.presentation.theme.AliraTheme

@Composable
fun WearApp(
    onHeartRateClick: () -> Unit = {},
    onGenerateReportClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onInfoScreenClick: () -> Unit = {}
) {
    AliraTheme {
        AppScaffold {
            val listState = rememberTransformingLazyColumnState()
            val transformationSpec = rememberTransformationSpec()
            ScreenScaffold(
                scrollState = listState,
                edgeButton = {
                    EdgeButton(
                        onClick = onInfoScreenClick,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                    ) {
                        Text("Créditos")
                    }
                },
            ) { contentPadding -> // ScreenScaffold provides default padding; adjust as needed
                TransformingLazyColumn(contentPadding = contentPadding, state = listState) {
                    item {
                        ListHeader(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                        ) {
                            Text(text = stringResource(R.string.hello_world, "Usuário!"))
                        }
                    }
                    item {
                        FilledIconButton(
                            onClick = onHeartRateClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .transformedHeight(this, transformationSpec)
                        ) {
                            Row {
                                Icon(
                                    painter = painterResource(R.drawable.heart_icon),
                                    contentDescription = "Botão de frequência cardiaca"
                                )
                                Text(
                                    text = "Freq. Cardiaca"
                                )
                            }
                        }
                    }
                    item {
                        FilledIconButton (
                            onClick = onGenerateReportClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .transformedHeight(this, transformationSpec),
                        ) {
                            Row {
                                Icon(
                                    painter = painterResource(R.drawable.chart_icon),
                                    contentDescription = "Gerar relatório"
                                )
                                Text("Gerar relatório")
                            }
                        }
                    }
                    item {
                        FilledIconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row {
                                Icon(
                                    painter = painterResource(R.drawable.settings_icon),
                                    contentDescription = "Engrenagem de configurações"
                                )
                                Text("Configurações")
                            }
                        }
                    }
                }
            }
        }
    }
}