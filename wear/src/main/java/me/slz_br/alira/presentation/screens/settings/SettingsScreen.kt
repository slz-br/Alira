package me.slz_br.alira.presentation.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ScreenScaffold
import me.slz_br.alira.R
import me.slz_br.alira.acessibilidade.acaoAcessivel
import me.slz_br.alira.data.repository.AliraRepository
import me.slz_br.alira.emergencia.CentralDeEmergencia
import me.slz_br.alira.emergencia.TipoEmergencia
import me.slz_br.alira.monitoramento.PreferenciasMonitoramento
import me.slz_br.alira.presentation.componentes.CartaoMenu
import me.slz_br.alira.presentation.componentes.FundoAlira
import me.slz_br.alira.presentation.componentes.TituloTela
import me.slz_br.alira.presentation.theme.AliraTheme
import me.slz_br.alira.presentation.theme.CoresAlira

@Composable
fun SettingsScreen(
    onAcessibilityClick: () -> Unit,
    onMonitoramentoClick: () -> Unit,
    onCuidadorClick: () -> Unit,
) {
    val context = LocalContext.current
    val monitorando by remember { PreferenciasMonitoramento.ligado(context) }.collectAsState()
    val repository = remember { AliraRepository.getInstance(context) }
    val cuidador by remember { repository.observarCuidadorDoUsuarioAtivo() }.collectAsState(initial = null)

    AliraTheme {
        AppScaffold {
            val listState = rememberTransformingLazyColumnState()
            ScreenScaffold(
                scrollState = listState,
            ) { contentPadding ->
                FundoAlira {
                    TransformingLazyColumn(contentPadding = contentPadding, state = listState) {
                        item { TituloTela("Configurações") }
                        item {
                            CartaoMenu(
                                icone = R.drawable.accessibility_icon,
                                titulo = "Acessibilidade",
                                subtitulo = "Contraste, vibração e voz",
                                onClick = acaoAcessivel("Acessibilidade", onAcessibilityClick),
                            )
                        }
                        // O liga/desliga do monitoramento contínuo e o modo
                        // demonstração ficam no Painel de monitoramento.
                        item {
                            CartaoMenu(
                                icone = R.drawable.monitor_icon,
                                titulo = "Monitoramento",
                                subtitulo = if (monitorando) "Ligado" else "Desligado",
                                onClick = acaoAcessivel("Monitoramento", onMonitoramentoClick),
                            )
                        }
                        // Cuidador: só leitura aqui; é preenchido no Perfil do celular.
                        item {
                            CartaoMenu(
                                icone = R.drawable.person_icon,
                                titulo = "Cuidador",
                                subtitulo = cuidador?.nome ?: "Preencha no celular",
                                onClick = acaoAcessivel("Cuidador", onCuidadorClick),
                            )
                        }

                        // Botões de teste: disparam o fluxo de emergência
                        // completo (pergunta, contagem, SMS para o contato) sem
                        // ninguém precisar cair ou passar mal. O SMS sai
                        // marcado com [TESTE]. Úteis para testar e para a
                        // demonstração na banca.
                        item { TituloTela("Testes de emergência") }
                        item {
                            CartaoMenu(
                                icone = R.drawable.info_icon,
                                titulo = "Testar queda",
                                subtitulo = "Simula uma queda",
                                corIcone = CoresAlira.Rosa,
                                onClick = acaoAcessivel("Testar alerta de queda") {
                                    CentralDeEmergencia.iniciar(context, TipoEmergencia.QUEDA, teste = true)
                                },
                            )
                        }
                        item {
                            CartaoMenu(
                                icone = R.drawable.heart_icon,
                                titulo = "Testar ansiedade",
                                subtitulo = "Simula coração acelerado",
                                corIcone = CoresAlira.Rosa,
                                onClick = acaoAcessivel("Testar alerta de ansiedade") {
                                    CentralDeEmergencia.iniciar(context, TipoEmergencia.ANSIEDADE, teste = true)
                                },
                            )
                        }
                        item {
                            CartaoMenu(
                                icone = R.drawable.heart_rate,
                                titulo = "Testar batimento",
                                subtitulo = "Simula batimento fora do normal",
                                corIcone = CoresAlira.Rosa,
                                onClick = acaoAcessivel("Testar batimento fora do normal") {
                                    CentralDeEmergencia.iniciar(context, TipoEmergencia.FREQUENCIA_ANORMAL, teste = true)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
