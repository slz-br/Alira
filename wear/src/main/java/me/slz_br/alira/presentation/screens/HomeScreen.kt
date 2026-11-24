package me.slz_br.alira.presentation.screens

import android.Manifest.permission.ACTIVITY_RECOGNITION
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import me.slz_br.alira.R
import me.slz_br.alira.acessibilidade.acaoAcessivel
import me.slz_br.alira.data.repository.AliraRepository
import me.slz_br.alira.monitoramento.PreferenciasMonitoramento
import me.slz_br.alira.presentation.ActivityRecognitionPermissionTextProvider
import me.slz_br.alira.presentation.FrequenciaCardiacaPermissionTextProvider
import me.slz_br.alira.presentation.PermissionDialog
import me.slz_br.alira.presentation.componentes.CartaoMenu
import me.slz_br.alira.presentation.componentes.FundoAlira
import me.slz_br.alira.presentation.permissaoFrequenciaCardiaca
import me.slz_br.alira.presentation.permissoesSinaisVitais
import me.slz_br.alira.presentation.theme.AliraTheme
import me.slz_br.alira.presentation.theme.CoresAlira
import me.slz_br.alira.presentation.theme.LocalAltoContraste
import me.slz_br.alira.presentation.viewmodels.HomeScreenViewModel

@Composable
fun WearApp(
    viewModel: HomeScreenViewModel = viewModel(),
    onVitalsClick: () -> Unit = {},
    onMonitoramentoClick: () -> Unit = {},
    onGenerateReportClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onInfoScreenClick: () -> Unit = {}
) {
    AliraTheme {
        AppScaffold {
            val context = LocalContext.current
            val activity = LocalActivity.current!!
            val dialogueQueue = viewModel.visiblePermissionDialogQueue
            val repository = remember { AliraRepository.getInstance(context) }
            val usuario by remember { repository.observarUsuarioAtual() }.collectAsState(initial = null)
            val monitorando by remember { PreferenciasMonitoramento.ligado(context) }.collectAsState()

            // Para onde ir depois que a pessoa responder o pedido de
            // permissão (a tela de sinais vitais).
            var destinoAposPermissao by remember { mutableStateOf<(() -> Unit)?>(null) }

            val permissoesResultLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions(),
                onResult = { resultados ->
                    resultados.forEach { (permissao, concedida) ->
                        viewModel.onPermissionResult(
                            permission = permissao,
                            isGranted = concedida
                        )
                    }
                    if (resultados.values.any { it }) {
                        destinoAposPermissao?.invoke()
                    }
                    destinoAposPermissao = null
                }
            )

            // Abre [destino] se as [permissoes] já foram dadas; senão, pede
            // primeiro e abre depois (no onResult acima).
            fun abrirComPermissao(permissoes: List<String>, destino: () -> Unit) {
                val faltando = permissoes.filter { permissao ->
                    ActivityCompat.checkSelfPermission(context, permissao) !=
                        PackageManager.PERMISSION_GRANTED
                }
                if (faltando.isEmpty()) {
                    destino()
                } else {
                    destinoAposPermissao = destino
                    permissoesResultLauncher.launch(faltando.toTypedArray())
                }
            }

            val listState = rememberTransformingLazyColumnState()
            ScreenScaffold(
                scrollState = listState,
                edgeButton = {
                    EdgeButton(
                        onClick = acaoAcessivel("Créditos", onInfoScreenClick),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                    ) {
                        Text("Créditos")
                    }
                },
            ) { contentPadding -> // ScreenScaffold provides default padding; adjust as needed
                FundoAlira {
                    TransformingLazyColumn(contentPadding = contentPadding, state = listState) {
                        item {
                            Cabecalho(nome = usuario?.nome?.substringBefore(" ")?.ifBlank { null } ?: "Usuário")
                        }
                        item {
                            CartaoMenu(
                                icone = R.drawable.heart_rate,
                                titulo = "Sinais Vitais",
                                subtitulo = "Batimentos, passos, calorias",
                                corIcone = CoresAlira.Rosa,
                                onClick = acaoAcessivel("Sinais vitais") {
                                    abrirComPermissao(permissoesSinaisVitais, onVitalsClick)
                                },
                            )
                        }
                        item {
                            CartaoMenu(
                                icone = R.drawable.monitor_icon,
                                titulo = "Monitoramento",
                                subtitulo = if (monitorando) "Ligado · vigiando quedas" else "Desligado",
                                onClick = acaoAcessivel("Monitoramento", onMonitoramentoClick),
                            )
                        }
                        item {
                            CartaoMenu(
                                icone = R.drawable.chart_icon,
                                titulo = "Relatórios",
                                subtitulo = "Histórico e PDF",
                                onClick = acaoAcessivel("Relatórios", onGenerateReportClick),
                            )
                        }
                        item {
                            CartaoMenu(
                                icone = R.drawable.settings_icon,
                                titulo = "Configurações",
                                subtitulo = "Acessibilidade e testes",
                                onClick = acaoAcessivel("Configurações", onSettingsClick),
                            )
                        }
                    }
                }

                dialogueQueue
                    .reversed()
                    .forEach { permission ->
                        PermissionDialog(
                            permissionTextProvider = when (permission) {
                                ACTIVITY_RECOGNITION -> ActivityRecognitionPermissionTextProvider
                                permissaoFrequenciaCardiaca -> FrequenciaCardiacaPermissionTextProvider
                                else -> return@forEach
                            },
                            isPermanentlyDeclined = !activity.shouldShowRequestPermissionRationale(permission),
                            onDismiss = viewModel::dismissDialog,
                            onOkClick = {
                                viewModel.dismissDialog()
                                permissoesResultLauncher.launch(arrayOf(permission))
                            },
                            onGoToAppSettings = activity::openAppSettings
                        )
                    }
            }
        }
    }
}

/**
 * Topo da tela inicial: a marca "ALIRA" pequena e espaçada e, embaixo,
 * "Olá, <primeiro nome>!" em letra grande.
 */
@Composable
private fun Cabecalho(nome: String) {
    val altoContraste = LocalAltoContraste.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "A L I R A",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (altoContraste) MaterialTheme.colorScheme.primary else CoresAlira.VioletaClaro,
        )
        Text(
            text = stringResource(R.string.hello_world, nome),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

fun Activity.openAppSettings() {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).also(::startActivity)
}
