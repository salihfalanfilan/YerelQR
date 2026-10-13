package com.example.offlineqr.ui.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.offlineqr.R
import com.example.offlineqr.camera.QrScannerAnalyzer
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    onBackClick: () -> Unit,
    onCreateQrClick: () -> Unit,
    onScanSuccess: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isTorchOn by viewModel.isTorchOn.collectAsState()
    val isFrontCamera by viewModel.isFrontCamera.collectAsState()
    val zoomMultiplier by viewModel.zoomMultiplier.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var showPermissionDeniedDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            showPermissionDeniedDialog = true
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.decodeFromGallery(it, context) }
    }

    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var minZoomRatio by remember { mutableStateOf(1.0f) }
    var maxZoomRatio by remember { mutableStateOf(5.0f) }

    LaunchedEffect(cameraInstance) {
        cameraInstance?.cameraInfo?.zoomState?.observe(lifecycleOwner) { state ->
            if (state != null) {
                minZoomRatio = state.minZoomRatio
                maxZoomRatio = state.maxZoomRatio
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        viewModel.events.collect { event ->
            when (event) {
                is ScannerEvent.ScanSuccess -> onScanSuccess(event.qrId)
                is ScannerEvent.Error -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(isTorchOn, cameraInstance) {
        cameraInstance?.cameraControl?.enableTorch(isTorchOn)
    }

    LaunchedEffect(zoomMultiplier, cameraInstance) {
        cameraInstance?.let { cam ->
            try {
                val clamped = zoomMultiplier.coerceIn(minZoomRatio, maxZoomRatio)
                cam.cameraControl.setZoomRatio(clamped)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(isFrontCamera, hasCameraPermission, previewViewRef, cameraProviderRef) {
        val pv = previewViewRef ?: return@LaunchedEffect
        val cp = cameraProviderRef ?: return@LaunchedEffect
        if (!hasCameraPermission) return@LaunchedEffect

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(pv.surfaceProvider)
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, QrScannerAnalyzer { barcode ->
                    viewModel.onBarcodeDetected(barcode, context)
                })
            }

        val cameraSelector = if (isFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        try {
            cp.unbindAll()
            cameraInstance = cp.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            // CameraX Viewfinder
            AndroidView(
                factory = { ctx ->
                    val pv = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    previewViewRef = pv
                    val future = ProcessCameraProvider.getInstance(ctx)
                    future.addListener({
                        cameraProviderRef = future.get()
                    }, ContextCompat.getMainExecutor(ctx))
                    pv
                },
                modifier = Modifier.fillMaxSize()
            )

            // Scrim / Peripheral Vignette & Clear Focus Aperture
            ScannerOverlay(
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top Floating Action Cluster
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 44.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f))
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Controls Cluster
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.toggleTorch() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        if (isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                        contentDescription = "Flashlight",
                        tint = if (isTorchOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.flipCamera() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.FlipCameraAndroid,
                        contentDescription = "Flip Camera",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = "Import Gallery",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Floating Helper Pill (Directly above reticle)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 220.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.scanner_helper_text),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Viewport Bottom Controls HUD
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.95f))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Zoom Multipliers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isWideAngleSupported = minZoomRatio <= 0.65f
                    val zoomLevels = if (isWideAngleSupported) {
                        listOf(0.5f, 1.0f, 2.0f, 5.0f)
                    } else {
                        listOf(1.0f, 2.0f, 3.0f, 5.0f)
                    }
                    zoomLevels.forEach { zoom ->
                        val isSelected = (zoomMultiplier == zoom) || (!isWideAngleSupported && zoomMultiplier < 1.0f && zoom == 1.0f)
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable {
                                    if (zoom == 0.5f && !isWideAngleSupported) {
                                        Toast.makeText(context, context.getString(R.string.error_wide_angle_unsupported), Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.setZoom(zoom)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (zoom == 0.5f) ".5×" else "${zoom.toInt()}×",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Mode Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Auto Scan Chip
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (selectedMode == ScannerMode.AUTO_SCAN) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                            .clickable { viewModel.setMode(ScannerMode.AUTO_SCAN) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.auto_scan),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedMode == ScannerMode.AUTO_SCAN) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Batch Mode Chip
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (selectedMode == ScannerMode.BATCH_MODE) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                            .clickable { viewModel.setMode(ScannerMode.BATCH_MODE) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.batch_mode),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedMode == ScannerMode.BATCH_MODE) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Barcode 1D Chip
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (selectedMode == ScannerMode.BARCODE_1D) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                            .clickable { viewModel.setMode(ScannerMode.BARCODE_1D) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.barcode_1d),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedMode == ScannerMode.BARCODE_1D) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Lower Bar: Create QR + MLKit Ready
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.clickable(onClick = onCreateQrClick),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.AddBox,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.create_qr_fab),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = stringResource(R.string.mlkit_ready),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }

    // Permission Denied Dialog
    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text(stringResource(R.string.camera_permission_title)) },
            text = { Text(stringResource(R.string.camera_permission_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                        showPermissionDeniedDialog = false
                    }
                ) {
                    Text(stringResource(R.string.open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false; onBackClick() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ScannerOverlay(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary

    val infiniteTransition = rememberInfiniteTransition(label = "Laser")
    val laserYRatio by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserMotion"
    )

    Canvas(modifier = modifier) {
        val reticleSize = size.width * 0.68f
        val left = (size.width - reticleSize) / 2f
        val top = (size.height - reticleSize) / 2f - 40.dp.toPx()
        val right = left + reticleSize
        val bottom = top + reticleSize

        val cornerLen = 36.dp.toPx()
        val strokeW = 4.dp.toPx()

        // Corner 1: Top-Left
        drawLine(primaryColor, Offset(left, top), Offset(left + cornerLen, top), strokeW)
        drawLine(primaryColor, Offset(left, top), Offset(left, top + cornerLen), strokeW)

        // Corner 2: Top-Right
        drawLine(primaryColor, Offset(right, top), Offset(right - cornerLen, top), strokeW)
        drawLine(primaryColor, Offset(right, top), Offset(right, top + cornerLen), strokeW)

        // Corner 3: Bottom-Left
        drawLine(primaryColor, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeW)
        drawLine(primaryColor, Offset(left, bottom), Offset(left, bottom - cornerLen), strokeW)

        // Corner 4: Bottom-Right
        drawLine(primaryColor, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeW)
        drawLine(primaryColor, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeW)

        // Optical Center Guide Marks
        val cx = left + reticleSize / 2f
        val cy = top + reticleSize / 2f
        val crosshairLen = 16.dp.toPx()
        drawLine(primaryColor.copy(alpha = 0.4f), Offset(cx - crosshairLen, cy), Offset(cx + crosshairLen, cy), 1.5.dp.toPx())
        drawLine(primaryColor.copy(alpha = 0.4f), Offset(cx, cy - crosshairLen), Offset(cx, cy + crosshairLen), 1.5.dp.toPx())

        // Animated Level Laser Line
        val laserY = top + reticleSize * laserYRatio
        drawLine(
            color = primaryColor.copy(alpha = 0.85f),
            start = Offset(left + 12.dp.toPx(), laserY),
            end = Offset(right - 12.dp.toPx(), laserY),
            strokeWidth = 2.dp.toPx()
        )
    }
}
