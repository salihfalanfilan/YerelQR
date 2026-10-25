package com.example.offlineqr.qr.engine

import android.graphics.Bitmap
import com.example.offlineqr.domain.model.QrConfig
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.concurrent.ConcurrentHashMap

object TemplateThumbnailCache {

    const val SAMPLE_URL = "https://github.com/salihfalanfilan/"
    private const val THUMBNAIL_SIZE = 140

    private val cache = ConcurrentHashMap<String, Bitmap>()
    private var cachedBaseMatrix: BitMatrix? = null

    @Synchronized
    private fun getBaseMatrix(): BitMatrix {
        val existing = cachedBaseMatrix
        if (existing != null) return existing

        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 0,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
        )
        val matrix = QRCodeWriter().encode(SAMPLE_URL, BarcodeFormat.QR_CODE, 0, 0, hints)
        cachedBaseMatrix = matrix
        return matrix
    }

    fun getThumbnail(templateId: String, config: QrConfig): Bitmap {
        val key = "${templateId}_${config.hashCode()}"
        cache[key]?.let {
            if (!it.isRecycled) return it
        }

        val matrix = getBaseMatrix()
        val bmp = QrCanvasRenderer.renderToBitmap(
            bitMatrix = matrix,
            config = config,
            targetSize = THUMBNAIL_SIZE
        )
        cache[key] = bmp
        return bmp
    }

    fun prewarm(templates: List<com.example.offlineqr.domain.model.QrTemplate>) {
        val matrix = getBaseMatrix()
        for (template in templates) {
            val key = "${template.id}_${template.qrConfig.hashCode()}"
            if (!cache.containsKey(key)) {
                val bmp = QrCanvasRenderer.renderToBitmap(
                    bitMatrix = matrix,
                    config = template.qrConfig,
                    targetSize = THUMBNAIL_SIZE
                )
                cache[key] = bmp
            }
        }
    }

    fun clear() {
        cache.clear()
    }
}
