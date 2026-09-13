package me.slz_br.alira.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Text

@Composable
fun PermissionDialog(
    permissionTextProvider: PermissionTextProvider,
    isPermanentlyDeclined: Boolean,
    onDismiss: () -> Unit,
    onOkClick: () -> Unit,
    onGoToAppSettings: () -> Unit
) {
    AlertDialog(
        visible = true,
        onDismissRequest = onDismiss,
        title = {
            Text("Permissão necessária")
        },
        text = {
            Text(permissionTextProvider.getDescription(isPermanentlyDeclined))
       },
        edgeButton = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                EdgeButton(
                    onClick = {
                        if (isPermanentlyDeclined) {
                            onGoToAppSettings()
                        } else {
                            onOkClick()
                        }
                    }
                ) {
                    Text(
                        if (isPermanentlyDeclined) {
                            "Dar permissão"
                        }
                        else {
                            "Ok"
                        },
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    )
}

interface PermissionTextProvider {
    fun getDescription(isPermanentlyDeclined: Boolean): String
}

object ActivityRecognitionPermissionTextProvider: PermissionTextProvider {
    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if (isPermanentlyDeclined) {
            "Parece que você negou permanentemente a permissão de reconhecimento de atividades físicas " +
            "Habilite-a nas configurações do aplicativo para acessar essa função"
        }
        else {
            "O aplicativo precisa acessar dados de suas atividades físicas para realizar o monitoramento de seus " +
            "sinais vitais."
        }
    }

}

object FrequenciaCardiacaPermissionTextProvider : PermissionTextProvider {
    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if (isPermanentlyDeclined) {
            "Parece que você negou permanentemente o acesso aos sensores corporais. " +
            "Habilite-o nas configurações do aplicativo para medir sua frequência cardíaca."
        }
        else {
            "O aplicativo precisa acessar o sensor de frequência cardíaca do relógio para " +
            "acompanhar seus batimentos."
        }
    }
}