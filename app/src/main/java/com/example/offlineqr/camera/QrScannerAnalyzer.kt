package com.example.offlineqr.camera

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class QrScannerAnalyzer(
    private val onBarcodeDetected: (barcode: String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner by lazy {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A
            )
            .build()
        BarcodeScanning.getClient(options)
    }

    private var lastScannedTimestamp: Long = 0L
    private var lastScannedValue: String? = null
    private val scanCooldownMillis = 1500L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        try {
            val inputImage = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    val now = System.currentTimeMillis()
                    val barcode = barcodes.firstOrNull()?.rawValue

                    if (!barcode.isNullOrBlank()) {
                        val isSameRecent = barcode == lastScannedValue && (now - lastScannedTimestamp < scanCooldownMillis)
                        if (!isSameRecent) {
                            lastScannedTimestamp = now
                            lastScannedValue = barcode
                            onBarcodeDetected(barcode)
                        }
                    }
                }
                .addOnFailureListener {
                    // Failures ignored for video frame stream
                }
                .addOnCompleteListener {
                    // Guaranteed ImageProxy release on complete
                    imageProxy.close()
                }
        } catch (e: Exception) {
            imageProxy.close()
        }
    }
}
