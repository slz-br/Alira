package me.slz_br.alira.presentation.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import me.slz_br.alira.presentation.theme.AliraTheme

@Composable
fun SettingsScreen(
    onAcessibilityClick: () -> Unit
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
                            fontWeight = FontWeight(800),
                            text = "Configurações"
                        )
                    }
                    item {
                        Button(onAcessibilityClick) {
                            Text("Acessibilidade")
                        }
                    }
                }
            }
        }
    }
}