package com.example.offlineqr.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class QrTemplate(
    val id: String,
    val name: String,
    val qrConfig: QrConfig,
    val isBuiltIn: Boolean = false
)
