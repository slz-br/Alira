package me.slz_br.alira.presentation.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import me.slz_br.alira.R
import me.slz_br.alira.presentation.theme.AliraTheme
import me.slz_br.alira.presentation.viewmodels.EstadoSinaisVitais
import me.slz_br.alira.presentation.viewmodels.StatusMetrica
import me.slz_br.alira.presentation.viewmodels.VitalScreenViewModel

@Composable
fun VitalsScreen(viewModel: VitalScreenViewModel = viewModel()) {
    VitalsScreenConteudo(estado = viewModel.estado)
}

@Composable
private fun VitalsScreenConteudo(estado: EstadoSinaisVitais) {
    AliraTheme {
        AppScaffold {
            val listState = rememberTransformingLazyColumnState()
            ScreenScaffold(scrollState = listState) { contentPadding ->
                TransformingLazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.Center,
                    contentPadding = contentPadding,
                ) {
                    item {
                        ListHeader(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight(800),
                                text = "Sinais Vitais",
                            )
                        }
                    }
                    item {
                        LinhaMetrica(
                            icone = R.drawable.heart_rate,
                            descricaoIcone = "Frequência cardíaca",
                            rotulo = "Freq. cardíaca",
                            valor = estado.bpm?.let { "${formatar(it, 0)} bpm" },
                            status = estado.statusBpm,
                        )
                    }
                    item {
                        LinhaMetrica(
                            icone = R.drawable.calories_icon,
                            descricaoIcone = "Calorias",
                            rotulo = "Calorias (hoje)",
                            valor = estado.calorias?.let { "${formatar(it, 0)} kcal" },
                            status = estado.statusDiarias,
                        )
                    }
                    item {
                        LinhaMetrica(
                            icone = R.drawable.steps_icon,
                            descricaoIcone = "Passos",
                            rotulo = "Passos (hoje)",
                            valor = estado.passos?.let { "$it" },
                            status = estado.statusDiarias,
                        )
                    }
                    item {
                        LinhaMetrica(
                            icone = R.drawable.distance_icon,
                            descricaoIcone = "Distância",
                            rotulo = "Distância (hoje)",
                            valor = estado.distanciaMetros?.let { metros ->
                                if (metros >= 1000.0) "${formatar(metros / 1000.0, 2)} km"
                                else "${formatar(metros, 0)} m"
                            },
                            status = estado.statusDiarias,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LinhaMetrica(
    @DrawableRes icone: Int,
    descricaoIcone: String,
    rotulo: String,
    valor: String?,
    status: StatusMetrica,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icone),
            contentDescription = descricaoIcone,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = rotulo, fontSize = 12.sp)
            Text(
                text = valor.takeIf { status == StatusMetrica.OK } ?: textoDoStatus(status),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
    }
}

private fun textoDoStatus(status: StatusMetrica): String = when (status) {
    StatusMetrica.AGUARDANDO -> "medindo…"
    StatusMetrica.OK -> "medindo…"
    StatusMetrica.ADQUIRINDO -> "estabilizando…"
    StatusMetrica.INDISPONIVEL -> "indisponível agora"
    StatusMetrica.NAO_SUPORTADO -> "sem suporte neste relógio"
    StatusMetrica.SEM_PERMISSAO -> "permissão negada"
    StatusMetrica.ERRO -> "erro ao medir"
}

private fun formatar(valor: Double, casas: Int): String = when (casas) {
    0 -> "%.0f".format(valor)
    1 -> "%.1f".format(valor)
    else -> "%.2f".format(valor)
}

@Preview
@Composable
private fun VitalsScreenPreview() {
    VitalsScreenConteudo(
        estado = EstadoSinaisVitais(
            bpm = 78.0,
            statusBpm = StatusMetrica.OK,
            calorias = 412.0,
            passos = 3820L,
            distanciaMetros = 2730.0,
            statusDiarias = StatusMetrica.OK,
        )
    )
}
