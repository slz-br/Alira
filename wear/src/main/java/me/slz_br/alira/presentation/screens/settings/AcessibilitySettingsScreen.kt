package me.slz_br.alira.presentation.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.launch
import me.slz_br.alira.data.repository.AliraRepository

/**
 * Cada linha da lista abaixo é uma linha da tabela "acessibilidade"
 * (classe Acessibilidade do diagrama), uma por recurso — em vez do
 * esquema anterior (3 colunas fixas numa única linha de configuração).
 */
@Composable
fun AcessibilitySettingsScreen() {
    val ctx = LocalContext.current
    val repository = remember { AliraRepository.getInstance(ctx) }
    val scope = rememberCoroutineScope()
    var usuarioId by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        usuarioId = repository.garantirUsuarioLocal()
    }

    val recursos by repository.observarRecursosAcessibilidade(usuarioId)
        .collectAsState(initial = emptyList())

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
                items(recursos) { recurso ->
                    SwitchButton(
                        checked = recurso.ativo,
                        onCheckedChange = { ativo ->
                            scope.launch {
                                repository.personalizar(usuarioId, recurso.nomeAcessibilidade, ativo)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = recurso.nomeAcessibilidade)
                    }
                }
            }
        }
    }
}
