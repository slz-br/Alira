package me.slz_br.alira.presentation.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import me.slz_br.alira.R
import me.slz_br.alira.acessibilidade.FeedbackAcessivel
import me.slz_br.alira.presentation.componentes.AnelProgresso
import me.slz_br.alira.presentation.componentes.FundoAlira
import me.slz_br.alira.presentation.componentes.GraficoOnda
import me.slz_br.alira.presentation.componentes.IndicadorPaginas
import me.slz_br.alira.presentation.theme.AliraTheme
import me.slz_br.alira.presentation.theme.CoresAlira
import me.slz_br.alira.presentation.theme.LocalAltoContraste
import me.slz_br.alira.presentation.viewmodels.EstadoSinaisVitais
import me.slz_br.alira.presentation.viewmodels.StatusMetrica
import me.slz_br.alira.presentation.viewmodels.VitalScreenViewModel

/** Meta diária de passos mostrada no anel (valor de referência comum: 10.000). */
private const val META_DE_PASSOS = 10_000

private const val NUMERO_DE_PAGINAS = 4

@Composable
fun VitalsScreen(viewModel: VitalScreenViewModel = viewModel()) {
    VitalsScreenConteudo(estado = viewModel.estado)
}

/**
 * Sinais Vitais em 4 páginas: deslize para o lado para trocar.
 * 1) batimento com gráfico ao vivo; 2) passos com o anel da meta diária;
 * 3) calorias; 4) distância. As bolinhas embaixo mostram a página atual.
 * Tocar numa página faz o relógio falar o valor (com "Texto para voz").
 */
@Composable
private fun VitalsScreenConteudo(estado: EstadoSinaisVitais) {
    AliraTheme {
        AppScaffold {
            val pagerState = rememberPagerState(pageCount = { NUMERO_DE_PAGINAS })
            FundoAlira(corDoBrilho = corDaPagina(pagerState.currentPage)) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pagina ->
                    when (pagina) {
                        0 -> PaginaBatimento(estado)
                        1 -> PaginaPassos(estado)
                        2 -> PaginaNumero(
                            icone = R.drawable.calories_icon,
                            cor = CoresAlira.Magenta,
                            rotulo = "Calorias (hoje)",
                            valor = estado.calorias?.let { formatar(it, 0) },
                            unidade = "kcal",
                            falado = estado.calorias?.let { "${formatar(it, 0)} quilocalorias" },
                            status = estado.statusDiarias,
                        )
                        else -> PaginaNumero(
                            icone = R.drawable.distance_icon,
                            cor = CoresAlira.VioletaClaro,
                            rotulo = "Distância (hoje)",
                            valor = estado.distanciaMetros?.let { metros ->
                                if (metros >= 1000.0) formatar(metros / 1000.0, 2) else formatar(metros, 0)
                            },
                            unidade = if ((estado.distanciaMetros ?: 0.0) >= 1000.0) "km" else "m",
                            falado = estado.distanciaMetros?.let { metros ->
                                if (metros >= 1000.0) "${formatar(metros / 1000.0, 2)} quilômetros"
                                else "${formatar(metros, 0)} metros"
                            },
                            status = estado.statusDiarias,
                        )
                    }
                }
                IndicadorPaginas(
                    total = NUMERO_DE_PAGINAS,
                    atual = pagerState.currentPage,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp),
                )
            }
        }
    }
}

/** Cada página tem a sua cor de brilho no fundo. */
private fun corDaPagina(pagina: Int): Color = when (pagina) {
    0 -> CoresAlira.Rosa
    1 -> CoresAlira.Violeta
    2 -> CoresAlira.Magenta
    else -> CoresAlira.VioletaEscuro
}

/** Moldura comum das páginas: centraliza o conteúdo e fala [textoFalado] ao tocar. */
@Composable
private fun Pagina(textoFalado: String, content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClickLabel = "Ouvir o valor") { FeedbackAcessivel.aoTocar(ctx, textoFalado) }
            .semantics { contentDescription = textoFalado },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Página 1: coração + número grande + gráfico ao vivo dos últimos batimentos. */
@Composable
private fun PaginaBatimento(estado: EstadoSinaisVitais) {
    val ok = estado.statusBpm == StatusMetrica.OK && estado.bpm != null
    val numero = if (ok) formatar(estado.bpm ?: 0.0, 0) else "--"
    val falado = if (ok) "Frequência cardíaca: $numero batimentos por minuto"
    else "Frequência cardíaca: ${textoDoStatus(estado.statusBpm)}"
    val altoContraste = LocalAltoContraste.current

    Pagina(textoFalado = falado) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.heart_icon),
                    contentDescription = null,
                    tint = if (altoContraste) MaterialTheme.colorScheme.primary else CoresAlira.Rosa,
                    modifier = Modifier.size(26.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = numero,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "BPM",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 14.dp),
                )
            }
            GraficoOnda(
                valores = estado.historicoBpm,
                cor = CoresAlira.Rosa,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp)
                    .padding(vertical = 6.dp),
            )
            Text(
                text = if (ok) "Frequência cardíaca" else textoDoStatus(estado.statusBpm),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Página 2: passos do dia dentro de um anel que enche até a meta de 10.000. */
@Composable
private fun PaginaPassos(estado: EstadoSinaisVitais) {
    val ok = estado.statusDiarias == StatusMetrica.OK && estado.passos != null
    val passos = estado.passos ?: 0L
    val falado = if (ok) "Passos hoje: $passos de $META_DE_PASSOS"
    else "Passos: ${textoDoStatus(estado.statusDiarias)}"
    val altoContraste = LocalAltoContraste.current

    Pagina(textoFalado = falado) {
        AnelProgresso(
            progresso = if (ok) passos.toFloat() / META_DE_PASSOS else 0f,
            modifier = Modifier.size(150.dp),
            espessura = 11.dp,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(R.drawable.steps_icon),
                    contentDescription = null,
                    tint = if (altoContraste) MaterialTheme.colorScheme.primary else CoresAlira.VioletaClaro,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = if (ok) "$passos" else "--",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (ok) "de 10.000 passos" else textoDoStatus(estado.statusDiarias),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Páginas 3 e 4: ícone num círculo brilhante + número grande + unidade. */
@Composable
private fun PaginaNumero(
    @DrawableRes icone: Int,
    cor: Color,
    rotulo: String,
    valor: String?,
    unidade: String,
    falado: String?,
    status: StatusMetrica,
) {
    val ok = status == StatusMetrica.OK && valor != null
    val altoContraste = LocalAltoContraste.current
    val textoFalado = "$rotulo: ${if (ok) falado else textoDoStatus(status)}"
    val fundoDoIcone = if (altoContraste) {
        SolidColor(MaterialTheme.colorScheme.primary)
    } else {
        Brush.linearGradient(listOf(cor, CoresAlira.VioletaEscuro))
    }

    Pagina(textoFalado = textoFalado) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(fundoDoIcone),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(icone),
                    contentDescription = null,
                    tint = if (altoContraste) MaterialTheme.colorScheme.onPrimary else Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = rotulo,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (ok) valor ?: "--" else "--",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = unidade,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
            }
            if (!ok) {
                Text(
                    text = textoDoStatus(status),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
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
            historicoBpm = listOf(72f, 74f, 77f, 80f, 79f, 76f, 78f, 81f, 78f),
        )
    )
}
