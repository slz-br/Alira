package me.slz_br.alira.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import me.slz_br.alira.presentation.theme.AliraTheme
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import me.slz_br.alira.R
import me.slz_br.alira.presentation.viewmodels.VitalScreenViewModel

@Composable
    fun VitalsScreen(viewModel: VitalScreenViewModel) {
        AliraTheme {
            val calories by viewModel.calories
            AppScaffold {
                val listState = rememberTransformingLazyColumnState()
                ScreenScaffold(
                    scrollState = listState,
                ) { contentPadding ->
                    TransformingLazyColumn(
                        verticalArrangement = Arrangement.Center,
                        contentPadding = contentPadding
                    ) {
                        item {
                            ListHeader(modifier =
                                Modifier
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight(800),
                                    text = "Sinais Vitais"
                                )
                            }
                        }
                        item {
                            Row {
                                Icon(
                                    painter = painterResource(R.drawable.heart_rate),
                                    contentDescription = "Vital signs icon"
                                )
                                Text("Calorias: $calories")
                            }
                        }
                    }
                }
            }
        }
    }
