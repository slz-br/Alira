package me.slz_br.alira.presentation.componentes

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import me.slz_br.alira.presentation.theme.CoresAlira
import me.slz_br.alira.presentation.theme.LocalAltoContraste

/*
 * As "peças de Lego" do visual do ALIRA. Todas as telas montam o desenho
 * com estas funções, então o estilo fica igual no app inteiro e, para
 * mudar o visual, basta mexer aqui.
 *
 * Regra de acessibilidade: com o alto contraste ligado (LocalAltoContraste),
 * cada peça troca os degradês e brilhos por cores lisas e fortes do tema.
 */

/**
 * Fundo das telas: preto com um brilho roxo vindo de cima e um brilho
 * magenta no canto de baixo (o "glow" do visual moderno).
 */
@Composable
fun FundoAlira(
    modifier: Modifier = Modifier,
    corDoBrilho: Color = CoresAlira.Violeta,
    content: @Composable BoxScope.() -> Unit,
) {
    val altoContraste = LocalAltoContraste.current
    val brilho = if (altoContraste) Modifier else Modifier.drawBehind {
        drawRect(
            Brush.radialGradient(
                colors = listOf(corDoBrilho.copy(alpha = 0.45f), Color.Transparent),
                center = Offset(size.width / 2f, 0f),
                radius = size.width * 0.9f,
            )
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(CoresAlira.Magenta.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(size.width, size.height),
                radius = size.width * 0.7f,
            )
        )
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(brilho),
        content = content,
    )
}

/** Título curto no topo de uma tela (cor clara do tema). */
@Composable
fun TituloTela(texto: String, modifier: Modifier = Modifier) {
    val altoContraste = LocalAltoContraste.current
    Text(
        text = texto,
        color = if (altoContraste) MaterialTheme.colorScheme.primary else CoresAlira.VioletaClaro,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    )
}

/**
 * Cartão do menu: uma "pílula" escura com um círculo colorido (ícone),
 * o título e uma linha de explicação. A pílula inteira é o botão.
 */
@Composable
fun CartaoMenu(
    @DrawableRes icone: Int,
    titulo: String,
    subtitulo: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    corIcone: Color = CoresAlira.Violeta,
) {
    val altoContraste = LocalAltoContraste.current
    val cores = MaterialTheme.colorScheme
    val fundoDoIcone = if (altoContraste) {
        SolidColor(cores.primary)
    } else {
        Brush.linearGradient(listOf(corIcone, CoresAlira.Magenta))
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(cores.surfaceContainer)
            .clickable(role = Role.Button, onClickLabel = titulo, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(fundoDoIcone),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icone),
                contentDescription = null,
                tint = if (altoContraste) cores.onPrimary else Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                color = cores.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
            if (subtitulo != null) {
                Text(
                    text = subtitulo,
                    color = cores.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 2,
                )
            }
        }
    }
}

/**
 * Anel de progresso (270°, aberto embaixo), com degradê roxo → magenta.
 * [progresso] vai de 0 a 1; a mudança é animada. O [content] fica no meio.
 */
@Composable
fun AnelProgresso(
    progresso: Float,
    modifier: Modifier = Modifier,
    espessura: Dp = 10.dp,
    corInicio: Color = CoresAlira.Violeta,
    corFim: Color = CoresAlira.Magenta,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val altoContraste = LocalAltoContraste.current
    val corTrilho = MaterialTheme.colorScheme.surfaceContainerHigh
    val corSolida = MaterialTheme.colorScheme.primary
    val animado by animateFloatAsState(
        targetValue = progresso.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "anel",
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val traco = espessura.toPx()
            val lado = size.minDimension - traco
            val canto = Offset((size.width - lado) / 2f, (size.height - lado) / 2f)
            val tamanho = Size(lado, lado)
            drawArc(
                color = corTrilho,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = canto,
                size = tamanho,
                style = Stroke(width = traco, cap = StrokeCap.Round),
            )
            if (animado > 0f) {
                val pincel = if (altoContraste) {
                    SolidColor(corSolida)
                } else {
                    Brush.linearGradient(
                        colors = listOf(corInicio, corFim),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, 0f),
                    )
                }
                drawArc(
                    brush = pincel,
                    startAngle = 135f,
                    sweepAngle = 270f * animado,
                    useCenter = false,
                    topLeft = canto,
                    size = tamanho,
                    style = Stroke(width = traco, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

/**
 * Gráfico de linha com a área de baixo em degradê (como uma "onda").
 * [valores] = os pontos, do mais antigo (esquerda) ao mais novo (direita).
 * [linhaLimite] (opcional) desenha uma linha fina horizontal naquele valor.
 */
@Composable
fun GraficoOnda(
    valores: List<Float>,
    modifier: Modifier = Modifier,
    minimo: Float? = null,
    maximo: Float? = null,
    cor: Color = CoresAlira.Magenta,
    linhaLimite: Float? = null,
) {
    val altoContraste = LocalAltoContraste.current
    val corLinha = if (altoContraste) MaterialTheme.colorScheme.primary else cor
    val corLimite = MaterialTheme.colorScheme.outline
    Canvas(modifier = modifier) {
        if (valores.size < 2) return@Canvas
        val menor = minimo ?: valores.min()
        val maiorBruto = maximo ?: valores.max()
        val maior = if (maiorBruto - menor < 0.001f) menor + 1f else maiorBruto
        fun y(v: Float): Float = size.height - ((v - menor) / (maior - menor)).coerceIn(0f, 1f) * size.height
        val passo = size.width / (valores.size - 1)

        val linha = Path()
        val area = Path()
        area.moveTo(0f, size.height)
        valores.forEachIndexed { i, v ->
            val x = i * passo
            if (i == 0) linha.moveTo(x, y(v)) else linha.lineTo(x, y(v))
            area.lineTo(x, y(v))
        }
        area.lineTo(size.width, size.height)
        area.close()

        if (!altoContraste) {
            drawPath(
                path = area,
                brush = Brush.verticalGradient(listOf(corLinha.copy(alpha = 0.45f), Color.Transparent)),
            )
        }
        if (linhaLimite != null) {
            val yLimite = y(linhaLimite)
            drawLine(
                color = corLimite,
                start = Offset(0f, yLimite),
                end = Offset(size.width, yLimite),
                strokeWidth = 1.dp.toPx(),
            )
        }
        drawPath(
            path = linha,
            color = corLinha,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawCircle(
            color = corLinha,
            radius = 4.dp.toPx(),
            center = Offset(size.width, y(valores.last())),
        )
    }
}

/** As bolinhas que mostram em qual página você está (a atual fica comprida). */
@Composable
fun IndicadorPaginas(total: Int, atual: Int, modifier: Modifier = Modifier) {
    val cores = MaterialTheme.colorScheme
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .size(width = if (i == atual) 14.dp else 6.dp, height = 6.dp)
                    .clip(CircleShape)
                    .background(if (i == atual) cores.primary else cores.outline)
            )
        }
    }
}

/** "Etiqueta" pequena e arredondada, com ícone opcional (ex.: "♥ 78 bpm"). */
@Composable
fun ChipInfo(
    texto: String,
    modifier: Modifier = Modifier,
    @DrawableRes icone: Int? = null,
    cor: Color = CoresAlira.VioletaClaro,
) {
    val altoContraste = LocalAltoContraste.current
    val cores = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(cores.surfaceContainer)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icone != null) {
            Icon(
                painter = painterResource(icone),
                contentDescription = null,
                tint = if (altoContraste) cores.primary else cor,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(text = texto, fontSize = 12.sp, color = cores.onSurface)
    }
}
