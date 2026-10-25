package com.example.offlineqr.ui.create

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.ShapeLine
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offlineqr.R
import com.example.offlineqr.domain.model.ErrorCorrectionLevel
import com.example.offlineqr.domain.model.FinderShape
import com.example.offlineqr.domain.model.GradientDirection
import com.example.offlineqr.domain.model.GradientType
import com.example.offlineqr.domain.model.ModuleShape
import com.example.offlineqr.domain.model.QrFrameStyle
import com.example.offlineqr.domain.model.QrPayloadType
import com.example.offlineqr.domain.model.QrTemplate
import com.example.offlineqr.qr.engine.ReadabilityResult
import com.example.offlineqr.qr.engine.TemplateThumbnailCache
import com.example.offlineqr.qr.export.ExportFormat
import com.example.offlineqr.qr.export.QrExporter
import com.example.offlineqr.ui.components.ColorSpectrumDialog
import com.example.offlineqr.ui.components.DeleteColorConfirmDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CreateQrScreen(
    viewModel: CreateQrViewModel,
    onCloseClick: () -> Unit,
    onSaveSuccess: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showShapesSection by remember { mutableStateOf(true) }
    var showColorsSection by remember { mutableStateOf(true) }
    var showFrameSection by remember { mutableStateOf(false) }
    var showSizingSection by remember { mutableStateOf(true) }
    var showLogoSection by remember { mutableStateOf(false) }

    // Color Spectrum Dialog state
    var showSpectrumDialog by remember { mutableStateOf(false) }
    var spectrumTargetChannel by remember { mutableStateOf("FG_PRIMARY") } // FG_PRIMARY, FG_SECONDARY, BG_PRIMARY, BG_SECONDARY
    var spectrumInitialColor by remember { mutableStateOf(0xFF00677DL) }
    var spectrumTitle by remember { mutableStateOf("Renk Seçimi") }

    // Custom color deletion dialog
    var colorToDelete by remember { mutableStateOf<String?>(null) }

    // Design templates state
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var newTemplateName by remember { mutableStateOf("") }
    var templateToDelete by remember { mutableStateOf<QrTemplate?>(null) }
    var isDeleteTemplateModeActive by remember { mutableStateOf(false) }

    // Readability check state
    var showReadabilityWarningDialog by remember { mutableStateOf(false) }
    var readabilityWarningMessage by remember { mutableStateOf("") }

    // 5-second link warning dialog in edit mode
    var showLinkWarningDialog by remember { mutableStateOf(false) }
    var warningCountdown by remember { mutableIntStateOf(5) }

    var showExportFormatDialog by remember { mutableStateOf(false) }
    var isExportForShare by remember { mutableStateOf(false) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.setLogo(uri, context)
    }

    LaunchedEffect(showLinkWarningDialog) {
        if (showLinkWarningDialog) {
            warningCountdown = 5
            while (warningCountdown > 0) {
                delay(1000)
                warningCountdown--
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { qrId ->
            onSaveSuccess(qrId)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { err ->
            val msg = if (err == CreateQrViewModel.ERROR_CONTENT_EMPTY) context.getString(R.string.error_content_empty) else err
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            // Sticky Bottom Action Bar: ONLY KAYDET (Save) - bi qrı kaydetmeden indirme ve paylaşma olmasın
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f))
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Button(
                    onClick = {
                        val payload = viewModel.buildCurrentPayload()
                        if (payload.isBlank()) {
                            Toast.makeText(context, context.getString(R.string.error_content_empty), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (uiState.isEditMode && viewModel.isLinkModified()) {
                            showLinkWarningDialog = true
                            return@Button
                        }
                        scope.launch {
                            val scannability = viewModel.verifyScannability()
                            if (scannability is ReadabilityResult.Warning) {
                                readabilityWarningMessage = scannability.message
                                showReadabilityWarningDialog = true
                            } else {
                                showExportFormatDialog = true
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.save_btn),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Bar (Pinned)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCloseClick) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                }

                Text(
                    text = if (uiState.isEditMode) stringResource(R.string.edit_qr_title) else stringResource(R.string.create_qr_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                FilledTonalButton(
                    onClick = {
                        newTemplateName = ""
                        showSaveTemplateDialog = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.save_as_template),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // Live Preview Card (PINNED STICKY TOP - ALWAYS VISIBLE!)
            LiveQrPreviewCard(viewModel = viewModel)

            // Scrollable Configuration List (Scrolls below the pinned preview)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp)
            ) {
                // Direct Error Correction Level Selector (L, M, Q, H)
                ErrorCorrectionSelector(
                    errorCorrectionLevel = uiState.qrConfig.errorCorrectionLevel,
                    onLevelSelected = { viewModel.updateErrorCorrection(it) }
                )

            // Payload Standard Category Tabs
            PayloadTypeSelector(
                selectedType = uiState.payloadType,
                onTypeSelected = { viewModel.setPayloadType(it) }
            )

            // Payload Content Inputs
            PayloadInputFields(
                payloadType = uiState.payloadType,
                urlText = uiState.urlText,
                onUrlChanged = { viewModel.onUrlChanged(it) },
                wifiSsid = uiState.wifiSsid,
                wifiPassword = uiState.wifiPassword,
                wifiSecurity = uiState.wifiSecurity,
                onWifiChanged = { s, p, sec -> viewModel.onWifiChanged(s, p, sec) },
                contactName = uiState.contactName,
                contactPhone = uiState.contactPhone,
                contactEmail = uiState.contactEmail,
                contactOrg = uiState.contactOrg,
                onContactChanged = { n, ph, e, o -> viewModel.onContactChanged(n, ph, e, o) },
                emailTo = uiState.emailTo,
                emailSubject = uiState.emailSubject,
                emailBody = uiState.emailBody,
                onEmailChanged = { t, s, b -> viewModel.onEmailChanged(t, s, b) },
                textContent = uiState.textContent,
                onTextChanged = { viewModel.onTextChanged(it) }
            )

            // Design Templates Carousel
            TemplatesSection(
                templates = uiState.templates,
                isDeleteModeActive = isDeleteTemplateModeActive,
                onToggleDeleteMode = { isDeleteTemplateModeActive = !isDeleteTemplateModeActive },
                onSelectTemplate = { viewModel.applyTemplate(it) },
                onRequestDeleteTemplate = { templateToDelete = it },
                onSaveNewTemplate = {
                    newTemplateName = ""
                    showSaveTemplateDialog = true
                }
            )

            // Accordion Sections ("Vector Styling Engine")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.vector_styling_engine),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                ShapesAccordion(
                    isExpanded = showShapesSection,
                    onToggleExpand = { showShapesSection = !showShapesSection },
                    finderShape = uiState.qrConfig.finderShape,
                    moduleShape = uiState.qrConfig.moduleShape,
                    onFinderShapeSelected = { viewModel.updateFinderShape(it) },
                    onModuleShapeSelected = { viewModel.updateModuleShape(it) }
                )

                ColorsAccordion(
                    isExpanded = showColorsSection,
                    onToggleExpand = { showColorsSection = !showColorsSection },
                    foregroundColor = uiState.qrConfig.foregroundColor,
                    secondaryColor = uiState.qrConfig.secondaryColor,
                    gradientType = uiState.qrConfig.gradientType,
                    gradientDirection = uiState.qrConfig.gradientDirection,
                    backgroundColor = uiState.qrConfig.backgroundColor,
                    backgroundSecondaryColor = uiState.qrConfig.backgroundSecondaryColor,
                    backgroundGradientType = uiState.qrConfig.backgroundGradientType,
                    backgroundGradientDirection = uiState.qrConfig.backgroundGradientDirection,
                    customColors = uiState.customColors,
                    onUpdateForeground = { p, s, gt -> viewModel.updateForeground(p, s, gt) },
                    onUpdateBackground = { p, s, gt -> viewModel.updateBackground(p, s, gt) },
                    onUpdateForegroundDirection = { viewModel.updateForegroundDirection(it) },
                    onUpdateBackgroundDirection = { viewModel.updateBackgroundDirection(it) },
                    onOpenSpectrum = { channel, initCol, title ->
                        spectrumTargetChannel = channel
                        spectrumInitialColor = initCol
                        spectrumTitle = title
                        showSpectrumDialog = true
                    },
                    onRequestDeleteColor = { colorToDelete = it }
                )

                    FrameAccordion(
                        isExpanded = showFrameSection,
                        onToggleExpand = { showFrameSection = !showFrameSection },
                        frameStyle = uiState.qrConfig.frameStyle,
                        frameText = uiState.qrConfig.frameText,
                        frameColor = uiState.qrConfig.frameColor,
                        frameTextColor = uiState.qrConfig.frameTextColor,
                        qrForegroundColor = uiState.qrConfig.foregroundColor,
                        customColors = uiState.customColors,
                        onSelectFrameStyle = { viewModel.updateFrameStyle(it) },
                        onFrameTextChanged = { viewModel.updateFrameText(it) },
                        onSelectFrameColor = { viewModel.updateFrameColor(it) },
                        onSelectFrameTextColor = { viewModel.updateFrameTextColor(it) },
                        onOpenSpectrum = { channel, initCol, title ->
                            spectrumTargetChannel = channel
                            spectrumInitialColor = initCol
                            spectrumTitle = title
                            showSpectrumDialog = true
                        },
                        onRequestDeleteColor = { colorToDelete = it }
                    )

                    SizingAccordion(
                        isExpanded = showSizingSection,
                        onToggleExpand = { showSizingSection = !showSizingSection },
                        qrScale = uiState.qrConfig.qrScale,
                        quietZone = uiState.qrConfig.quietZone,
                        backgroundCornerRadius = uiState.qrConfig.backgroundCornerRadius,
                        onQrScaleChanged = { viewModel.updateQrScale(it) },
                        onQuietZoneChanged = { viewModel.updateQuietZone(it) },
                        onBackgroundCornerRadiusChanged = { viewModel.updateBackgroundCornerRadius(it) }
                    )

                    val currentEc = remember(uiState.qrConfig.errorCorrectionLevel) {
                        ErrorCorrectionLevel.fromString(uiState.qrConfig.errorCorrectionLevel)
                    }
                    val maxAllowedScale = remember(currentEc) {
                        viewModel.getMaxAllowedLogoScale(currentEc)
                    }

                    LogoAccordion(
                        isExpanded = showLogoSection,
                        onToggleExpand = { showLogoSection = !showLogoSection },
                        logoPath = uiState.qrConfig.logoPath,
                        logoScale = uiState.qrConfig.logoScale,
                        maxAllowedScale = maxAllowedScale,
                        logoCornerRadius = uiState.qrConfig.logoCornerRadius,
                        logoCutoutFillBackground = uiState.qrConfig.logoCutoutFillBackground,
                        onPickLogo = { logoPickerLauncher.launch("image/*") },
                        onRemoveLogo = { viewModel.removeLogo() },
                        onLogoScaleChanged = { viewModel.updateLogoScale(it) },
                        onLogoCornerRadiusChanged = { viewModel.updateLogoCornerRadius(it) },
                        onLogoCutoutFillBackgroundChanged = { viewModel.updateLogoCutoutFillBackground(it) }
                    )
            }
        }
    }
}

    // Color Spectrum Dialog
    if (showSpectrumDialog) {
        ColorSpectrumDialog(
            initialColor = spectrumInitialColor,
            title = spectrumTitle,
            onColorSelected = { cLong, hex ->
                showSpectrumDialog = false
                when (spectrumTargetChannel) {
                    "FG_PRIMARY" -> viewModel.updateForeground(
                        primary = cLong,
                        secondary = uiState.qrConfig.secondaryColor,
                        gradientType = GradientType.fromString(uiState.qrConfig.gradientType)
                    )
                    "FG_SECONDARY" -> viewModel.updateForeground(
                        primary = uiState.qrConfig.foregroundColor,
                        secondary = cLong,
                        gradientType = GradientType.fromString(uiState.qrConfig.gradientType)
                    )
                    "BG_PRIMARY" -> viewModel.updateBackground(
                        primary = cLong,
                        secondary = uiState.qrConfig.backgroundSecondaryColor,
                        gradientType = GradientType.fromString(uiState.qrConfig.backgroundGradientType)
                    )
                    "BG_SECONDARY" -> viewModel.updateBackground(
                        primary = uiState.qrConfig.backgroundColor,
                        secondary = cLong,
                        gradientType = GradientType.fromString(uiState.qrConfig.backgroundGradientType)
                    )
                    "FRAME_COLOR" -> viewModel.updateFrameColor(cLong)
                    "FRAME_TEXT_COLOR" -> viewModel.updateFrameTextColor(cLong)
                }
            },
            onSaveToCustomPalette = { hex ->
                viewModel.addCustomColor(hex)
            },
            onDismissRequest = { showSpectrumDialog = false }
        )
    }

    // Custom Color Deletion Confirmation Dialog
    if (colorToDelete != null) {
        DeleteColorConfirmDialog(
            hexColor = colorToDelete!!,
            onConfirm = {
                viewModel.removeCustomColor(colorToDelete!!)
                colorToDelete = null
            },
            onDismissRequest = { colorToDelete = null }
        )
    }

    // 5-Second Warning Dialog for Link Modification in Edit Mode
    if (showLinkWarningDialog) {
        AlertDialog(
            onDismissRequest = { showLinkWarningDialog = false },
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
            },
            title = {
                Text(stringResource(R.string.alert_link_change_title), fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.alert_link_change_msg),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (warningCountdown > 0) {
                        Text(
                            stringResource(R.string.link_warning_countdown, warningCountdown),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLinkWarningDialog = false
                        scope.launch {
                            val scannability = viewModel.verifyScannability()
                            if (scannability is ReadabilityResult.Warning) {
                                readabilityWarningMessage = scannability.message
                                showReadabilityWarningDialog = true
                            } else {
                                showExportFormatDialog = true
                            }
                        }
                    },
                    enabled = warningCountdown == 0
                ) {
                    Text(if (warningCountdown > 0) stringResource(R.string.link_warning_wait, warningCountdown) else stringResource(R.string.link_warning_understand))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkWarningDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Scannability / Readability Warning Dialog
    if (showReadabilityWarningDialog) {
        AlertDialog(
            onDismissRequest = { showReadabilityWarningDialog = false },
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
            },
            title = {
                Text(stringResource(R.string.readability_warning_dialog_title), fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = if (readabilityWarningMessage.isNotBlank()) {
                        readabilityWarningMessage + "\n\n" + stringResource(R.string.readability_warning_dialog_msg)
                    } else {
                        stringResource(R.string.readability_warning_dialog_msg)
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReadabilityWarningDialog = false
                        showExportFormatDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.continue_anyway))
                }
            },
            dismissButton = {
                TextButton(onClick = { showReadabilityWarningDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Export Format Chooser Dialog (In-App Only, PNG, JPG, SVG)
    if (showExportFormatDialog) {
        AlertDialog(
            onDismissRequest = { showExportFormatDialog = false },
            title = {
                Text(
                    stringResource(R.string.export_format_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Sadece Uygulama İçi Kaydet (Dışarı aktarmadan kaydet)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable {
                                showExportFormatDialog = false
                                Toast.makeText(context, context.getString(R.string.saved_in_app_only_toast), Toast.LENGTH_SHORT).show()
                                viewModel.saveInAppOnly()
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = stringResource(R.string.save_in_app_only),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = stringResource(R.string.save_in_app_only_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    val formats = listOf(
                        Triple(ExportFormat.PNG, stringResource(R.string.format_png_desc), Icons.Default.FileDownload),
                        Triple(ExportFormat.JPG, stringResource(R.string.format_jpg_desc), Icons.Default.FileDownload),
                        Triple(ExportFormat.SVG, stringResource(R.string.format_svg_desc), Icons.Default.ShapeLine)
                    )

                    formats.forEach { (fmt, desc, icon) ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable {
                                    showExportFormatDialog = false
                                    viewModel.saveAndExport(fmt, context)
                                }
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = fmt.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportFormatDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Save as Custom Template Dialog
    if (showSaveTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showSaveTemplateDialog = false },
            icon = {
                Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            },
            title = {
                Text(stringResource(R.string.save_as_template), fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.save_as_template_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newTemplateName,
                        onValueChange = { newTemplateName = it },
                        label = { Text(stringResource(R.string.template_name_label)) },
                        placeholder = { Text(stringResource(R.string.template_name_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newTemplateName.trim().ifBlank { context.getString(R.string.template_name_placeholder) }
                        viewModel.saveCurrentAsTemplate(name)
                        showSaveTemplateDialog = false
                        Toast.makeText(context, context.getString(R.string.template_saved_toast), Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(stringResource(R.string.save_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveTemplateDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Custom Template Deletion Confirmation Dialog
    if (templateToDelete != null) {
        AlertDialog(
            onDismissRequest = { templateToDelete = null },
            icon = {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(28.dp))
            },
            title = {
                Text(stringResource(R.string.delete_template_title), fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    stringResource(R.string.delete_template_confirm, templateToDelete!!.name),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeCustomTemplate(templateToDelete!!.id)
                        templateToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { templateToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun ColorSelectionRow(
    title: String,
    currentColorLong: Long,
    customColors: List<String>,
    isBackgroundDefault: Boolean = false,
    onSelectColor: (Long) -> Unit,
    onOpenSpectrum: () -> Unit,
    onRequestDeleteColor: (String) -> Unit
) {
    val presets = if (isBackgroundDefault) {
        listOf("#FFFFFF", "#F8FAFC", "#F1F5F9", "#E2E8F0", "#121212", "#000000")
    } else {
        listOf("#00677D", "#1976D2", "#388E3C", "#7B1FA2", "#D97706", "#E11D48", "#000000")
    }
    var isDeleteModeActive by remember { mutableStateOf(false) }
    val filteredCustom = remember(customColors, presets) { customColors.filter { !presets.contains(it) } }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (filteredCustom.isNotEmpty()) {
                    Text(
                        text = if (isDeleteModeActive) stringResource(R.string.cancel) else stringResource(R.string.delete_colors_btn),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDeleteModeActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        fontWeight = if (isDeleteModeActive) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.clickable { isDeleteModeActive = !isDeleteModeActive }
                    )
                }
                Text(
                    text = stringResource(R.string.color_spectrum_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onOpenSpectrum)
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Pinned Palette + Add icon button (NO TEXT as requested)
            item {
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(onClick = onOpenSpectrum)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = stringResource(R.string.add_custom_color),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Standard System Presets (Never Deletable)
            items(presets) { hex ->
                val parsedColor = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Black }
                val isCur = currentColorLong == (parsedColor.value.toLong() shr 32)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(parsedColor)
                        .border(if (isCur) 2.5.dp else 1.dp, if (isCur) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clickable { onSelectColor(parsedColor.value.toLong() shr 32) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isCur) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = if (hex == "#FFFFFF") Color.Black else Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // User Custom Colors (Only non-system custom colors)
            items(filteredCustom) { hex ->
                val parsedColor = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Gray }
                val isCur = currentColorLong == (parsedColor.value.toLong() shr 32)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(parsedColor)
                        .border(if (isCur) 2.5.dp else 1.5.dp, if (isCur) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary, CircleShape)
                        .clickable {
                            if (isDeleteModeActive) {
                                onRequestDeleteColor(hex)
                            } else {
                                onSelectColor(parsedColor.value.toLong() shr 32)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isCur && !isDeleteModeActive) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    // Delete badge only visible when user deliberately taps "Renk Sil"
                    if (isDeleteModeActive) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Sil", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChunkyModernSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    modifier: Modifier = Modifier
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(36.dp),
        colors = SliderDefaults.colors(
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            thumbColor = MaterialTheme.colorScheme.primary
        ),
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                drawStopIndicator = null,
                modifier = Modifier
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )
        },
        thumb = {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    )
}

@Composable
private fun LiveQrPreviewCard(
    viewModel: CreateQrViewModel,
    modifier: Modifier = Modifier
) {
    val previewBitmap by viewModel.previewBitmap.collectAsState()
    val readabilityResult by viewModel.readabilityResult.collectAsState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                val imgBitmap = remember(previewBitmap) { previewBitmap?.asImageBitmap() }
                if (imgBitmap != null) {
                    Image(
                        bitmap = imgBitmap,
                        contentDescription = "QR Code Preview",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp)
                    )
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }

            // Scannability Test Strip (UNLIMITED TESTING & RETESTING)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (readabilityResult) {
                    is ReadabilityResult.Success -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                            Text(
                                text = stringResource(R.string.readability_optimal),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    is ReadabilityResult.Warning -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                            Text(
                                text = stringResource(R.string.readability_warning_short),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    is ReadabilityResult.Testing -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Text(
                                text = stringResource(R.string.readability_testing),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    ReadabilityResult.Idle -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                            Text(
                                text = stringResource(R.string.readability_not_tested),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Unlimited Test / Retest button
                FilledTonalButton(
                    onClick = { viewModel.testReadability() },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(30.dp),
                    enabled = readabilityResult !is ReadabilityResult.Testing && previewBitmap != null
                ) {
                    Icon(
                        if (readabilityResult is ReadabilityResult.Idle) Icons.Default.FactCheck else Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (readabilityResult is ReadabilityResult.Idle) stringResource(R.string.test_readability) else stringResource(R.string.retest_readability),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplatesSection(
    templates: List<QrTemplate>,
    isDeleteModeActive: Boolean,
    onToggleDeleteMode: () -> Unit,
    onSelectTemplate: (QrTemplate) -> Unit,
    onRequestDeleteTemplate: (QrTemplate) -> Unit,
    onSaveNewTemplate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasCustomTemplates = remember(templates) { templates.any { !it.isBuiltIn } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.GridView,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.templates_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasCustomTemplates) {
                    Text(
                        text = if (isDeleteModeActive) stringResource(R.string.cancel) else stringResource(R.string.delete_templates_btn),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDeleteModeActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        fontWeight = if (isDeleteModeActive) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.clickable(onClick = onToggleDeleteMode)
                    )
                }

                Text(
                    text = stringResource(R.string.save_as_template),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onSaveNewTemplate)
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(templates, key = { it.id }) { template ->
                val thumbnailBmp = remember(template.id, template.qrConfig) {
                    TemplateThumbnailCache.getThumbnail(template.id, template.qrConfig)
                }

                Box(
                    modifier = Modifier
                        .width(84.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            if (isDeleteModeActive && !template.isBuiltIn) {
                                onRequestDeleteTemplate(template)
                            } else {
                                onSelectTemplate(template)
                            }
                        }
                        .padding(6.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = thumbnailBmp.asImageBitmap(),
                                contentDescription = template.name,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                            )
                        }

                        Text(
                            text = template.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (isDeleteModeActive && !template.isBuiltIn) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                                .clickable { onRequestDeleteTemplate(template) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorCorrectionSelector(
    errorCorrectionLevel: String,
    onLevelSelected: (ErrorCorrectionLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.error_correction_selector_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Level $errorCorrectionLevel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val levels = listOf(
                Triple(ErrorCorrectionLevel.L, "L (~7%)", stringResource(R.string.ec_level_desc_l)),
                Triple(ErrorCorrectionLevel.M, "M (~15%)", stringResource(R.string.ec_level_desc_m)),
                Triple(ErrorCorrectionLevel.Q, "Q (~25%)", stringResource(R.string.ec_level_desc_q)),
                Triple(ErrorCorrectionLevel.H, "H (~30%)", stringResource(R.string.ec_level_desc_h))
            )

            levels.forEach { (lvl, title, _) ->
                val isSelected = errorCorrectionLevel == lvl.name
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerLow
                        )
                        .border(
                            if (isSelected) 1.5.dp else 0.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onLevelSelected(lvl) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PayloadTypeSelector(
    selectedType: QrPayloadType,
    onTypeSelected: (QrPayloadType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.payload_standard),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )

        val payloadTypes = listOf(
            QrPayloadType.URL to (Icons.Default.Link to R.string.payload_url),
            QrPayloadType.WIFI to (Icons.Default.Wifi to R.string.payload_wifi),
            QrPayloadType.TEXT to (Icons.Default.Notes to R.string.payload_text),
            QrPayloadType.CONTACT to (Icons.Default.ContactPage to R.string.payload_contact),
            QrPayloadType.EMAIL to (Icons.Default.Mail to R.string.payload_email)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(payloadTypes) { (type, iconRes) ->
                val isSelected = selectedType == type
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainer
                        )
                        .clickable { onTypeSelected(type) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            iconRes.first,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(iconRes.second),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PayloadInputFields(
    payloadType: QrPayloadType,
    urlText: String,
    onUrlChanged: (String) -> Unit,
    wifiSsid: String,
    wifiPassword: String,
    wifiSecurity: String,
    onWifiChanged: (String, String, String) -> Unit,
    contactName: String,
    contactPhone: String,
    contactEmail: String,
    contactOrg: String,
    onContactChanged: (String, String, String, String) -> Unit,
    emailTo: String,
    emailSubject: String,
    emailBody: String,
    onEmailChanged: (String, String, String) -> Unit,
    textContent: String,
    onTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (payloadType) {
                QrPayloadType.URL -> {
                    Text(stringResource(R.string.destination_url), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    var tfv by remember(urlText) {
                        mutableStateOf(TextFieldValue(text = urlText, selection = TextRange(urlText.length)))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = tfv,
                            onValueChange = { newTfv ->
                                tfv = newTfv
                                onUrlChanged(newTfv.text)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused && tfv.text.isNotEmpty()) {
                                        // Auto-select entire text so typing replaces it immediately
                                        tfv = tfv.copy(selection = TextRange(0, tfv.text.length))
                                    }
                                },
                            singleLine = true
                        )
                        IconButton(onClick = { onUrlChanged("") }) {
                            Icon(Icons.Default.Cancel, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                QrPayloadType.WIFI -> {
                    Text(stringResource(R.string.payload_wifi), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = wifiSsid,
                        onValueChange = { onWifiChanged(it, wifiPassword, wifiSecurity) },
                        label = { Text(stringResource(R.string.wifi_ssid)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = wifiPassword,
                        onValueChange = { onWifiChanged(wifiSsid, it, wifiSecurity) },
                        label = { Text(stringResource(R.string.wifi_password)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                QrPayloadType.CONTACT -> {
                    Text(stringResource(R.string.payload_contact), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { onContactChanged(it, contactPhone, contactEmail, contactOrg) },
                        label = { Text(stringResource(R.string.contact_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { onContactChanged(contactName, it, contactEmail, contactOrg) },
                        label = { Text(stringResource(R.string.contact_phone)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                QrPayloadType.EMAIL -> {
                    Text(stringResource(R.string.payload_email), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = emailTo,
                        onValueChange = { onEmailChanged(it, emailSubject, emailBody) },
                        label = { Text(stringResource(R.string.contact_email)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = emailSubject,
                        onValueChange = { onEmailChanged(emailTo, it, emailBody) },
                        label = { Text(stringResource(R.string.email_subject)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                else -> {
                    Text(stringResource(R.string.text_content), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = textContent,
                        onValueChange = onTextChanged,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            }
        }
    }
}

@Composable
private fun ShapesAccordion(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    finderShape: String,
    moduleShape: String,
    onFinderShapeSelected: (FinderShape) -> Unit,
    onModuleShapeSelected: (ModuleShape) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.ShapeLine, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.corner_and_dot_shapes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Eye Finder Shapes (8 Diverse Shapes)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.eye_corner_radius), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val finders = listOf(
                            FinderShape.SQUARE to (Icons.Default.CropSquare to R.string.shape_square),
                            FinderShape.ROUNDED to (Icons.Default.RoundedCorner to R.string.shape_rounded),
                            FinderShape.CIRCLE to (Icons.Default.RadioButtonUnchecked to R.string.shape_circle),
                            FinderShape.DOT to (Icons.Default.Grain to R.string.shape_dot),
                            FinderShape.LEAF to (Icons.Default.Eco to R.string.shape_leaf),
                            FinderShape.DIAMOND to (Icons.Default.GridView to R.string.shape_diamond),
                            FinderShape.HEXAGON to (Icons.Default.Hexagon to R.string.shape_hexagon),
                            FinderShape.DOUBLE_CIRCLE to (Icons.Default.RadioButtonUnchecked to R.string.shape_double_circle)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(finders) { (shape, iconRes) ->
                                val isSel = finderShape.equals(shape.name, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSel) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceContainerLow
                                        )
                                        .border(
                                            if (isSel) 1.5.dp else 0.dp,
                                            if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onFinderShapeSelected(shape) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(iconRes.first, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(stringResource(iconRes.second), style = MaterialTheme.typography.labelSmall, color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    // Body Matrix Module Shapes (7 Diverse Shapes)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.matrix_body_modules), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val modules = listOf(
                            ModuleShape.SQUARE to (Icons.Default.GridView to R.string.shape_square),
                            ModuleShape.PILLS to (Icons.Default.ViewStream to R.string.shape_pills),
                            ModuleShape.CIRCLES to (Icons.Default.Grain to R.string.shape_circles),
                            ModuleShape.DIAMOND to (Icons.Default.CropSquare to R.string.shape_diamond),
                            ModuleShape.STAR to (Icons.Default.Star to R.string.shape_star),
                            ModuleShape.HEXAGON to (Icons.Default.Hexagon to R.string.shape_hexagon),
                            ModuleShape.LEAF to (Icons.Default.Eco to R.string.shape_leaf)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(modules) { (shape, iconRes) ->
                                val isSel = moduleShape.equals(shape.name, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSel) MaterialTheme.colorScheme.secondaryContainer
                                            else MaterialTheme.colorScheme.surfaceContainerLow
                                        )
                                        .border(
                                            if (isSel) 1.5.dp else 0.dp,
                                            if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onModuleShapeSelected(shape) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(iconRes.first, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (isSel) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(stringResource(iconRes.second), style = MaterialTheme.typography.labelSmall, color = if (isSel) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorsAccordion(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    foregroundColor: Long,
    secondaryColor: Long?,
    gradientType: String,
    gradientDirection: String = GradientDirection.DIAGONAL.name,
    backgroundColor: Long,
    backgroundSecondaryColor: Long?,
    backgroundGradientType: String,
    backgroundGradientDirection: String = GradientDirection.DIAGONAL.name,
    customColors: List<String>,
    onUpdateForeground: (primary: Long, secondary: Long?, gradientType: GradientType) -> Unit,
    onUpdateBackground: (primary: Long, secondary: Long?, gradientType: GradientType) -> Unit,
    onUpdateForegroundDirection: (GradientDirection) -> Unit = {},
    onUpdateBackgroundDirection: (GradientDirection) -> Unit = {},
    onOpenSpectrum: (channel: String, initialColor: Long, title: String) -> Unit,
    onRequestDeleteColor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.color_palette_contrast), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // A. FOREGROUND / PATTERN SECTION
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.pattern_color), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                        // Gradient style selector (Only 2 options: Düz ve Degrade)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val isFgSolid = gradientType == GradientType.NONE.name
                            val isFgGradient = !isFgSolid

                            // 1. DÜZ (Solid)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isFgSolid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
                                    .clickable {
                                        onUpdateForeground(
                                            foregroundColor,
                                            secondaryColor ?: 0xFF004E5FL,
                                            GradientType.NONE
                                        )
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.bg_solid),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isFgSolid) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isFgSolid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // 2. DEGRADE (Gradient) - 3 noktadan dikey, yatay, çapraz, ters çapraz ve dairesel seçilebilir
                            var fgMenuOpen by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isFgGradient) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
                                    .clickable {
                                        if (!isFgGradient) {
                                            val targetType = if (GradientDirection.fromString(gradientDirection) == GradientDirection.RADIAL) {
                                                GradientType.RADIAL
                                            } else {
                                                GradientType.LINEAR
                                            }
                                            onUpdateForeground(
                                                foregroundColor,
                                                secondaryColor ?: 0xFF004E5FL,
                                                targetType
                                            )
                                        } else {
                                            fgMenuOpen = true
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.gradient_label_general),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isFgGradient) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isFgGradient) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Box {
                                        IconButton(
                                            onClick = {
                                                if (!isFgGradient) {
                                                    val targetType = if (GradientDirection.fromString(gradientDirection) == GradientDirection.RADIAL) {
                                                        GradientType.RADIAL
                                                    } else {
                                                        GradientType.LINEAR
                                                    }
                                                    onUpdateForeground(
                                                        foregroundColor,
                                                        secondaryColor ?: 0xFF004E5FL,
                                                        targetType
                                                    )
                                                }
                                                fgMenuOpen = true
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.MoreVert,
                                                contentDescription = stringResource(R.string.grad_dir_title),
                                                tint = if (isFgGradient) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = fgMenuOpen,
                                            onDismissRequest = { fgMenuOpen = false }
                                        ) {
                                            val currentDir = GradientDirection.fromString(gradientDirection)
                                            val isRadialCurrent = gradientType == GradientType.RADIAL.name || currentDir == GradientDirection.RADIAL

                                            GradientDirection.selectableDirections.forEach { dir ->
                                                val isDirSel = if (dir == GradientDirection.RADIAL) {
                                                    isRadialCurrent
                                                } else if (isRadialCurrent) {
                                                    false
                                                } else when (dir) {
                                                    GradientDirection.VERTICAL -> currentDir == GradientDirection.VERTICAL || currentDir == GradientDirection.TOP_TO_BOTTOM
                                                    GradientDirection.HORIZONTAL -> currentDir == GradientDirection.HORIZONTAL || currentDir == GradientDirection.LEFT_TO_RIGHT
                                                    GradientDirection.REVERSE_DIAGONAL -> currentDir == GradientDirection.REVERSE_DIAGONAL
                                                    GradientDirection.DIAGONAL -> currentDir == GradientDirection.DIAGONAL || (currentDir != GradientDirection.VERTICAL && currentDir != GradientDirection.HORIZONTAL && currentDir != GradientDirection.REVERSE_DIAGONAL && currentDir != GradientDirection.TOP_TO_BOTTOM && currentDir != GradientDirection.LEFT_TO_RIGHT)
                                                    else -> currentDir == dir
                                                }

                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = stringResource(dir.getLabelResId()),
                                                            fontWeight = if (isDirSel) FontWeight.Bold else FontWeight.Normal,
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    },
                                                    onClick = {
                                                        onUpdateForegroundDirection(dir)
                                                        fgMenuOpen = false
                                                    },
                                                    trailingIcon = if (isDirSel) {
                                                        {
                                                            Icon(
                                                                Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    } else null
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        val isFgGradient = gradientType != GradientType.NONE.name

                        // Foreground Color 1
                        ColorSelectionRow(
                            title = if (isFgGradient) "1. Renk (Başlangıç)" else "Desen Rengi",
                            currentColorLong = foregroundColor,
                            customColors = customColors,
                            onSelectColor = { cLong ->
                                onUpdateForeground(
                                    cLong,
                                    secondaryColor,
                                    GradientType.fromString(gradientType)
                                )
                            },
                            onOpenSpectrum = {
                                onOpenSpectrum("FG_PRIMARY", foregroundColor, if (isFgGradient) "Desen 1. Renk Spektrumu" else "Desen Rengi Spektrumu")
                            },
                            onRequestDeleteColor = onRequestDeleteColor
                        )

                        // Foreground Color 2 (When gradient is selected)
                        if (isFgGradient) {
                            ColorSelectionRow(
                                title = "2. Renk (Bitiş)",
                                currentColorLong = secondaryColor ?: 0xFF004E5FL,
                                customColors = customColors,
                                onSelectColor = { cLong ->
                                    onUpdateForeground(
                                        foregroundColor,
                                        cLong,
                                        GradientType.fromString(gradientType)
                                    )
                                },
                                onOpenSpectrum = {
                                    onOpenSpectrum("FG_SECONDARY", secondaryColor ?: 0xFF004E5FL, "Desen 2. Renk (Bitiş) Spektrumu")
                                },
                                onRequestDeleteColor = onRequestDeleteColor
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // B. BACKGROUND SECTION
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.background_color), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                        // Gradient style selector for background (Only 2 options: Düz ve Degrade)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val isBgSolid = backgroundGradientType == GradientType.NONE.name
                            val isBgGradient = !isBgSolid

                            // 1. DÜZ (Solid)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isBgSolid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
                                    .clickable {
                                        onUpdateBackground(
                                            backgroundColor,
                                            backgroundSecondaryColor ?: 0xFFF1F5F9L,
                                            GradientType.NONE
                                        )
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.bg_solid),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isBgSolid) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isBgSolid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // 2. DEGRADE (Gradient) - 3 noktadan dikey, yatay, çapraz, ters çapraz ve dairesel seçilebilir
                            var bgMenuOpen by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isBgGradient) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
                                    .clickable {
                                        if (!isBgGradient) {
                                            val targetType = if (GradientDirection.fromString(backgroundGradientDirection) == GradientDirection.RADIAL) {
                                                GradientType.RADIAL
                                            } else {
                                                GradientType.LINEAR
                                            }
                                            onUpdateBackground(
                                                backgroundColor,
                                                backgroundSecondaryColor ?: 0xFFF1F5F9L,
                                                targetType
                                            )
                                        } else {
                                            bgMenuOpen = true
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.gradient_label_general),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isBgGradient) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isBgGradient) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Box {
                                        IconButton(
                                            onClick = {
                                                if (!isBgGradient) {
                                                    val targetType = if (GradientDirection.fromString(backgroundGradientDirection) == GradientDirection.RADIAL) {
                                                        GradientType.RADIAL
                                                    } else {
                                                        GradientType.LINEAR
                                                    }
                                                    onUpdateBackground(
                                                        backgroundColor,
                                                        backgroundSecondaryColor ?: 0xFFF1F5F9L,
                                                        targetType
                                                    )
                                                }
                                                bgMenuOpen = true
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.MoreVert,
                                                contentDescription = stringResource(R.string.grad_dir_title),
                                                tint = if (isBgGradient) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = bgMenuOpen,
                                            onDismissRequest = { bgMenuOpen = false }
                                        ) {
                                            val currentBgDir = GradientDirection.fromString(backgroundGradientDirection)
                                            val isRadialCurrent = backgroundGradientType == GradientType.RADIAL.name || currentBgDir == GradientDirection.RADIAL

                                            GradientDirection.selectableDirections.forEach { dir ->
                                                val isDirSel = if (dir == GradientDirection.RADIAL) {
                                                    isRadialCurrent
                                                } else if (isRadialCurrent) {
                                                    false
                                                } else when (dir) {
                                                    GradientDirection.VERTICAL -> currentBgDir == GradientDirection.VERTICAL || currentBgDir == GradientDirection.TOP_TO_BOTTOM
                                                    GradientDirection.HORIZONTAL -> currentBgDir == GradientDirection.HORIZONTAL || currentBgDir == GradientDirection.LEFT_TO_RIGHT
                                                    GradientDirection.REVERSE_DIAGONAL -> currentBgDir == GradientDirection.REVERSE_DIAGONAL
                                                    GradientDirection.DIAGONAL -> currentBgDir == GradientDirection.DIAGONAL || (currentBgDir != GradientDirection.VERTICAL && currentBgDir != GradientDirection.HORIZONTAL && currentBgDir != GradientDirection.REVERSE_DIAGONAL && currentBgDir != GradientDirection.TOP_TO_BOTTOM && currentBgDir != GradientDirection.LEFT_TO_RIGHT)
                                                    else -> currentBgDir == dir
                                                }

                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = stringResource(dir.getLabelResId()),
                                                            fontWeight = if (isDirSel) FontWeight.Bold else FontWeight.Normal,
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    },
                                                    onClick = {
                                                        onUpdateBackgroundDirection(dir)
                                                        bgMenuOpen = false
                                                    },
                                                    trailingIcon = if (isDirSel) {
                                                        {
                                                            Icon(
                                                                Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    } else null
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        val isBgGradient = backgroundGradientType != GradientType.NONE.name

                        // Background Color 1
                        ColorSelectionRow(
                            title = if (isBgGradient) "1. Renk (Başlangıç)" else "Arka Plan Rengi",
                            currentColorLong = backgroundColor,
                            customColors = customColors,
                            isBackgroundDefault = true,
                            onSelectColor = { cLong ->
                                onUpdateBackground(
                                    cLong,
                                    backgroundSecondaryColor,
                                    GradientType.fromString(backgroundGradientType)
                                )
                            },
                            onOpenSpectrum = {
                                onOpenSpectrum("BG_PRIMARY", backgroundColor, if (isBgGradient) "Arka Plan 1. Renk Spektrumu" else "Arka Plan Rengi Spektrumu")
                            },
                            onRequestDeleteColor = onRequestDeleteColor
                        )

                        // Background Color 2 (When gradient is selected)
                        if (isBgGradient) {
                            ColorSelectionRow(
                                title = "2. Renk (Bitiş)",
                                currentColorLong = backgroundSecondaryColor ?: 0xFFF1F5F9L,
                                customColors = customColors,
                                isBackgroundDefault = true,
                                onSelectColor = { cLong ->
                                    onUpdateBackground(
                                        backgroundColor,
                                        cLong,
                                        GradientType.fromString(backgroundGradientType)
                                    )
                                },
                                onOpenSpectrum = {
                                    onOpenSpectrum("BG_SECONDARY", backgroundSecondaryColor ?: 0xFFF1F5F9L, "Arka Plan 2. Renk (Bitiş) Spektrumu")
                                },
                                onRequestDeleteColor = onRequestDeleteColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SizingAccordion(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    qrScale: Float,
    quietZone: Float,
    backgroundCornerRadius: Float,
    onQrScaleChanged: (Float) -> Unit,
    onQuietZoneChanged: (Float) -> Unit,
    onBackgroundCornerRadiusChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.PhotoSizeSelectLarge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.sizing_and_margins), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // QR Matrix Scale Slider
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.qr_scale_title), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "${(qrScale * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        ChunkyModernSlider(
                            value = qrScale,
                            onValueChange = onQrScaleChanged,
                            valueRange = 0.50f..1.0f
                        )
                    }

                    // Background Quiet Zone Margin Slider
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.margin_quiet_zone_title), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                val zoneStr = if (quietZone % 1f == 0f) {
                                    quietZone.toInt().toString()
                                } else {
                                    String.format(java.util.Locale.ROOT, "%.1f", quietZone)
                                }
                                Text(
                                    stringResource(R.string.modules_count, zoneStr),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        ChunkyModernSlider(
                            value = quietZone,
                            onValueChange = onQuietZoneChanged,
                            valueRange = 0f..8f
                        )
                    }

                    // Background Corner Radius Slider (Arka Plan Yuvarlaklığı)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.background_corner_radius_title), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "${(backgroundCornerRadius * 200).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        ChunkyModernSlider(
                            value = backgroundCornerRadius,
                            onValueChange = onBackgroundCornerRadiusChanged,
                            valueRange = 0f..0.5f
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LogoAccordion(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    logoPath: String?,
    logoScale: Float,
    maxAllowedScale: Float,
    logoCornerRadius: Float,
    logoCutoutFillBackground: Boolean,
    onPickLogo: () -> Unit,
    onRemoveLogo: () -> Unit,
    onLogoScaleChanged: (Float) -> Unit,
    onLogoCornerRadiusChanged: (Float) -> Unit,
    onLogoCutoutFillBackgroundChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.center_logo_branding), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (logoPath == null) {
                        Button(
                            onClick = onPickLogo,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.choose_logo_btn))
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.logo_embedded), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            TextButton(onClick = onRemoveLogo) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.remove_logo), color = MaterialTheme.colorScheme.error)
                            }
                        }

                        // Logo Scale
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(R.string.logo_scale_label), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "${(logoScale * 100).toInt()}% (Maks: ${(maxAllowedScale * 100).toInt()}%)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            ChunkyModernSlider(
                                value = logoScale,
                                onValueChange = onLogoScaleChanged,
                                valueRange = 0.08f..maxAllowedScale
                            )
                        }

                        // Logo Corner Radius
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(R.string.logo_corner_radius_title), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "${(logoCornerRadius * 200).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            ChunkyModernSlider(
                                value = logoCornerRadius,
                                onValueChange = onLogoCornerRadiusChanged,
                                valueRange = 0f..0.5f
                            )
                        }

                        // Corner Cutout Style (Transparent vs Background Color)
                        if (logoCornerRadius > 0f) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    stringResource(R.string.logo_cutout_style_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        onClick = { onLogoCutoutFillBackgroundChanged(false) },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (!logoCutoutFillBackground) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            1.dp,
                                            if (!logoCutoutFillBackground) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.logo_cutout_transparent),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (!logoCutoutFillBackground) FontWeight.Bold else FontWeight.Normal,
                                            color = if (!logoCutoutFillBackground) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp)
                                        )
                                    }

                                    Surface(
                                        onClick = { onLogoCutoutFillBackgroundChanged(true) },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (logoCutoutFillBackground) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            1.dp,
                                            if (logoCutoutFillBackground) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.logo_cutout_background),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (logoCutoutFillBackground) FontWeight.Bold else FontWeight.Normal,
                                            color = if (logoCutoutFillBackground) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FrameAccordion(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    frameStyle: String,
    frameText: String,
    frameColor: Long?,
    frameTextColor: Long,
    qrForegroundColor: Long,
    customColors: List<String>,
    onSelectFrameStyle: (QrFrameStyle) -> Unit,
    onFrameTextChanged: (String) -> Unit,
    onSelectFrameColor: (Long?) -> Unit,
    onSelectFrameTextColor: (Long) -> Unit,
    onOpenSpectrum: (targetChannel: String, initialColor: Long, title: String) -> Unit,
    onRequestDeleteColor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentStyle = remember(frameStyle) { QrFrameStyle.fromString(frameStyle) }
    val isEnabled = currentStyle != QrFrameStyle.NONE

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CropSquare,
                        contentDescription = null,
                        tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.frame_and_text),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = if (isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        val badgeLabel = when (currentStyle) {
                            QrFrameStyle.NONE -> stringResource(R.string.frame_style_none)
                            QrFrameStyle.BOTTOM_BADGE -> stringResource(R.string.frame_style_bottom_badge)
                            QrFrameStyle.TOP_BADGE -> stringResource(R.string.frame_style_top_badge)
                            QrFrameStyle.ROUNDED_FRAME -> stringResource(R.string.frame_style_rounded_frame)
                            QrFrameStyle.BOX_BORDER -> stringResource(R.string.frame_style_box_border)
                        }
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Frame Style Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.frame_style_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        val styles = listOf(
                            QrFrameStyle.NONE to stringResource(R.string.frame_style_none),
                            QrFrameStyle.BOTTOM_BADGE to stringResource(R.string.frame_style_bottom_badge),
                            QrFrameStyle.TOP_BADGE to stringResource(R.string.frame_style_top_badge),
                            QrFrameStyle.ROUNDED_FRAME to stringResource(R.string.frame_style_rounded_frame),
                            QrFrameStyle.BOX_BORDER to stringResource(R.string.frame_style_box_border)
                        )

                        val row1 = styles.take(3)
                        val row2 = styles.drop(3)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            row1.forEach { (style, label) ->
                                val isSel = currentStyle == style
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
                                        .clickable { onSelectFrameStyle(style) }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            row2.forEach { (style, label) ->
                                val isSel = currentStyle == style
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
                                        .clickable { onSelectFrameStyle(style) }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    if (isEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // 2. Badge Text Input
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                stringResource(R.string.frame_text_label),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = frameText,
                                onValueChange = { onFrameTextChanged(it.take(30)) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text(stringResource(R.string.frame_text_placeholder)) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                trailingIcon = {
                                    if (frameText.isNotEmpty()) {
                                        IconButton(onClick = { onFrameTextChanged("") }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            )

                            // Quick Suggestions Row
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    stringResource(R.string.frame_quick_presets),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val presets = listOf("SCAN ME", "BENİ TARA", "MENÜ", "WIFI BAĞLAN", "İLETİŞİM", "BİLGİ")
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(presets) { preset ->
                                        val isSel = frameText == preset
                                        Surface(
                                            onClick = { onFrameTextChanged(preset) },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            )
                                        ) {
                                            Text(
                                                text = preset,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // 3. Frame Color
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.frame_color_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                Surface(
                                    onClick = { onSelectFrameColor(null) },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (frameColor == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                                    border = BorderStroke(1.dp, if (frameColor == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = stringResource(R.string.frame_color_match_qr),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (frameColor == null) FontWeight.Bold else FontWeight.Normal,
                                        color = if (frameColor == null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            ColorSelectionRow(
                                title = stringResource(R.string.frame_color_title),
                                currentColorLong = frameColor ?: qrForegroundColor,
                                customColors = customColors,
                                isBackgroundDefault = false,
                                onSelectColor = { onSelectFrameColor(it) },
                                onOpenSpectrum = {
                                    onOpenSpectrum(
                                        "FRAME_COLOR",
                                        frameColor ?: qrForegroundColor,
                                        "Çerçeve Rengi Spektrumu"
                                    )
                                },
                                onRequestDeleteColor = onRequestDeleteColor
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // 4. Text Color
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                stringResource(R.string.frame_text_color_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            ColorSelectionRow(
                                title = stringResource(R.string.frame_text_color_title),
                                currentColorLong = frameTextColor,
                                customColors = customColors,
                                isBackgroundDefault = true,
                                onSelectColor = { onSelectFrameTextColor(it) },
                                onOpenSpectrum = {
                                    onOpenSpectrum(
                                        "FRAME_TEXT_COLOR",
                                        frameTextColor,
                                        "Yazı Rengi Spektrumu"
                                    )
                                },
                                onRequestDeleteColor = onRequestDeleteColor
                            )
                        }
                    }
                }
            }
        }
    }
}

