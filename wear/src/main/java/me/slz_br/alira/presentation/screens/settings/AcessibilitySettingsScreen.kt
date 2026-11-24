package me.slz_br.alira.presentation.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.launch
import me.slz_br.alira.acessibilidade.FeedbackAcessivel
import me.slz_br.alira.data.repository.AliraRepository
import me.slz_br.alira.data.repository.RecursosAcessibilidade
import me.slz_br.alira.presentation.componentes.FundoAlira
import me.slz_br.alira.presentation.componentes.TituloTela
import me.slz_br.alira.presentation.theme.AliraTheme

/**
 * Cada linha da lista abaixo é uma linha da tabela "acessibilidade"
 * (classe Acessibilidade do diagrama), uma por recurso.
 *
 * Ao tocar numa chave, o valor é salvo no banco (personalizar). O
 * GerenciadorAcessibilidade percebe a mudança e o app inteiro passa a usar
 * a nova configuração na hora (cores, tamanho da letra, vibração, voz).
 */
@Composable
fun AcessibilitySettingsScreen() {
    val ctx = LocalContext.current
    val repository = remember { AliraRepository.getInstance(ctx) }
    val scope = rememberCoroutineScope()

    // "remember": cria a consulta uma vez só (e não a cada redesenho da tela).
    val recursos by remember { repository.observarRecursosDoUsuarioAtivo() }
        .collectAsState(initial = emptyList())

    AliraTheme {
        AppScaffold {
            val listState = rememberTransformingLazyColumnState()
            ScreenScaffold(
                scrollState = listState,
            ) { contentPadding ->
                FundoAlira {
                    TransformingLazyColumn(contentPadding = contentPadding, state = listState) {
                        item { TituloTela("Acessibilidade") }
                        items(recursos) { recurso ->
                            val nome = recurso.nomeAcessibilidade
                            SwitchButton(
                                checked = recurso.ativo,
                                onCheckedChange = { ativo ->
                                    scope.launch {
                                        repository.personalizar(recurso.usuarioId, nome, ativo)
                                    }
                                    // Confirma a mudança na hora. Quando a própria opção
                                    // está sendo LIGADA, usa "mesmoDesligado", porque o
                                    // banco ainda não terminou de salvar.
                                    val estado = if (ativo) "ligado" else "desligado"
                                    FeedbackAcessivel.vibrarToque(
                                        ctx,
                                        mesmoDesligado = ativo && nome == RecursosAcessibilidade.SINAIS_VIBRACAO,
                                    )
                                    FeedbackAcessivel.falar(
                                        ctx,
                                        "$nome $estado",
                                        mesmoDesligado = ativo && nome == RecursosAcessibilidade.TEXTO_PARA_VOZ,
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                secondaryLabel = {
                                    Text(text = recurso.descricao ?: RecursosAcessibilidade.descricao(nome))
                                },
                            ) {
                                Text(text = nome)
                            }
                        }
                    }
                }
            }
        }
    }
}
