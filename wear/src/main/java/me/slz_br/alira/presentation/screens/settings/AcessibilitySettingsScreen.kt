package me.slz_br.alira.presentation.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text

@Composable
fun AccessibilitySettingsScreen() {
    AppScaffold {
        val listState = rememberTransformingLazyColumnState()
        ScreenScaffold(
            scrollState = listState,
        ) { contentPadding ->
            TransformingLazyColumn(contentPadding = contentPadding) {
                item {
                    Text(
                        fontWeight = FontWeight(800),
                        text = "Acessibilidade"
                    )
                }
                item {
                    SwitchButton (
                        checked = false,
                        onCheckedChange = {},
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Fonte de alto contraste"
                        )
                    }
                }
                item {
                    SwitchButton (
                        checked = false,
                        onCheckedChange = {},
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Sinais de vibração"
                        )
                    }
                }
                item {
                    SwitchButton (
                        checked = false,
                        onCheckedChange = {},
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Texto para Voz"
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun AccessibilitySettingsScreenPreview() = AccessibilitySettingsScreen()