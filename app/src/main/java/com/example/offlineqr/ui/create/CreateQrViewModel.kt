package com.example.offlineqr.ui.create

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.offlineqr.domain.model.ErrorCorrectionLevel
import com.example.offlineqr.domain.model.FinderShape
import com.example.offlineqr.domain.model.GradientType
import com.example.offlineqr.domain.model.ModuleShape
import com.example.offlineqr.domain.model.QrCode
import com.example.offlineqr.domain.model.QrConfig
import com.example.offlineqr.domain.model.QrFrameStyle
import com.example.offlineqr.domain.model.QrPayloadType
import com.example.offlineqr.domain.model.QrType
import com.example.offlineqr.domain.usecase.GetQrCodesUseCase
import com.example.offlineqr.domain.usecase.SaveQrCodeUseCase
import com.example.offlineqr.domain.usecase.SettingsUseCases
import com.example.offlineqr.domain.model.DefaultTemplates
import com.example.offlineqr.domain.model.GradientDirection
import com.example.offlineqr.domain.model.QrTemplate
import com.example.offlineqr.domain.usecase.UpdateQrCodeUseCase
import com.example.offlineqr.qr.engine.LogoBitmapCache
import com.example.offlineqr.qr.engine.QrBitmapCache
import com.example.offlineqr.qr.engine.QrCanvasRenderer
import com.example.offlineqr.qr.engine.QrMatrixGenerator
import com.example.offlineqr.qr.engine.QrReadabilityAnalyzer
import com.example.offlineqr.qr.engine.QrSvgRenderer
import com.example.offlineqr.qr.engine.ReadabilityResult
import com.example.offlineqr.qr.engine.TemplateThumbnailCache
import com.example.offlineqr.qr.export.ExportFormat
import com.example.offlineqr.qr.export.QrExporter
import com.google.zxing.common.BitMatrix
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

data class CreateQrUiState(
    val isEditMode: Boolean = false,
    val editQrId: Long? = null,
    val payloadType: QrPayloadType = QrPayloadType.URL,
    val urlText: String = "https://material.io/design",
    val textContent: String = "",
    val wifiSsid: String = "",
    val wifiPassword: String = "",
    val wifiSecurity: String = "WPA",
    val contactName: String = "",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val contactOrg: String = "",
    val emailTo: String = "",
    val emailSubject: String = "",
    val emailBody: String = "",
    val qrConfig: QrConfig = QrConfig(),
    val previewBitmap: Bitmap? = null,
    val previewSvg: String? = null,
    val readabilityResult: ReadabilityResult = ReadabilityResult.Idle,
    val customColors: List<String> = emptyList(),
    val templates: List<QrTemplate> = DefaultTemplates.list,
    val isSaving: Boolean = false,
    val error: String? = null
)

private data class ConfigAndAssets(
    val cfg: QrConfig,
    val svg: String?,
    val colors: List<String>,
    val templates: List<QrTemplate>
)

@HiltViewModel
class CreateQrViewModel @Inject constructor(
    private val saveQrCodeUseCase: SaveQrCodeUseCase,
    private val updateQrCodeUseCase: UpdateQrCodeUseCase,
    private val getQrCodesUseCase: GetQrCodesUseCase,
    private val settingsUseCases: SettingsUseCases
) : ViewModel() {

    private val _isEditMode = MutableStateFlow(false)
    private val _editQrId = MutableStateFlow<Long?>(null)
    private var initialContent: String? = null

    private val _payloadType = MutableStateFlow(QrPayloadType.URL)
    private val _urlText = MutableStateFlow("https://")
    private val _textContent = MutableStateFlow("")
    private val _wifiSsid = MutableStateFlow("")
    private val _wifiPassword = MutableStateFlow("")
    private val _wifiSecurity = MutableStateFlow("WPA")
    private val _contactName = MutableStateFlow("")
    private val _contactPhone = MutableStateFlow("")
    private val _contactEmail = MutableStateFlow("")
    private val _contactOrg = MutableStateFlow("")
    private val _emailTo = MutableStateFlow("")
    private val _emailSubject = MutableStateFlow("")
    private val _emailBody = MutableStateFlow("")

    private val _qrConfig = MutableStateFlow(QrConfig())
    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

    private val _previewSvg = MutableStateFlow<String?>(null)
    val previewSvg: StateFlow<String?> = _previewSvg.asStateFlow()

    private val _readabilityResult = MutableStateFlow<ReadabilityResult>(ReadabilityResult.Idle)
    val readabilityResult: StateFlow<ReadabilityResult> = _readabilityResult.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    private val _events = MutableSharedFlow<Long>()
    val events: SharedFlow<Long> = _events.asSharedFlow()

    // Matrix caching for optimization
    private var cachedMatrix: BitMatrix? = null
    private var cachedContent: String? = null
    private var cachedEcLevel: String? = null

    // Double buffer ping-pong: zero heap allocations while dragging sliders
    private var bufferA: Bitmap? = null
    private var bufferB: Bitmap? = null
    private var useBufferA = false

    private val previewChannel = Channel<Unit>(Channel.CONFLATED)
    private var debounceJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.Default) {
            for (request in previewChannel) {
                renderPreviewInternal()
                delay(14)
            }
        }

        viewModelScope.launch {
            val defaultEc = settingsUseCases.defaultErrorCorrection
            defaultEc.collect { ec ->
                if (!_isEditMode.value) {
                    val maxAllowed = getMaxAllowedLogoScale(ec)
                    val newLogoScale = _qrConfig.value.logoScale.coerceAtMost(maxAllowed)
                    _qrConfig.value = _qrConfig.value.copy(
                        errorCorrectionLevel = ec.name,
                        logoScale = newLogoScale
                    )
                    schedulePreview(immediate = true)
                }
            }
        }

        viewModelScope.launch(Dispatchers.Default) {
            TemplateThumbnailCache.prewarm(DefaultTemplates.list)
        }
    }

    fun loadForEdit(id: Long) {
        viewModelScope.launch {
            val qr = getQrCodesUseCase.getById(id).firstOrNull() ?: return@launch
            _isEditMode.value = true
            _editQrId.value = id
            initialContent = qr.content
            _qrConfig.value = qr.qrConfig

            // Parse payload
            when {
                qr.content.startsWith("http://") || qr.content.startsWith("https://") -> {
                    _payloadType.value = QrPayloadType.URL
                    _urlText.value = qr.content
                }
                qr.content.startsWith("WIFI:") -> {
                    _payloadType.value = QrPayloadType.WIFI
                    // Parse SSID and pass
                    val ssid = Regex("S:([^;]+);").find(qr.content)?.groupValues?.get(1) ?: ""
                    val pass = Regex("P:([^;]+);").find(qr.content)?.groupValues?.get(1) ?: ""
                    val sec = Regex("T:([^;]+);").find(qr.content)?.groupValues?.get(1) ?: "WPA"
                    _wifiSsid.value = ssid
                    _wifiPassword.value = pass
                    _wifiSecurity.value = sec
                }
                qr.content.startsWith("BEGIN:VCARD") -> {
                    _payloadType.value = QrPayloadType.CONTACT
                    _contactName.value = Regex("FN:([^\\n]+)").find(qr.content)?.groupValues?.get(1) ?: ""
                    _contactPhone.value = Regex("TEL:([^\\n]+)").find(qr.content)?.groupValues?.get(1) ?: ""
                    _contactEmail.value = Regex("EMAIL:([^\\n]+)").find(qr.content)?.groupValues?.get(1) ?: ""
                    _contactOrg.value = Regex("ORG:([^\\n]+)").find(qr.content)?.groupValues?.get(1) ?: ""
                }
                qr.content.startsWith("mailto:") -> {
                    _payloadType.value = QrPayloadType.EMAIL
                    val uri = Uri.parse(qr.content)
                    _emailTo.value = uri.schemeSpecificPart.substringBefore("?")
                    _emailSubject.value = uri.getQueryParameter("subject") ?: ""
                    _emailBody.value = uri.getQueryParameter("body") ?: ""
                }
                else -> {
                    _payloadType.value = QrPayloadType.TEXT
                    _textContent.value = qr.content
                }
            }

            schedulePreview(immediate = true)
        }
    }

    val uiState: StateFlow<CreateQrUiState> = combine(
        combine(
            _isEditMode, _editQrId, _payloadType, _urlText, _textContent
        ) { editMode, editId, pt, url, txt ->
            listOf(editMode, editId, pt, url, txt)
        },
        combine(
            _wifiSsid, _wifiPassword, _wifiSecurity, _contactName, _contactPhone
        ) { ssid, pass, sec, name, phone ->
            listOf(ssid, pass, sec, name, phone)
        },
        combine(
            _contactEmail, _contactOrg, _emailTo, _emailSubject, _emailBody
        ) { cEmail, cOrg, to, subj, body ->
            listOf(cEmail, cOrg, to, subj, body)
        },
        combine(
            _qrConfig, _previewSvg, settingsUseCases.customColors, settingsUseCases.customTemplates
        ) { cfg, svg, colors, customTpls ->
            ConfigAndAssets(cfg, svg, colors, DefaultTemplates.list + customTpls)
        },
        combine(_isSaving, _error) { saving, err -> Pair(saving, err) }
    ) { p1, p2, p3, p4, p5 ->
        CreateQrUiState(
            isEditMode = p1[0] as Boolean,
            editQrId = p1[1] as Long?,
            payloadType = p1[2] as QrPayloadType,
            urlText = p1[3] as String,
            textContent = p1[4] as String,
            wifiSsid = p2[0] as String,
            wifiPassword = p2[1] as String,
            wifiSecurity = p2[2] as String,
            contactName = p2[3] as String,
            contactPhone = p2[4] as String,
            contactEmail = p3[0] as String,
            contactOrg = p3[1] as String,
            emailTo = p3[2] as String,
            emailSubject = p3[3] as String,
            emailBody = p3[4] as String,
            qrConfig = p4.cfg,
            previewBitmap = _previewBitmap.value,
            previewSvg = p4.svg,
            readabilityResult = _readabilityResult.value,
            customColors = p4.colors,
            templates = p4.templates,
            isSaving = p5.first,
            error = p5.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CreateQrUiState()
    )

    fun getMaxAllowedLogoScale(ec: ErrorCorrectionLevel): Float {
        return when (ec) {
            ErrorCorrectionLevel.L -> 0.12f
            ErrorCorrectionLevel.M -> 0.18f
            ErrorCorrectionLevel.Q -> 0.24f
            ErrorCorrectionLevel.H -> 0.32f
        }
    }

    fun setPayloadType(type: QrPayloadType) {
        _payloadType.value = type
        schedulePreview(immediate = true)
    }

    fun onUrlChanged(url: String) {
        _urlText.value = url
        schedulePreview(immediate = false)
    }

    fun onTextChanged(text: String) {
        _textContent.value = text
        schedulePreview(immediate = false)
    }

    fun onWifiChanged(ssid: String, pass: String, sec: String) {
        _wifiSsid.value = ssid
        _wifiPassword.value = pass
        _wifiSecurity.value = sec
        schedulePreview(immediate = false)
    }

    fun onContactChanged(name: String, phone: String, email: String, org: String) {
        _contactName.value = name
        _contactPhone.value = phone
        _contactEmail.value = email
        _contactOrg.value = org
        schedulePreview(immediate = false)
    }

    fun onEmailChanged(to: String, subject: String, body: String) {
        _emailTo.value = to
        _emailSubject.value = subject
        _emailBody.value = body
        schedulePreview(immediate = false)
    }

    fun updateFinderShape(shape: FinderShape) {
        _qrConfig.value = _qrConfig.value.copy(finderShape = shape.name)
        schedulePreview(immediate = true)
    }

    fun updateModuleShape(shape: ModuleShape) {
        _qrConfig.value = _qrConfig.value.copy(moduleShape = shape.name)
        schedulePreview(immediate = true)
    }

    fun updateForeground(
        primary: Long,
        secondary: Long? = null,
        gradientType: GradientType = GradientType.NONE
    ) {
        _qrConfig.value = _qrConfig.value.copy(
            foregroundColor = primary,
            secondaryColor = secondary,
            gradientType = gradientType.name
        )
        schedulePreview(immediate = true)
    }

    fun updateBackground(
        primary: Long,
        secondary: Long? = null,
        gradientType: GradientType = GradientType.NONE
    ) {
        _qrConfig.value = _qrConfig.value.copy(
            backgroundColor = primary,
            backgroundSecondaryColor = secondary,
            backgroundGradientType = gradientType.name
        )
        schedulePreview(immediate = true)
    }

    fun updateQrScale(scale: Float) {
        _qrConfig.value = _qrConfig.value.copy(qrScale = scale.coerceIn(0.50f, 1.0f))
        schedulePreview(immediate = false)
    }

    fun updateQuietZone(zone: Float) {
        _qrConfig.value = _qrConfig.value.copy(quietZone = zone.coerceIn(0f, 8f))
        schedulePreview(immediate = false)
    }

    fun updateBackgroundCornerRadius(radius: Float) {
        _qrConfig.value = _qrConfig.value.copy(backgroundCornerRadius = radius.coerceIn(0f, 0.5f))
        schedulePreview(immediate = false)
    }

    fun updateErrorCorrection(level: ErrorCorrectionLevel) {
        val maxAllowed = getMaxAllowedLogoScale(level)
        val newLogoScale = _qrConfig.value.logoScale.coerceAtMost(maxAllowed)
        _qrConfig.value = _qrConfig.value.copy(
            errorCorrectionLevel = level.name,
            logoScale = newLogoScale
        )
        schedulePreview(immediate = true)
    }

    fun updateLogoScale(scale: Float) {
        val currentEc = ErrorCorrectionLevel.fromString(_qrConfig.value.errorCorrectionLevel)
        val maxAllowed = getMaxAllowedLogoScale(currentEc)
        val clamped = scale.coerceIn(0.08f, maxAllowed)
        _qrConfig.value = _qrConfig.value.copy(logoScale = clamped)
        schedulePreview(immediate = false)
    }

    fun updateLogoCornerRadius(radius: Float) {
        _qrConfig.value = _qrConfig.value.copy(logoCornerRadius = radius.coerceIn(0f, 0.5f))
        schedulePreview(immediate = false)
    }

    fun updateLogoCutoutFillBackground(fill: Boolean) {
        _qrConfig.value = _qrConfig.value.copy(logoCutoutFillBackground = fill)
        schedulePreview(immediate = true)
    }

    fun setLogo(uri: Uri?, context: Context) {
        viewModelScope.launch {
            LogoBitmapCache.clear()
            if (uri == null) {
                _qrConfig.value = _qrConfig.value.copy(logoPath = null)
            } else {
                val logoFile = withContext(Dispatchers.IO) {
                    val dir = File(context.filesDir, "logos").apply { mkdirs() }
                    val file = File(dir, "logo_${System.currentTimeMillis()}.png")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                    file
                }
                _qrConfig.value = _qrConfig.value.copy(logoPath = logoFile.absolutePath)
            }
            schedulePreview(immediate = true)
        }
    }

    fun removeLogo() {
        LogoBitmapCache.clear()
        _qrConfig.value = _qrConfig.value.copy(logoPath = null)
        schedulePreview(immediate = true)
    }

    fun updateFrameStyle(style: QrFrameStyle) {
        val currentText = _qrConfig.value.frameText
        val defaultText = if (currentText.isBlank()) "SCAN ME" else currentText
        _qrConfig.value = _qrConfig.value.copy(
            frameStyle = style.name,
            frameText = if (style == QrFrameStyle.NONE) currentText else defaultText
        )
        schedulePreview(immediate = true)
    }

    fun updateFrameText(text: String) {
        _qrConfig.value = _qrConfig.value.copy(frameText = text)
        schedulePreview(immediate = true)
    }

    fun updateFrameColor(color: Long?) {
        _qrConfig.value = _qrConfig.value.copy(frameColor = color)
        schedulePreview(immediate = true)
    }

    fun updateFrameTextColor(color: Long) {
        _qrConfig.value = _qrConfig.value.copy(frameTextColor = color)
        schedulePreview(immediate = true)
    }

    fun updateForegroundDirection(direction: GradientDirection) {
        val newType = if (direction == GradientDirection.RADIAL) GradientType.RADIAL else GradientType.LINEAR
        _qrConfig.value = _qrConfig.value.copy(
            gradientDirection = direction.name,
            gradientType = newType.name
        )
        schedulePreview(immediate = true)
    }

    fun updateBackgroundDirection(direction: GradientDirection) {
        val newType = if (direction == GradientDirection.RADIAL) GradientType.RADIAL else GradientType.LINEAR
        _qrConfig.value = _qrConfig.value.copy(
            backgroundGradientDirection = direction.name,
            backgroundGradientType = newType.name
        )
        schedulePreview(immediate = true)
    }

    fun applyTemplate(template: QrTemplate) {
        _qrConfig.value = template.qrConfig
        schedulePreview(immediate = true)
    }

    fun saveCurrentAsTemplate(name: String) {
        viewModelScope.launch {
            val template = QrTemplate(
                id = "custom_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}",
                name = name.trim().ifBlank { "Özel Şablon" },
                qrConfig = _qrConfig.value,
                isBuiltIn = false
            )
            settingsUseCases.saveCustomTemplate(template)
        }
    }

    fun removeCustomTemplate(id: String) {
        viewModelScope.launch {
            settingsUseCases.removeCustomTemplate(id)
        }
    }

    suspend fun verifyScannability(): ReadabilityResult {
        val content = buildCurrentPayload()
        if (content.isBlank()) return ReadabilityResult.Warning("İçerik boş.")
        val config = _qrConfig.value
        return withContext(Dispatchers.Default) {
            try {
                val matrix = QrMatrixGenerator.generateMatrix(content, config, 0)
                val bmp = QrCanvasRenderer.renderToBitmap(matrix, config, 512)
                QrReadabilityAnalyzer.analyzeBitmap(bmp)
            } catch (e: Exception) {
                ReadabilityResult.Warning(e.localizedMessage ?: "Taranabilirlik analizi yapılamadı.")
            }
        }
    }

    fun saveInAppOnly(
        onSuccess: ((Long) -> Unit)? = null
    ) {
        val content = buildCurrentPayload()
        if (content.isBlank()) {
            _error.value = ERROR_CONTENT_EMPTY
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val config = _qrConfig.value
                val qrId: Long = if (_isEditMode.value && _editQrId.value != null) {
                    val existingId = _editQrId.value!!
                    val existing = getQrCodesUseCase.getById(existingId).firstOrNull()
                    if (existing != null) {
                        val updated = existing.copy(qrConfig = config, type = QrType.OLUSTURULAN)
                        updateQrCodeUseCase(updated)
                        existingId
                    } else {
                        saveQrCodeUseCase(QrCode(content = content, type = QrType.OLUSTURULAN, qrConfig = config))
                    }
                } else {
                    saveQrCodeUseCase(QrCode(content = content, type = QrType.OLUSTURULAN, qrConfig = config))
                }

                _events.emit(qrId)
                onSuccess?.invoke(qrId)
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Kayıt işlemi başarısız"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun saveAndExport(
        format: ExportFormat,
        context: Context,
        onSuccess: ((Long) -> Unit)? = null
    ) {
        val content = buildCurrentPayload()
        if (content.isBlank()) {
            _error.value = ERROR_CONTENT_EMPTY
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val config = _qrConfig.value
                val qrId: Long = if (_isEditMode.value && _editQrId.value != null) {
                    val existingId = _editQrId.value!!
                    val existing = getQrCodesUseCase.getById(existingId).firstOrNull()
                    if (existing != null) {
                        val updated = existing.copy(qrConfig = config, type = QrType.OLUSTURULAN)
                        updateQrCodeUseCase(updated)
                        existingId
                    } else {
                        saveQrCodeUseCase(QrCode(content = content, type = QrType.OLUSTURULAN, qrConfig = config))
                    }
                } else {
                    saveQrCodeUseCase(QrCode(content = content, type = QrType.OLUSTURULAN, qrConfig = config))
                }

                // Export to external files in chosen format
                withContext(Dispatchers.IO) {
                    val matrix = QrMatrixGenerator.generateMatrix(content, config, 0)
                    val exportBmp = QrCanvasRenderer.renderToBitmap(matrix, config, 1024)
                    val exportSvg = QrSvgRenderer.renderToSvg(matrix, config, 1024)
                    val title = "QR_${System.currentTimeMillis()}"
                    QrExporter.saveToStorage(
                        context = context,
                        bitmap = exportBmp,
                        svgString = exportSvg,
                        format = format,
                        title = title
                    )
                }

                _events.emit(qrId)
                onSuccess?.invoke(qrId)
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Kayıt işlemi başarısız"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun addCustomColor(hex: String) {
        viewModelScope.launch {
            if (hex.matches(Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$"))) {
                settingsUseCases.addCustomColor(hex)
            }
        }
    }

    fun removeCustomColor(hex: String) {
        viewModelScope.launch {
            settingsUseCases.removeCustomColor(hex)
        }
    }

    fun testReadability() {
        viewModelScope.launch {
            _readabilityResult.value = ReadabilityResult.Testing
            val content = buildCurrentPayload()
            // Dedicated bitmap: the live preview buffers are reused and may be overwritten during analysis
            val bmp: Bitmap? = if (content.isNotBlank()) {
                try {
                    withContext(Dispatchers.Default) {
                        val config = _qrConfig.value
                        val matrix = QrMatrixGenerator.generateMatrix(content, config, 0)
                        QrCanvasRenderer.renderToBitmap(matrix, config, 512)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            } else null
            if (bmp != null) {
                _readabilityResult.value = withContext(Dispatchers.Default) { QrReadabilityAnalyzer.analyzeBitmap(bmp) }
            } else {
                _readabilityResult.value = ReadabilityResult.Warning("Önizleme hazır değil.")
            }
        }
    }

    fun buildCurrentPayload(): String {
        return when (_payloadType.value) {
            QrPayloadType.URL -> _urlText.value.trim()
            QrPayloadType.WIFI -> "WIFI:S:${_wifiSsid.value};T:${_wifiSecurity.value};P:${_wifiPassword.value};;"
            QrPayloadType.CONTACT -> "BEGIN:VCARD\nVERSION:3.0\nFN:${_contactName.value}\nTEL:${_contactPhone.value}\nEMAIL:${_contactEmail.value}\nORG:${_contactOrg.value}\nEND:VCARD"
            QrPayloadType.EMAIL -> "mailto:${_emailTo.value}?subject=${Uri.encode(_emailSubject.value)}&body=${Uri.encode(_emailBody.value)}"
            else -> _textContent.value
        }
    }

    fun isLinkModified(): Boolean {
        if (!_isEditMode.value || initialContent == null) return false
        return buildCurrentPayload() != initialContent
    }

    private suspend fun renderPreviewInternal() {
        val content = buildCurrentPayload()
        if (content.isBlank()) return

        if (_readabilityResult.value !is ReadabilityResult.Idle) {
            _readabilityResult.value = ReadabilityResult.Idle
        }
        val config = _qrConfig.value
        val previewKey = "preview_${content}_${config.hashCode()}_384"
        val cached = QrBitmapCache.get(previewKey)
        if (cached != null) {
            _previewBitmap.value = cached
            return
        }

        try {
            val matrix = if (content == cachedContent && config.errorCorrectionLevel == cachedEcLevel && cachedMatrix != null) {
                cachedMatrix!!
            } else {
                val m = QrMatrixGenerator.generateMatrix(content, config, 0)
                cachedMatrix = m
                cachedContent = content
                cachedEcLevel = config.errorCorrectionLevel
                m
            }

            // Zero heap allocation ping-pong double buffering
            val targetBuf = if (useBufferA) bufferB else bufferA
            val rendered = QrCanvasRenderer.renderToBitmap(matrix, config, 384, targetBuf)
            if (useBufferA) {
                bufferB = rendered
            } else {
                bufferA = rendered
            }
            useBufferA = !useBufferA
            QrBitmapCache.put(previewKey, rendered)
            _previewBitmap.value = rendered
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun schedulePreview(immediate: Boolean) {
        if (immediate) {
            debounceJob?.cancel()
            previewChannel.trySend(Unit)
        } else {
            debounceJob?.cancel()
            debounceJob = viewModelScope.launch(Dispatchers.Default) {
                delay(65) // Smooth 65ms debounce for sliders & typing
                previewChannel.trySend(Unit)
            }
        }
    }

    fun saveOrUpdateQrCode(onRequireLinkWarning: () -> Unit) {
        val content = buildCurrentPayload()
        if (content.isBlank()) {
            _error.value = ERROR_CONTENT_EMPTY
            return
        }

        if (_isEditMode.value && isLinkModified()) {
            // User altered the link/payload of an existing QR!
            onRequireLinkWarning()
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                if (_isEditMode.value && _editQrId.value != null) {
                    val existingId = _editQrId.value!!
                    val existing = getQrCodesUseCase.getById(existingId).firstOrNull()
                    if (existing != null) {
                        val updated = existing.copy(qrConfig = _qrConfig.value, type = QrType.OLUSTURULAN)
                        updateQrCodeUseCase(updated)
                        _events.emit(existingId)
                        return@launch
                    }
                }

                // New QR
                val qr = QrCode(
                    content = content,
                    type = QrType.OLUSTURULAN,
                    qrConfig = _qrConfig.value
                )
                val id = saveQrCodeUseCase(qr)
                _events.emit(id)
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "QR kaydedilemedi"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun confirmLinkChangeAndSave() {
        val content = buildCurrentPayload()
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val qr = QrCode(
                    content = content,
                    type = QrType.OLUSTURULAN,
                    qrConfig = _qrConfig.value
                )
                val id = saveQrCodeUseCase(qr)
                _events.emit(id)
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Yeni QR kaydedilemedi"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        previewChannel.close()
        // Not recycled on purpose: Compose may still draw them during the exit animation.
        bufferA = null
        bufferB = null
        LogoBitmapCache.clear()
    }

    companion object {
        const val ERROR_CONTENT_EMPTY = "ERROR_CONTENT_EMPTY"
    }
}
