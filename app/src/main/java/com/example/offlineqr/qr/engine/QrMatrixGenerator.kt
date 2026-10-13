package com.example.offlineqr.qr.engine

import com.example.offlineqr.domain.model.ErrorCorrectionLevel
import com.example.offlineqr.domain.model.QrConfig
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel as ZxingEcLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object QrMatrixGenerator {

    suspend fun generateMatrix(
        content: String,
        config: QrConfig = QrConfig(),
        size: Int = 512
    ): BitMatrix = withContext(Dispatchers.Default) {
        require(content.isNotBlank()) { "Content cannot be empty" }

        val ecLevel = when (ErrorCorrectionLevel.fromString(config.errorCorrectionLevel)) {
            ErrorCorrectionLevel.L -> ZxingEcLevel.L
            ErrorCorrectionLevel.M -> ZxingEcLevel.M
            ErrorCorrectionLevel.Q -> ZxingEcLevel.Q
            ErrorCorrectionLevel.H -> ZxingEcLevel.H
        }

        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ecLevel,
            EncodeHintType.MARGIN to 0,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )

        QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    }
}
