package me.slz_br.alira.presentation.viewmodels

import androidx.lifecycle.ViewModel

class HomeScreenViewModel(): ViewModel() {

    val visiblePermissionDialogueQueue = mutableListOf<String>()

    fun dismissDialog() {
        visiblePermissionDialogueQueue.removeAt(visiblePermissionDialogueQueue.lastIndex)
    }

    fun onPermissionResult(
        permission: String,
        isGranted: Boolean
    ) {

        if (!isGranted) {
            visiblePermissionDialogueQueue.add(0, permission)
        }

    }
}