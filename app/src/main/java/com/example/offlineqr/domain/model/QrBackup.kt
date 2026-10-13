package com.example.offlineqr.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class QrBackupItem(
    val content: String,
    val type: String = "OLUSTURULAN",
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val folderName: String? = null,
    val qrConfig: QrConfig = QrConfig()
)

@Serializable
data class QrBackupData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val app: String = "OfflineQR",
    val items: List<QrBackupItem> = emptyList()
)
