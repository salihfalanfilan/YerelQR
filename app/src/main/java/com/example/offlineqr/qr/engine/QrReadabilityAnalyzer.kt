package com.example.offlineqr.qr.engine

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

sealed interface ReadabilityResult {
    data object Idle : ReadabilityResult
    data object Testing : ReadabilityResult
    data class Success(val decodedText: String) : ReadabilityResult
    data class Warning(val message: String) : ReadabilityResult
}

object QrReadabilityAnalyzer {

    private val scanner by lazy {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }

    suspend fun analyzeBitmap(bitmap: Bitmap): ReadabilityResult = withContext(Dispatchers.Default) {
        // Transparent (rounded) corners are read as black by the decoder; composite onto white first
        val flat = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        android.graphics.Canvas(flat).apply {
            drawColor(android.graphics.Color.WHITE)
            drawBitmap(bitmap, 0f, 0f, null)
        }
        val inputImage = InputImage.fromBitmap(flat, 0)

        suspendCancellableCoroutine { continuation ->
            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    val qr = barcodes.firstOrNull()
                    if (qr != null && !qr.rawValue.isNullOrBlank()) {
                        continuation.resume(ReadabilityResult.Success(qr.rawValue!!))
                    } else {
                        continuation.resume(
                            ReadabilityResult.Warning(
                                "Design may be difficult to scan. Increase error correction or boost color contrast."
                            )
                        )
                    }
                }
                .addOnFailureListener { error ->
                    continuation.resume(
                        ReadabilityResult.Warning(
                            error.localizedMessage ?: "Failed to verify readability."
                        )
                    )
                }
        }
    }
}
