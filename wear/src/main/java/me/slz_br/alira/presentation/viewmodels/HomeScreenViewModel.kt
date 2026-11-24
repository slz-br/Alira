package me.slz_br.alira.presentation.viewmodels

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

class HomeScreenViewModel : ViewModel() {

    val visiblePermissionDialogQueue = mutableStateListOf<String>()

    fun dismissDialog() {
        if (visiblePermissionDialogQueue.isNotEmpty()) {
            visiblePermissionDialogQueue.removeAt(0)
        }
    }

    /**
     * Ao negar uma permissão necessária para utilizar uma feature do sistema, essa permissão é
     * adicionada na fila 'visiblePermissionQueue' para ser contextualizada e
     * requisitada novamente posteriormente (caso necessário).
     */
    fun onPermissionResult(
        permission: String,
        isGranted: Boolean,
    ) {
        if (!isGranted && permission !in visiblePermissionDialogQueue) {
            visiblePermissionDialogQueue.add(0, permission)
        }
    }
}
