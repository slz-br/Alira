package me.slz_br.alira.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import me.slz_br.alira.acessibilidade.FeedbackAcessivel
import me.slz_br.alira.data.repository.AliraRepository
import me.slz_br.alira.presentation.componentes.FundoAlira
import me.slz_br.alira.presentation.componentes.TituloTela
import me.slz_br.alira.presentation.theme.AliraTheme

/**
 * Mostra o cuidador do usuário (classe Cuidador do diagrama).
 *
 * É só para LER: os dados são digitados no Perfil do celular (teclado de
 * verdade) e chegam aqui sozinhos pela Data Layer. Como a tela "escuta" o
 * banco (Flow), se o celular mandar uma mudança com a tela aberta, ela
 * se atualiza na hora.
 */
@Composable
fun CuidadorScreen() {
    val ctx = LocalContext.current
    val repository = remember { AliraRepository.getInstance(ctx) }

    // "remember": cria a consulta uma vez só (e não a cada redesenho da tela).
    // initial = null: até o banco responder, mostra "nenhum cuidador".
    val cuidador by remember { repository.observarCuidadorDoUsuarioAtivo() }
        .collectAsState(initial = null)

    AliraTheme {
        AppScaffold {
            val listState = rememberTransformingLazyColumnState()
            ScreenScaffold(scrollState = listState) { contentPadding ->
                FundoAlira {
                    TransformingLazyColumn(contentPadding = contentPadding, state = listState) {
                        item { TituloTela("Cuidador") }

                        val atual = cuidador
                        if (atual == null) {
                            item {
                                Text(
                                    text = "Nenhum cuidador cadastrado.\nPreencha no Perfil do app do celular.",
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        } else {
                            // Cada dado vira um cartãozinho "rótulo + valor".
                            // Campos que o celular mandou vazios não aparecem.
                            val linhas = listOfNotNull(
                                "Nome" to atual.nome,
                                atual.parentesco?.let { "Parentesco" to it },
                                "Telefone" to atual.telefone,
                                atual.email?.let { "E-mail" to it },
                            ).filter { it.second.isNotBlank() }

                            items(linhas) { (rotulo, valor) ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainer)
                                        // Com "Texto para voz" ligado, tocar lê o dado.
                                        .clickable { FeedbackAcessivel.aoTocar(ctx, "$rotulo: $valor") }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = rotulo,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = valor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                            item {
                                Text(
                                    text = "Para mudar, edite no Perfil do celular.",
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
