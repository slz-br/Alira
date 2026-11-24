package me.slz_br.alira.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import me.slz_br.alira.acessibilidade.GerenciadorAcessibilidade

/** Quanto as letras crescem com "Fonte de alto contraste" ligada (30% maiores). */
private const val AUMENTO_DA_FONTE = 1.3f

/**
 * A paleta do ALIRA: roxo/violeta com um toque de magenta, sobre fundo
 * preto (fundo preto economiza bateria nas telas OLED dos relógios).
 * As telas e os componentes (ComponentesAlira.kt) usam estas cores.
 */
object CoresAlira {
    val Violeta = Color(0xFF8B5CF6)
    val VioletaClaro = Color(0xFFC4B5FD)
    val VioletaEscuro = Color(0xFF4C1D95)
    val Magenta = Color(0xFFE879F9)
    val Rosa = Color(0xFFF472B6)
    val Fundo = Color(0xFF000000)
    val Cartao = Color(0xFF1B1526)
    val CartaoClaro = Color(0xFF2A2140)
    val Texto = Color(0xFFFFFFFF)
    val TextoSuave = Color(0xFFBDB4D3)
    val Alerta = Color(0xFFFF5470)
}

/** Cores normais (roxo): usadas por todos os componentes do Wear OS (botões, chaves...). */
private val CoresNormais = ColorScheme(
    primary = CoresAlira.VioletaClaro,
    onPrimary = Color(0xFF1E1035),
    primaryContainer = Color(0xFF5B2DB3),
    onPrimaryContainer = CoresAlira.Texto,
    secondary = CoresAlira.Magenta,
    onSecondary = Color(0xFF2A0A33),
    secondaryContainer = CoresAlira.CartaoClaro,
    onSecondaryContainer = CoresAlira.Texto,
    surfaceContainerLow = Color(0xFF130F1B),
    surfaceContainer = CoresAlira.Cartao,
    surfaceContainerHigh = CoresAlira.CartaoClaro,
    onSurface = CoresAlira.Texto,
    onSurfaceVariant = CoresAlira.TextoSuave,
    outline = Color(0xFF4B3F66),
    background = CoresAlira.Fundo,
    onBackground = CoresAlira.Texto,
)

private val Amarelo = Color(0xFFFFD600)
private val Preto = Color(0xFF000000)
private val Branco = Color(0xFFFFFFFF)
private val CinzaEscuro = Color(0xFF262626)

/**
 * Cores do modo alto contraste: fundo preto, botões amarelos com letra
 * preta, textos brancos. Amarelo sobre preto e preto sobre amarelo têm
 * contraste bem acima do mínimo recomendado para baixa visão (WCAG 7:1).
 */
private val CoresAltoContraste = ColorScheme(
    primary = Amarelo,
    onPrimary = Preto,
    primaryContainer = Amarelo,
    onPrimaryContainer = Preto,
    secondary = Branco,
    onSecondary = Preto,
    secondaryContainer = Branco,
    onSecondaryContainer = Preto,
    surfaceContainerLow = CinzaEscuro,
    surfaceContainer = CinzaEscuro,
    surfaceContainerHigh = CinzaEscuro,
    onSurface = Branco,
    onSurfaceVariant = Branco,
    outline = Branco,
    background = Preto,
    onBackground = Branco,
)

/** Marca que o tema já foi aplicado mais acima (evita aumentar a letra duas vezes). */
private val LocalTemaAplicado = staticCompositionLocalOf { false }

/**
 * true quando o alto contraste está ligado. Os componentes do app olham
 * isto para trocar os degradês e brilhos por cores lisas e fortes.
 */
val LocalAltoContraste = staticCompositionLocalOf { false }

/**
 * Tema do app. Todas as telas ficam dentro dele.
 *
 * Normal: paleta roxa (CoresNormais).
 * "Fonte de alto contraste" ligada: cores amarelo/preto/branco e todas as
 * letras 30% maiores (os botões crescem junto com o texto).
 *
 * Quando a pessoa liga ou desliga a opção, a tela se redesenha sozinha,
 * porque o tema observa o StateFlow da configuração (collectAsState).
 */
@Composable
fun AliraTheme(
    content: @Composable () -> Unit
) {
    if (LocalTemaAplicado.current) {
        content()
        return
    }

    val context = LocalContext.current
    remember { GerenciadorAcessibilidade.iniciar(context) }
    val config by GerenciadorAcessibilidade.config.collectAsState()

    if (!config.altoContraste) {
        CompositionLocalProvider(
            LocalTemaAplicado provides true,
            LocalAltoContraste provides false,
        ) {
            MaterialTheme(colorScheme = CoresNormais, content = content)
        }
        return
    }

    // A "densidade" diz ao Compose quantos pixels vale cada unidade de
    // tamanho. Mudando só o fontScale, todas as medidas em "sp" (letras)
    // ficam 30% maiores, sem precisar mexer em cada Text do app.
    val densidadeAtual = LocalDensity.current
    val densidadeAumentada = Density(
        density = densidadeAtual.density,
        fontScale = densidadeAtual.fontScale * AUMENTO_DA_FONTE,
    )
    CompositionLocalProvider(
        LocalTemaAplicado provides true,
        LocalAltoContraste provides true,
        LocalDensity provides densidadeAumentada,
    ) {
        MaterialTheme(colorScheme = CoresAltoContraste, content = content)
    }
}
