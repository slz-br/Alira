package me.slz_br.alira.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.launch
import me.slz_br.alira.R
import me.slz_br.alira.acessibilidade.FeedbackAcessivel
import me.slz_br.alira.acessibilidade.acaoAcessivel
import me.slz_br.alira.data.repository.AliraRepository
import me.slz_br.alira.presentation.componentes.CartaoMenu
import me.slz_br.alira.presentation.componentes.FundoAlira
import me.slz_br.alira.presentation.componentes.TituloTela
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

    // Últimos registros do usuário ativo (batimentos e atividade do dia).
    // "remember": cria a consulta uma vez só (e não a cada redesenho da tela).
    val historico by remember { repository.observarHistoricoDoUsuarioAtivo(limite = 10) }
        .collectAsState(initial = emptyList())

    AliraTheme {
        AppScaffold {
            val listState = rememberTransformingLazyColumnState()
            ScreenScaffold(
                scrollState = listState,
            ) { contentPadding ->
                FundoAlira {
                    TransformingLazyColumn(contentPadding = contentPadding, state = listState) {
                        item { TituloTela("Relatórios") }
                        item {
                            // Relatorio.gerarPDF() do diagrama de classes.
                            CartaoMenu(
                                icone = R.drawable.graph_icon,
                                titulo = "Gerar relatório",
                                subtitulo = "PDF de 24 h + envio ao celular",
                                onClick = acaoAcessivel("Gerar relatório") {
                                    mostrarHistorico = false
                                    scope.launch {
                                        val fim = System.currentTimeMillis()
                                        val inicio = fim - TimeUnit.DAYS.toMillis(1)
                                        val relatorio = repository.gerarPDF(usuarioId, inicio, fim)
                                        val nomeArquivo = relatorio.caminhoArquivo?.substringAfterLast('/') ?: "PDF"
                                        mensagem = "Gerando… enviando leituras ao celular"
                                        // Manda ao celular as leituras novas, para os
                                        // gráficos do app do celular (enviarDados).
                                        val envio = repository.enviarDados(usuarioId)
                                        val textoEnvio = when {
                                            !envio.celularConectado ->
                                                "Celular não conectado: leituras não enviadas"
                                            envio.enviadas == 0 -> "Nenhuma leitura nova para o celular"
                                            else -> "${envio.enviadas} leituras enviadas ao celular"
                                        }
                                        mensagem = "Relatório PDF salvo no relógio ($nomeArquivo)\n$textoEnvio"
                                        FeedbackAcessivel.falar(ctx, "Relatório gerado. $textoEnvio")
                                    }
                                },
                            )
                        }
                        item {
                            CartaoMenu(
                                icone = R.drawable.chart_icon,
                                titulo = if (mostrarHistorico) "Esconder histórico" else "Ver histórico",
                                subtitulo = "Últimos 10 registros",
                                onClick = acaoAcessivel("Ver histórico") { mostrarHistorico = !mostrarHistorico },
                            )
                        }
                        mensagem?.let { texto ->
                            item {
                                Text(
                                    text = texto,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        if (mostrarHistorico) {
                            if (historico.isEmpty()) {
                                item {
                                    Text(
                                        text = "Sem registros salvos ainda",
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            } else {
                                items(historico) { registro ->
                                    val formato = remember { SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")) }
                                    val quando = formato.format(Date(registro.dataHora))
                                    val oQue = AliraRepository.descreverRegistro(registro)
                                    // Cada registro é um cartãozinho; tocar faz o relógio
                                    // ler o registro (com "Texto para voz" ligado).
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainer)
                                            .clickable { FeedbackAcessivel.aoTocar(ctx, "$quando, $oQue") }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                    ) {
                                        Text(
                                            text = oQue,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = quando,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
