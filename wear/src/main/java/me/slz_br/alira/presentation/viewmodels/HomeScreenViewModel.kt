package me.slz_br.alira.presentation.viewmodels

import androidx.lifecycle.ViewModel

class HomeScreenViewModel: ViewModel() {

    val visiblePermissionDialogQueue = mutableListOf<String>()

    fun dismissDialog() {
        visiblePermissionDialogQueue.removeFirst()
    }

    fun onPermissionResult(
        permission: String,
        isGranted: Boolean
    ) {
        if (!isGranted && permission !in visiblePermissionDialogQueue) {
            visiblePermissionDialogQueue.add(0, permission)
        }
    }
}