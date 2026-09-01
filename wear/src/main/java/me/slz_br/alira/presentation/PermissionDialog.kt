package me.slz_br.alira.presentation

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.Text

@Composable
fun PermissionDialog(
    permission: String,
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
        text = { Text(permissionTextProvider.getDescription(isPermanentlyDeclined)) },
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