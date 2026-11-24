package me.slz_br.alira.presentation.screens.monitoramento

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import java.util.Locale
import me.slz_br.alira.R
import me.slz_br.alira.acessibilidade.FeedbackAcessivel
import me.slz_br.alira.monitoramento.LeituraAoVivo
import me.slz_br.alira.monitoramento.MonitoramentoAoVivo
import me.slz_br.alira.monitoramento.SituacaoQueda
import me.slz_br.alira.presentation.componentes.AnelProgresso
import me.slz_br.alira.presentation.componentes.ChipInfo
import me.slz_br.alira.presentation.componentes.FundoAlira
import me.slz_br.alira.presentation.componentes.GraficoOnda
import me.slz_br.alira.presentation.componentes.TituloTela
import me.slz_br.alira.presentation.theme.AliraTheme
import me.slz_br.alira.presentation.theme.CoresAlira

/**
 * Painel de monitoramento AO VIVO.
 *
 * Mostra, enquanto o monitoramento contínuo está ligado:
 *  - um anel com o que o detector de queda está fazendo agora
 *    (Vigiando → Impacto! → Conferindo… → QUEDA!);
 *  - o gráfico da força do movimento nos últimos ~4 segundos, com uma linha
 *    no limite de impacto (2,5 g normal / 1,8 g no modo demonstração);
 *  - se a pessoa está parada ou em movimento e o último batimento.
 *
 * Embaixo ficam as chaves "Monitoramento contínuo" e "Modo demonstração".
 * Na apresentação: ligue as duas, caia no colchão e fique parado ~3 s; o
 * anel vai de "Impacto!" para "Conferindo…" e abre a pergunta "Você está bem?".
 */
@Composable
fun MonitoramentoScreen() {
    val controle = rememberControleMonitoramento()
    val leitura by MonitoramentoAoVivo.leitura.collectAsState()

    AliraTheme {
        AppScaffold {
            val listState = rememberTransformingLazyColumnState()
            ScreenScaffold(scrollState = listState) { contentPadding ->
                FundoAlira(corDoBrilho = corDaSituacao(leitura, controle.ligado)) {
                    TransformingLazyColumn(contentPadding = contentPadding, state = listState) {
                        item { TituloTela("Monitoramento") }
                        item {
                            AnelDaQueda(
                                leitura = leitura,
                                ligado = controle.ligado,
                                onTocarDesligado = { controle.alternar(true) },
                            )
                        }
                        if (controle.ligado && leitura.ativo) {
                            item { CartaoGrafico(leitura) }
                            item { LinhaDeChips(leitura) }
                        }
                        item {
                            SwitchButton(
                                checked = controle.ligado,
                                onCheckedChange = controle.alternar,
                                modifier = Modifier.fillMaxWidth(),
                                secondaryLabel = { Text("Queda, ansiedade e batimento") },
                            ) {
                                Text("Monitoramento contínuo")
                            }
                        }
                        item {
                            SwitchButton(
                                checked = controle.demonstracao,
                                onCheckedChange = controle.alternarDemonstracao,
                                modifier = Modifier.fillMaxWidth(),
                                secondaryLabel = { Text("Queda mais sensível (colchão)") },
                            ) {
                                Text("Modo demonstração")
                            }
                        }
                        controle.aviso?.let { texto ->
                            item {
                                Text(
                                    text = texto,
                                    color = CoresAlira.Alerta,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        item {
                            Text(
                                text = "Para demonstrar: ligue as duas chaves, caia no colchão e " +
                                    "fique parado uns 3 segundos.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Cor do brilho de fundo: roxo vigiando, magenta conferindo, vermelho na queda. */
private fun corDaSituacao(leitura: LeituraAoVivo, ligado: Boolean): Color = when {
    !ligado || !leitura.ativo -> CoresAlira.VioletaEscuro
    leitura.situacao == SituacaoQueda.QUEDA_DETECTADA -> CoresAlira.Alerta
    leitura.situacao == SituacaoQueda.VIGIANDO -> CoresAlira.Violeta
    else -> CoresAlira.Magenta
}

/** O anel grande do meio, com o estado do detector de queda. */
@Composable
private fun AnelDaQueda(leitura: LeituraAoVivo, ligado: Boolean, onTocarDesligado: () -> Unit) {
    val ctx = LocalContext.current
    val ativo = ligado && leitura.ativo
    val (titulo, detalhe) = when {
        !ligado -> "Desligado" to "toque para ligar"
        !leitura.ativo -> "Iniciando…" to "ligando os sensores"
        leitura.situacao == SituacaoQueda.QUEDA_DETECTADA -> "QUEDA!" to "abrindo a pergunta"
        leitura.situacao == SituacaoQueda.IMPACTO -> "Impacto!" to "assentando…"
        leitura.situacao == SituacaoQueda.CONFERINDO -> "Conferindo…" to "fique parado"
        else -> "Vigiando" to String.format(Locale("pt", "BR"), "%.1f g", leitura.forcaG)
    }
    val progresso = when {
        !ativo -> 0f
        leitura.situacao == SituacaoQueda.VIGIANDO -> 1f
        else -> leitura.progresso
    }
    val (corInicio, corFim) = when {
        leitura.situacao == SituacaoQueda.QUEDA_DETECTADA -> CoresAlira.Rosa to CoresAlira.Alerta
        leitura.situacao == SituacaoQueda.VIGIANDO -> CoresAlira.Violeta to CoresAlira.VioletaClaro
        else -> CoresAlira.Magenta to CoresAlira.Rosa
    }
    val textoFalado = "Detector de queda: $titulo, $detalhe"

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AnelProgresso(
            progresso = progresso,
            corInicio = corInicio,
            corFim = corFim,
            espessura = 9.dp,
            modifier = Modifier
                .size(128.dp)
                .clip(CircleShape)
                .clickable(onClickLabel = if (ligado) "Ouvir o estado" else "Ligar o monitoramento") {
                    if (ligado) FeedbackAcessivel.aoTocar(ctx, textoFalado) else onTocarDesligado()
                }
                .semantics { contentDescription = textoFalado },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(R.drawable.monitor_icon),
                    contentDescription = null,
                    tint = if (ativo) corFim else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp),
                )
                Text(
                    text = titulo,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = detalhe,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Cartão com o gráfico da força do movimento. */
@Composable
private fun CartaoGrafico(leitura: LeituraAoVivo) {
    val maiorValor = maxOf(3f, leitura.historicoForca.maxOrNull() ?: 0f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = "Força do movimento",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        GraficoOnda(
            valores = leitura.historicoForca,
            minimo = 0f,
            maximo = maiorValor,
            linhaLimite = leitura.limiteImpactoG,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(vertical = 4.dp),
        )
        Text(
            text = String.format(
                Locale("pt", "BR"),
                "linha = impacto (%.1f g)%s",
                leitura.limiteImpactoG,
                if (leitura.demonstracao) " · demonstração" else "",
            ),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** As duas etiquetas: parado / em movimento e o batimento. */
@Composable
private fun LinhaDeChips(leitura: LeituraAoVivo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        ChipInfo(
            texto = if (leitura.parado) "Parado" else "Em movimento",
            icone = R.drawable.steps_icon,
        )
        ChipInfo(
            texto = if (leitura.bpm > 0f) "${leitura.bpm.toInt()} bpm" else "-- bpm",
            icone = R.drawable.heart_icon,
            cor = CoresAlira.Rosa,
        )
    }
}
