package me.slz_br.alira.presentation.screens.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import me.slz_br.alira.R
import me.slz_br.alira.presentation.theme.AliraTheme

@Composable
fun SettingsScreen(
    onAccessibilityClick: () -> Unit
) {
    AliraTheme {
        AppScaffold {
            val listState = rememberTransformingLazyColumnState()
            ScreenScaffold(
                scrollState = listState,
            ) { contentPadding ->
                TransformingLazyColumn(contentPadding = contentPadding) {
                    item {
                        Text(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight(800),
                            text = "Configurações"
                        )
                    }
                    item {
                        FilledIconButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onAccessibilityClick) {
                            Row {
                                Icon(
                                    painter = painterResource(R.drawable.accessibility_icon),
                                    contentDescription = "Configurações de acessibilidade"
                                )
                                Text(
                                    textAlign = TextAlign.Center,
                                    text = "Acessibilidade"
                                )
                            }
                        }
                    }
                    item {
                        FilledIconButton(
                            modifier = Modifier
                                .fillMaxWidth(),
                            onClick = onAccessibilityClick
                        ) {
                            Row {
                                Icon(
                                    painter = painterResource(R.drawable.privacy_icon),
                                    contentDescription = "Configurações de privacidade"
                                )
                                Text("Privacidade")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() = SettingsScreen {}