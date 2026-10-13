package com.example.offlineqr.ui.scanner

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.offlineqr.domain.model.QrCode
import com.example.offlineqr.domain.model.QrConfig
import com.example.offlineqr.domain.model.QrType
import com.example.offlineqr.domain.usecase.SaveQrCodeUseCase
import com.example.offlineqr.domain.usecase.SettingsUseCases
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ScannerMode {
    AUTO_SCAN,
    BATCH_MODE,
    BARCODE_1D
}

sealed interface ScannerEvent {
    data class ScanSuccess(val qrId: Long) : ScannerEvent
    data class Error(val message: String) : ScannerEvent
}

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val saveQrCodeUseCase: SaveQrCodeUseCase,
    private val settingsUseCases: SettingsUseCases
) : ViewModel() {

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(false)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    private val _zoomMultiplier = MutableStateFlow(1.0f)
    val zoomMultiplier: StateFlow<Float> = _zoomMultiplier.asStateFlow()

    private val _selectedMode = MutableStateFlow(ScannerMode.AUTO_SCAN)
    val selectedMode: StateFlow<ScannerMode> = _selectedMode.asStateFlow()

    private val _events = MutableSharedFlow<ScannerEvent>()
    val events: SharedFlow<ScannerEvent> = _events.asSharedFlow()

    private var isProcessingScan = false

    fun toggleTorch() {
        _isTorchOn.value = !_isTorchOn.value
    }

    fun flipCamera() {
        _isFrontCamera.value = !_isFrontCamera.value
    }

    fun setZoom(zoom: Float) {
        _zoomMultiplier.value = zoom
    }

    fun setMode(mode: ScannerMode) {
        _selectedMode.value = mode
    }

    fun onBarcodeDetected(barcode: String, context: Context) {
        if (isProcessingScan && _selectedMode.value != ScannerMode.BATCH_MODE) return
        isProcessingScan = true

        viewModelScope.launch {
            try {
                // Haptic feedback
                val vibrate = settingsUseCases.vibrateOnScan.first()
                if (vibrate) {
                    performHapticFeedback(context)
                }

                // Audio beep
                val beep = settingsUseCases.beepOnScan.first()
                if (beep) {
                    playBeepTone()
                }

                // Save as TARANAN
                val qrCode = QrCode(
                    content = barcode,
                    type = QrType.TARANAN,
                    qrConfig = QrConfig()
                )
                val id = saveQrCodeUseCase(qrCode)

                if (_selectedMode.value != ScannerMode.BATCH_MODE) {
                    _events.emit(ScannerEvent.ScanSuccess(id))
                } else {
                    isProcessingScan = false
                }
            } catch (e: Exception) {
                _events.emit(ScannerEvent.Error(e.localizedMessage ?: "Scan processing failed"))
                isProcessingScan = false
            }
        }
    }

    fun decodeFromGallery(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                val scanner = BarcodeScanning.getClient(
                    BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                        .build()
                )
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        val barcode = barcodes.firstOrNull()?.rawValue
                        if (!barcode.isNullOrBlank()) {
                            onBarcodeDetected(barcode, context)
                        } else {
                            viewModelScope.launch {
                                _events.emit(ScannerEvent.Error("No barcode or QR code found in selected image."))
                            }
                        }
                    }
                    .addOnFailureListener {
                        viewModelScope.launch {
                            _events.emit(ScannerEvent.Error("Failed to decode image: ${it.localizedMessage}"))
                        }
                    }
            } catch (e: Exception) {
                _events.emit(ScannerEvent.Error(e.localizedMessage ?: "Failed to open image"))
            }
        }
    }

    private fun performHapticFeedback(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(100)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playBeepTone() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
