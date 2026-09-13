package me.slz_br.alira.presentation.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.launch
import me.slz_br.alira.R
import me.slz_br.alira.data.repository.AliraRepository
import me.slz_br.alira.presentation.theme.AliraTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun GenerateReportScreen() {
    val ctx = LocalContext.current
    val repository = remember { AliraRepository.getInstance(ctx) }
    val scope = rememberCoroutineScope()
    var usuarioId by remember { mutableLongStateOf(0L) }
    var mostrarHistorico by remember { mutableStateOf(false) }
    var mensagem by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        usuarioId = repository.garantirUsuarioLocal()
    }

    val historico by if (usuarioId != 0L) {
        repository.observarHistoricoMonitoramento(usuarioId, limite = 10).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    AliraTheme {
        AppScaffold {
            val listState = rememberTransformingLazyColumnState()
            ScreenScaffold(
                scrollState = listState,
            ) {
                TransformingLazyColumn {
                    item {
                        Text(
                            modifier = Modifier.padding(0.dp, 10.dp),
                            fontWeight = FontWeight(800),
                            text = "Relatórios"
                        )
                    }
                    item {
                        // Relatorio.gerarPDF() do diagrama de classes.
                        Button(
                            onClick = {
                                mostrarHistorico = false
                                scope.launch {
                                    val fim = System.currentTimeMillis()
                                    val inicio = fim - TimeUnit.DAYS.toMillis(1)
                                    val relatorio = repository.gerarPDF(usuarioId, inicio, fim)
                                    mensagem = "PDF gerado: ${relatorio.caminhoArquivo}"
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
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
                        Button(
                            onClick = { mostrarHistorico = !mostrarHistorico },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                textAlign = TextAlign.Center,
                                text = "Ver Relatório"
                            )
                        }
                    }
                    mensagem?.let { texto ->
                        item { Text(texto) }
                    }
                    if (mostrarHistorico) {
                        if (historico.isEmpty()) {
                            item { Text("Sem registros salvos ainda") }
                        } else {
                            items(historico) { registro ->
                                val formato = remember { SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")) }
                                val bpm = registro.frequenciaCardiaca?.toInt()?.toString() ?: "-"
                                Text("${formato.format(Date(registro.dataHora))} · $bpm BPM")
                            }
                        }
                    }
                }
            }
        }
    }
}
