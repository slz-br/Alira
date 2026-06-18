package me.slz_br.alira.presentation.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import me.slz_br.alira.R
import me.slz_br.alira.presentation.theme.AliraTheme

@Preview(showBackground = true)
@Composable
fun GenerateReportScreen() {
    AliraTheme {
        AppScaffold {
            val listState = rememberTransformingLazyColumnState()
            ScreenScaffold(
                scrollState = listState,
            ) {
                TransformingLazyColumn() {
                    item {
                        Text(
                            modifier = Modifier.padding(0.dp, 10.dp),
                            fontWeight = FontWeight(800),
                            text = "Relatórios"
                        )
                    }
                    item {
                        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                            Row {
                                Icon(painter = painterResource(R.drawable.graph_icon), "gráfico")
                                Text(
                                    textAlign = TextAlign.Center,
                                    text = "Gerar Relatório"
                                )
                            }
                        }
                    }
                    item {
                        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                textAlign = TextAlign.Center,
                                text = "Ver Relatório"
                            )
                        }
                    }
                }
            }
        }
    }
}