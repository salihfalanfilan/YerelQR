package com.example.offlineqr.ui.home

import android.content.ClipData
import android.graphics.Bitmap
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offlineqr.R
import com.example.offlineqr.domain.model.Folder
import com.example.offlineqr.domain.model.QrCode
import com.example.offlineqr.domain.model.QrPayloadType
import com.example.offlineqr.domain.model.QrType
import com.example.offlineqr.domain.usecase.QrFilterTab
import com.example.offlineqr.qr.engine.QrCanvasRenderer
import com.example.offlineqr.qr.engine.QrMatrixGenerator
import com.example.offlineqr.qr.engine.QrThumbnailCache
import com.example.offlineqr.qr.export.QrExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onCreateQrClick: () -> Unit,
    onQrClick: (Long) -> Unit,
    onFolderClick: (Long) -> Unit,
    onEditQrClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var folderNameInput by remember { mutableStateOf("") }

    var qrToDelete by remember { mutableStateOf<QrCode?>(null) }
    var qrToMove by remember { mutableStateOf<QrCode?>(null) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.decodeFromGallery(it, context) }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importBackup(it, context) }
    }

    val createdCodes = remember(uiState.qrCodes) { uiState.qrCodes.filter { it.type == QrType.OLUSTURULAN } }
    val scannedCodes = remember(uiState.qrCodes) { uiState.qrCodes.filter { it.type == QrType.TARANAN } }

    LaunchedEffect(Unit) {
        viewModel.events.collect { qrId ->
            onQrClick(qrId)
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateQrClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                icon = { Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(24.dp)) },
                text = { Text(stringResource(R.string.create_qr_fab), fontWeight = FontWeight.Medium) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Header Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.QrCode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.home_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = stringResource(R.string.home_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = "Galeriden QR Tara",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) {
                            Icon(
                                if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Search Bar (Expanded)
            if (isSearchExpanded) {
                item {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = { Text(stringResource(R.string.search_preferences)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Overview Metrics Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.GridView,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.active_codes, uiState.totalActiveCount),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.active_codes_subtitle, uiState.favoritesCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            FilledTonalButton(
                                onClick = { viewModel.exportBackup(context) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp).widthIn(min = 112.dp)
                            ) {
                                Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.export_btn), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            }

                            FilledTonalButton(
                                onClick = { importBackupLauncher.launch("*/*") },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp).widthIn(min = 112.dp)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.import_btn), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Filter Chips Horizontal Strip
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedTab == QrFilterTab.ALL,
                            onClick = { viewModel.selectTab(QrFilterTab.ALL) },
                            label = { Text(stringResource(R.string.tab_all, uiState.totalActiveCount)) },
                            leadingIcon = if (uiState.selectedTab == QrFilterTab.ALL) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.selectedTab == QrFilterTab.CREATED,
                            onClick = { viewModel.selectTab(QrFilterTab.CREATED) },
                            label = { Text(stringResource(R.string.tab_created, if (uiState.selectedTab == QrFilterTab.ALL) createdCodes.size else uiState.qrCodes.size)) },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.selectedTab == QrFilterTab.SCANNED,
                            onClick = { viewModel.selectTab(QrFilterTab.SCANNED) },
                            label = { Text(stringResource(R.string.tab_scanned, if (uiState.selectedTab == QrFilterTab.ALL) scannedCodes.size else uiState.qrCodes.size)) },
                            leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.selectedTab == QrFilterTab.FAVORITES,
                            onClick = { viewModel.selectTab(QrFilterTab.FAVORITES) },
                            label = { Text(stringResource(R.string.tab_favorites, uiState.favoritesCount)) },
                            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.selectedTab == QrFilterTab.FOLDERS,
                            onClick = { viewModel.selectTab(QrFilterTab.FOLDERS) },
                            label = { Text(stringResource(R.string.tab_folders, uiState.folders.size)) },
                            leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // Organized Folders Horizontal Section
            item {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.organized_folders),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = stringResource(R.string.view_all),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { viewModel.selectTab(QrFilterTab.FOLDERS) }
                        )
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.folders, key = { it.id }) { folder ->
                            FolderCard(
                                folder = folder,
                                onClick = { onFolderClick(folder.id) }
                            )
                        }

                        item {
                            // New Folder Action Card
                            Box(
                                modifier = Modifier
                                    .size(width = 130.dp, height = 110.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
                                    .clickable { showNewFolderDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.CreateNewFolder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        text = stringResource(R.string.new_folder),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.qrCodes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = stringResource(R.string.no_qr_codes_found),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.no_qr_codes_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (uiState.selectedTab == QrFilterTab.ALL) {
                // SECTION 1: SON OLUŞTURULAN QR KODLAR
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.recent_created_qr_codes),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 0.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 7.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${createdCodes.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                if (createdCodes.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.no_created_qr_codes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    items(createdCodes, key = { it.id }) { qrCode ->
                        QrCodeItemCard(
                            qrCode = qrCode,
                            onClick = { onQrClick(qrCode.id) },
                            onFavoriteToggle = { viewModel.toggleFavorite(qrCode.id, qrCode.isFavorite) },
                            onDeleteClick = { qrToDelete = qrCode },
                            onMoveClick = { qrToMove = qrCode },
                            onShareClick = {
                                scope.launch {
                                    val bitmap = withContext(Dispatchers.Default) { QrCanvasRenderer.renderToBitmap(QrMatrixGenerator.generateMatrix(qrCode.content, qrCode.qrConfig, 0), qrCode.qrConfig, 512) }
                                    QrExporter.shareBitmap(context, bitmap, qrCode.content)
                                }
                            },
                            onSaveGalleryClick = {
                                scope.launch {
                                    val bitmap = withContext(Dispatchers.Default) { QrCanvasRenderer.renderToBitmap(QrMatrixGenerator.generateMatrix(qrCode.content, qrCode.qrConfig, 0), qrCode.qrConfig, 512) }
                                    val result = QrExporter.saveToGallery(context, bitmap, "QR_${qrCode.id}")
                                    if (result.isSuccess) {
                                        Toast.makeText(context, context.getString(R.string.saved_to_gallery), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.failed_to_save), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onCopyClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("QR Content", qrCode.content))
                                Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                            },
                            onEditClick = { onEditQrClick(qrCode.id) }
                        )
                    }
                }

                // SECTION 2: SON TARANAN QR KODLAR
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.recent_scanned_qr_codes),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 0.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 7.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${scannedCodes.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                if (scannedCodes.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.no_scanned_qr_codes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    items(scannedCodes, key = { it.id }) { qrCode ->
                        QrCodeItemCard(
                            qrCode = qrCode,
                            onClick = { onQrClick(qrCode.id) },
                            onFavoriteToggle = { viewModel.toggleFavorite(qrCode.id, qrCode.isFavorite) },
                            onDeleteClick = { qrToDelete = qrCode },
                            onMoveClick = { qrToMove = qrCode },
                            onShareClick = {
                                scope.launch {
                                    val bitmap = withContext(Dispatchers.Default) { QrCanvasRenderer.renderToBitmap(QrMatrixGenerator.generateMatrix(qrCode.content, qrCode.qrConfig, 0), qrCode.qrConfig, 512) }
                                    QrExporter.shareBitmap(context, bitmap, qrCode.content)
                                }
                            },
                            onSaveGalleryClick = {
                                scope.launch {
                                    val bitmap = withContext(Dispatchers.Default) { QrCanvasRenderer.renderToBitmap(QrMatrixGenerator.generateMatrix(qrCode.content, qrCode.qrConfig, 0), qrCode.qrConfig, 512) }
                                    val result = QrExporter.saveToGallery(context, bitmap, "QR_${qrCode.id}")
                                    if (result.isSuccess) {
                                        Toast.makeText(context, context.getString(R.string.saved_to_gallery), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.failed_to_save), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onCopyClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("QR Content", qrCode.content))
                                Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                            },
                            onEditClick = { onEditQrClick(qrCode.id) }
                        )
                    }
                }
            } else {
                // Tab-specific filtered list (Created, Scanned, Favorites, etc.)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (uiState.selectedTab) {
                                QrFilterTab.CREATED -> stringResource(R.string.tab_created, uiState.qrCodes.size)
                                QrFilterTab.SCANNED -> stringResource(R.string.tab_scanned, uiState.qrCodes.size)
                                QrFilterTab.FAVORITES -> stringResource(R.string.tab_favorites, uiState.qrCodes.size)
                                else -> stringResource(R.string.recent_qr_codes)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                items(uiState.qrCodes, key = { it.id }) { qrCode ->
                    QrCodeItemCard(
                        qrCode = qrCode,
                        onClick = { onQrClick(qrCode.id) },
                        onFavoriteToggle = { viewModel.toggleFavorite(qrCode.id, qrCode.isFavorite) },
                        onDeleteClick = { qrToDelete = qrCode },
                        onMoveClick = { qrToMove = qrCode },
                        onShareClick = {
                            scope.launch {
                                val bitmap = withContext(Dispatchers.Default) { QrCanvasRenderer.renderToBitmap(QrMatrixGenerator.generateMatrix(qrCode.content, qrCode.qrConfig, 0), qrCode.qrConfig, 512) }
                                QrExporter.shareBitmap(context, bitmap, qrCode.content)
                            }
                        },
                        onSaveGalleryClick = {
                            scope.launch {
                                val bitmap = withContext(Dispatchers.Default) { QrCanvasRenderer.renderToBitmap(QrMatrixGenerator.generateMatrix(qrCode.content, qrCode.qrConfig, 0), qrCode.qrConfig, 512) }
                                val result = QrExporter.saveToGallery(context, bitmap, "QR_${qrCode.id}")
                                if (result.isSuccess) {
                                    Toast.makeText(context, context.getString(R.string.saved_to_gallery), Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, context.getString(R.string.failed_to_save), Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onCopyClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("QR Content", qrCode.content))
                            Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                        },
                        onEditClick = { onEditQrClick(qrCode.id) }
                    )
                }
            }
        }
    }

    // New Folder Dialog
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = {
                showNewFolderDialog = false
                folderNameInput = ""
            },
            title = { Text(stringResource(R.string.new_folder)) },
            text = {
                OutlinedTextField(
                    value = folderNameInput,
                    onValueChange = { folderNameInput = it },
                    label = { Text(stringResource(R.string.folder_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderNameInput.isNotBlank()) {
                            viewModel.createFolder(folderNameInput)
                            showNewFolderDialog = false
                            folderNameInput = ""
                        }
                    }
                ) {
                    Text(stringResource(R.string.create_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false; folderNameInput = "" }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Delete QR Confirm Dialog
    qrToDelete?.let { qr ->
        AlertDialog(
            onDismissRequest = { qrToDelete = null },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteQrCode(qr.id)
                        qrToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { qrToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Move to Folder Dialog
    qrToMove?.let { qr ->
        AlertDialog(
            onDismissRequest = { qrToMove = null },
            title = { Text(stringResource(R.string.action_move_to_folder)) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.moveToFolder(qr.id, null)
                                    qrToMove = null
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.none_folder), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    items(uiState.folders) { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.moveToFolder(qr.id, folder.id)
                                    qrToMove = null
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(folder.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { qrToMove = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun FolderCard(
    folder: Folder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 150.dp, height = 110.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${folder.itemCount} items",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(folder.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QrCodeItemCard(
    qrCode: QrCode,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDeleteClick: () -> Unit,
    onMoveClick: () -> Unit,
    onShareClick: () -> Unit,
    onSaveGalleryClick: () -> Unit,
    onCopyClick: () -> Unit,
    onEditClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val thumbKey = "${qrCode.id}_${qrCode.content}_${qrCode.qrConfig.hashCode()}"
    var qrBitmap by remember(thumbKey) { mutableStateOf(QrThumbnailCache.get(thumbKey)) }
    LaunchedEffect(thumbKey) {
        if (qrCode.type == QrType.OLUSTURULAN && qrBitmap == null) {
            qrBitmap = withContext(Dispatchers.IO) {
                try {
                    val matrix = QrMatrixGenerator.generateMatrix(qrCode.content, qrCode.qrConfig, 0)
                    QrCanvasRenderer.renderToBitmap(matrix, qrCode.qrConfig, 112).also { QrThumbnailCache.put(thumbKey, it) }
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // QR Glyph / Thumbnail Container
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (qrCode.type == QrType.OLUSTURULAN)
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                else
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrCode.type == QrType.TARANAN) {
                            Icon(
                                Icons.Default.Link,
                                contentDescription = "Bağlantı",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            val bmp = qrBitmap
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "QR Thumbnail",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(3.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = Color(qrCode.qrConfig.foregroundColor),
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    }

                    // Content info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = getDisplayTitle(qrCode),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = qrCode.content,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = qrCode.payloadType.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(qrCode.createdAt)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Favorite button
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        if (qrCode.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (qrCode.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Inline Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Primary payload-specific action
                    when (qrCode.payloadType) {
                        QrPayloadType.URL -> {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(qrCode.content)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, context.getString(R.string.error_no_app_open_link), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.action_open_link), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        QrPayloadType.WIFI -> {
                            Button(
                                onClick = onCopyClick,
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.action_join_wifi), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        else -> {
                            Button(
                                onClick = onCopyClick,
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.action_copy), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    // Copy icon button
                    IconButton(onClick = onCopyClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onShareClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.edit_design_title)) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    showMenu = false
                                    onEditClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_save_to_gallery)) },
                                onClick = {
                                    showMenu = false
                                    onSaveGalleryClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_move_to_folder)) },
                                onClick = {
                                    showMenu = false
                                    onMoveClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDeleteClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getDisplayTitle(qrCode: QrCode): String {
    return when (qrCode.payloadType) {
        QrPayloadType.URL -> {
            qrCode.content.removePrefix("https://").removePrefix("http://").take(30)
        }
        QrPayloadType.WIFI -> {
            val ssid = qrCode.content.substringAfter("S:", "").substringBefore(";")
            if (ssid.isNotBlank()) "Wi-Fi: $ssid" else "Wi-Fi Network"
        }
        QrPayloadType.CONTACT -> {
            val name = qrCode.content.substringAfter("FN:", "").substringBefore("\n")
            if (name.isNotBlank()) "Contact: $name" else "Contact Card"
        }
        QrPayloadType.EMAIL -> {
            val email = qrCode.content.removePrefix("mailto:").substringBefore("?")
            if (email.isNotBlank()) "Email: $email" else "Email"
        }
        else -> qrCode.content.take(24)
    }
}
