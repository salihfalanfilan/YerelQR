package com.example.offlineqr.domain.model

data class QrCode(
    val id: Long = 0,
    val content: String,
    val type: QrType,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val folderId: Long? = null,
    val qrConfig: QrConfig = QrConfig(),
    val payloadType: QrPayloadType = QrPayloadType.fromContent(content)
)
