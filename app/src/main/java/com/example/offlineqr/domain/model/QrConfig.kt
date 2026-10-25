package com.example.offlineqr.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class QrConfig(
    val backgroundColor: Long = 0xFFFFFFFFL,
    val backgroundSecondaryColor: Long? = null,
    val backgroundGradientType: String = GradientType.NONE.name,
    val foregroundColor: Long = 0xFF00677DL,
    val secondaryColor: Long? = null,
    val gradientType: String = GradientType.NONE.name,
    val moduleShape: String = ModuleShape.SQUARE.name,
    val finderShape: String = FinderShape.SQUARE.name,
    val errorCorrectionLevel: String = ErrorCorrectionLevel.H.name,
    val logoPath: String? = null,
    val logoScale: Float = 0.22f,
    val quietZone: Float = 2f,
    val qrScale: Float = 0.85f,
    val backgroundCornerRadius: Float = 0f,
    val logoCornerRadius: Float = 0f,
    val logoCutoutFillBackground: Boolean = false,
    val frameStyle: String = QrFrameStyle.NONE.name,
    val frameText: String = "",
    val frameColor: Long? = null,
    val frameTextColor: Long = 0xFFFFFFFFL,
    val gradientDirection: String = GradientDirection.DIAGONAL.name,
    val backgroundGradientDirection: String = GradientDirection.DIAGONAL.name
)
