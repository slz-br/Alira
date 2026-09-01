package me.slz_br.alira.presentation.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import me.slz_br.alira.R
import me.slz_br.alira.presentation.ActivityRecognitionPermissionTextProvider
import me.slz_br.alira.presentation.MainActivity
import me.slz_br.alira.presentation.PermissionDialog
import me.slz_br.alira.presentation.viewmodels.HomeScreenViewModel
import me.slz_br.alira.presentation.theme.AliraTheme

@Composable
fun WearApp(
    viewModel: HomeScreenViewModel = viewModel(),
    onVitalsClick: () -> Unit,
    onGenerateReportClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onInfoScreenClick: () -> Unit
) {
    AliraTheme {
        AppScaffold {
            val context = LocalContext.current

            val monitoringPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { isGranted ->
                    viewModel.onPermissionResult(Manifest.permission.ACTIVITY_RECOGNITION, isGranted)
                }
            )

//            val multiplePermissionLauncher = rememberLauncherForActivityResult(
//                contract = ActivityResultContracts.RequestMultiplePermissions(),
//                onResult = { perms ->
//                    perms.keys.forEach { permission ->
//                        viewModel.onPermissionResult(
//                            permission,
//                            perms[permission] == true
//                        )
//                    }
//                }
//            )
            val listState = rememberTransformingLazyColumnState()
            val transformationSpec = rememberTransformationSpec()
            ScreenScaffold(
                scrollState = listState,
                edgeButton = {
                    EdgeButton(
                        onClick = onInfoScreenClick,
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
                TransformingLazyColumn(contentPadding = contentPadding, state = listState) {
                    item {
                        ListHeader(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .transformedHeight(this, transformationSpec),
                        ) {
                            Text(fontWeight = FontWeight(800), text = "Alira")
                        }
                    }
                    item {
                        FilledIconButton(
                            onClick = {
                                monitoringPermissionLauncher.launch(
                                    Manifest.permission.ACTIVITY_RECOGNITION
                                )

                                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
                                    onVitalsClick()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Row {
                                Icon(
                                    painter = painterResource(R.drawable.heart_icon),
                                    contentDescription = "Botão de sinais vitais"
                                )
                                Text(
                                    text = "Sinais Vitais"
                                )
                            }
                            viewModel.visiblePermissionDialogueQueue.reversed().forEach { permission ->
                                PermissionDialog(
                                    permission = permission,
                                    permissionTextProvider = when (permission) {
                                        Manifest.permission.ACTIVITY_RECOGNITION -> ActivityRecognitionPermissionTextProvider
                                        else -> error("Unknown requested permission")
                                    },
                                    isPermanentlyDeclined = MainActivity().shouldShowRequestPermissionRationale(permission),
                                    onDismiss = {},
                                    onOkClick = {},
                                    onGoToAppSettings = { }
                                )
                            }
                        }
                    }
                    item {
                        FilledIconButton (
                            onClick = onGenerateReportClick,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Row {
                                Icon(
                                    painter = painterResource(R.drawable.chart_icon),
                                    contentDescription = "Gerar relatório"
                                )
                                Text("Gerar relatório")
                            }
                        }
                    }
                    item {
                        FilledIconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row {
                                Icon(
                                    painter = painterResource(R.drawable.settings_icon),
                                    contentDescription = "Engrenagem de configurações"
                                )
                                Text("Configurações")
                            }
                        }
                    }
                }
            }
        }
    }
}

fun Activity.openAppSettings() {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).also(::startActivity)
}

@Preview
@Composable
private fun HomeScreenPreview() {
    WearApp(
        onVitalsClick = {},
        onGenerateReportClick = {},
        onSettingsClick = {},
        onInfoScreenClick = {}
    )
}