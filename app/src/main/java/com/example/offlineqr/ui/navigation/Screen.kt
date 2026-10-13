package com.example.offlineqr.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Home : Screen

    @Serializable
    data object Scanner : Screen

    @Serializable
    data class CreateQr(val editQrId: Long? = null) : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data class FolderDetail(val folderId: Long) : Screen

    @Serializable
    data class QrDetail(val qrId: Long) : Screen
}
