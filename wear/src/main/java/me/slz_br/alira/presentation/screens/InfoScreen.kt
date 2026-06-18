package me.slz_br.alira.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import me.slz_br.alira.presentation.theme.AliraTheme

@Composable
fun InfoScreen() {
    AliraTheme {
        AppScaffold {
            val listState = rememberTransformingLazyColumnState()
            ScreenScaffold(
                scrollState = listState,
            ) { contentPadding ->
                TransformingLazyColumn(
                    contentPadding = contentPadding
                ) {
                    item {
                        Text(
                            fontWeight = FontWeight(800),
                            text = "Créditos"
                        )
                    }
                    item {
                        Column() {

                            Text(
                                textDecoration = TextDecoration.Underline,
                                text = "Felipe G.",
                            )
                            Text(
                                text = "Iasmin"
                            )
                            Text(
                                text = "Raphael M."
                            )
                            Text(
                                text = "Yasmin P."
                            )
                        }

                    }
                }
            }
        }
    }
}