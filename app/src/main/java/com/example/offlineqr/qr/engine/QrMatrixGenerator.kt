package com.example.offlineqr.qr.engine

import com.example.offlineqr.domain.model.ErrorCorrectionLevel
import com.example.offlineqr.domain.model.QrConfig
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel as ZxingEcLevel
import java.util.Collections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object QrMatrixGenerator {

    private const val MAX_CACHE_ENTRIES = 128

    // Pure Kotlin/Java LinkedHashMap LRU cache - safe across Android ART and JVM unit tests
    private val matrixCache = Collections.synchronizedMap(
        object : LinkedHashMap<String, BitMatrix>(32, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, BitMatrix>?): Boolean {
                return size > MAX_CACHE_ENTRIES
            }
        }
    )

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

        val cacheKey = "${content}_${ecLevel.name}_$size"
        matrixCache[cacheKey]?.let { return@withContext it }

        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ecLevel,
            EncodeHintType.MARGIN to 0,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )

        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        matrixCache[cacheKey] = matrix
        matrix
    }

    fun clearCache() {
        matrixCache.clear()
    }
}
