package com.example.offlineqr.ui.settings

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import com.example.offlineqr.ui.components.ColorSpectrumDialog
import com.example.offlineqr.ui.components.DeleteColorConfirmDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offlineqr.R
import com.example.offlineqr.domain.model.ErrorCorrectionLevel
import com.example.offlineqr.domain.model.ThemeMode

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var showEcDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showAddHexDialog by remember { mutableStateOf(false) }
    var showSpectrumDialog by remember { mutableStateOf(false) }
    var colorToDelete by remember { mutableStateOf<String?>(null) }
    var newHexInput by remember { mutableStateOf("#") }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Search Preferences Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_preferences)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // SECTION 1: Appearance & Theme
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text(
                            text = stringResource(R.string.appearance_and_theme),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column {
                                Text(
                                    text = stringResource(R.string.display_theme),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.display_theme_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // 4 Segmented Buttons (System, Light, Dark, AMOLED)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val themes = listOf(
                                    ThemeMode.SYSTEM to (Icons.Default.SettingsBrightness to stringResource(R.string.theme_system)),
                                    ThemeMode.LIGHT to (Icons.Default.LightMode to stringResource(R.string.theme_light)),
                                    ThemeMode.DARK to (Icons.Default.DarkMode to stringResource(R.string.theme_dark)),
                                    ThemeMode.AMOLED to (Icons.Default.Contrast to stringResource(R.string.theme_amoled))
                                )
                                themes.forEach { (mode, iconLabel) ->
                                    val isSelected = uiState.themeMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                else Color.Transparent
                                            )
                                            .clickable { viewModel.setThemeMode(mode) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                iconLabel.first,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = iconLabel.second,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // AMOLED Info Micro-Card (No lightning bolt icon as requested)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.amoled_info),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            // Accent Palette Swatches & Custom Gradient Palette (Always customizable)
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.custom_accent_palette),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    TextButton(onClick = { viewModel.setAccentColorHex("#00677D") }) {
                                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(stringResource(R.string.reset_colors), style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                // Active Accent Gradient Preview Bar
                                val currentAccentColor = try {
                                    Color(android.graphics.Color.parseColor(uiState.accentColorHex))
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    currentAccentColor,
                                                    currentAccentColor.copy(alpha = 0.6f),
                                                    currentAccentColor.copy(alpha = 0.2f)
                                                )
                                            )
                                        )
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                )

                                // Palette Swatches & Add Button
                                val defaultSwatches = listOf(
                                    "#00677D", // Teal
                                    "#1976D2", // Blue
                                    "#388E3C", // Green
                                    "#7B1FA2", // Purple
                                    "#D97706", // Amber
                                    "#E11D48", // Rose
                                    "#475569"  // Slate
                                )
                                var isSettingsDeleteModeActive by remember { mutableStateOf(false) }
                                val filteredCustom = remember(uiState.customColors) { uiState.customColors.filter { !defaultSwatches.contains(it) } }

                                if (filteredCustom.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isSettingsDeleteModeActive) stringResource(R.string.cancel) else stringResource(R.string.delete_colors_btn),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSettingsDeleteModeActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                                            fontWeight = if (isSettingsDeleteModeActive) FontWeight.Bold else FontWeight.Medium,
                                            modifier = Modifier.clickable { isSettingsDeleteModeActive = !isSettingsDeleteModeActive }
                                        )
                                    }
                                }

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Pinned "+ Renk Ekle" button
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .height(36.dp)
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                .clickable { showSpectrumDialog = true }
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

                                    // Default System Swatches (Never deletable)
                                    items(defaultSwatches) { hex ->
                                        val color = try {
                                            Color(android.graphics.Color.parseColor(hex))
                                        } catch (e: Exception) {
                                            Color.Gray
                                        }
                                        val isSel = uiState.accentColorHex.equals(hex, ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .border(
                                                    if (isSel) 2.5.dp else 1.dp,
                                                    if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                                    CircleShape
                                                )
                                                .clickable { viewModel.setAccentColorHex(hex) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSel) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }

                                    // User Custom Swatches (Only non-system custom colors)
                                    items(filteredCustom) { hex ->
                                        val color = try {
                                            Color(android.graphics.Color.parseColor(hex))
                                        } catch (e: Exception) {
                                            Color.Gray
                                        }
                                        val isSel = uiState.accentColorHex.equals(hex, ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .border(
                                                    if (isSel) 2.5.dp else 1.dp,
                                                    if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                                    CircleShape
                                                )
                                                .clickable {
                                                    if (isSettingsDeleteModeActive) {
                                                        colorToDelete = hex
                                                    } else {
                                                        viewModel.setAccentColorHex(hex)
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSel && !isSettingsDeleteModeActive) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                            }

                                            // Delete badge only visible when user deliberately taps "Renk Sil"
                                            if (isSettingsDeleteModeActive) {
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
                    }
                }
            }

            // SECTION 2: Scanner Defaults
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text(
                            text = stringResource(R.string.scanner_defaults),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column {
                            // Default Error Correction Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showEcDialog = true }
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(20.dp))
                                    }
                                    Column {
                                        Text(stringResource(R.string.default_error_correction), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                        Text("Level ${uiState.defaultErrorCorrection.name} (${uiState.defaultErrorCorrection.percentage}% recovery)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(start = 56.dp))

                            // Vibrate on Scan
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Vibration, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                    }
                                    Column {
                                        Text(stringResource(R.string.vibrate_on_scan), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                        Text(stringResource(R.string.vibrate_on_scan_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Switch(
                                    checked = uiState.vibrateOnScan,
                                    onCheckedChange = { viewModel.setVibrateOnScan(it) }
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(start = 56.dp))

                            // Play Audible Beep
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                    }
                                    Column {
                                        Text(stringResource(R.string.play_beep), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                        Text(stringResource(R.string.play_beep_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Switch(
                                    checked = uiState.beepOnScan,
                                    onCheckedChange = { viewModel.setBeepOnScan(it) }
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(start = 56.dp))

                            // Auto Open URLs
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                    }
                                    Column {
                                        Text(stringResource(R.string.auto_open_urls), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                        Text(stringResource(R.string.auto_open_urls_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Switch(
                                    checked = uiState.autoOpenUrls,
                                    onCheckedChange = { viewModel.setAutoOpenUrls(it) }
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 3: Storage & Privacy
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.LockReset, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text(
                            text = stringResource(R.string.storage_and_privacy),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column {
                            // Offline SQLite Database Info
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f))
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text(stringResource(R.string.local_sqlite_db), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        text = stringResource(R.string.local_db_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                            // Clear History Action
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showClearHistoryDialog = true }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text(stringResource(R.string.clear_history), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                                    Text(stringResource(R.string.clear_history_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(start = 56.dp))

                            // Reset Preferences Action
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showResetDialog = true }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text(stringResource(R.string.reset_preferences), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(stringResource(R.string.reset_preferences_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 4: Language
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text(
                            text = stringResource(R.string.language),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable { showLanguageDialog = true }
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(stringResource(R.string.app_language), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    when (uiState.appLanguage.lowercase()) {
                                        "tr" -> stringResource(R.string.turkish_lang)
                                        "en" -> stringResource(R.string.english_lang)
                                        else -> stringResource(R.string.system_default_lang)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 5: About
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
                    }
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.app_version_info), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    // Error Correction Level Picker Dialog
    if (showEcDialog) {
        AlertDialog(
            onDismissRequest = { showEcDialog = false },
            title = { Text(stringResource(R.string.default_error_correction)) },
            text = {
                Column {
                    ErrorCorrectionLevel.entries.forEach { level ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setDefaultErrorCorrection(level)
                                    showEcDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.defaultErrorCorrection == level,
                                onClick = {
                                    viewModel.setDefaultErrorCorrection(level)
                                    showEcDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Level ${level.name} (${level.percentage}% recovery)")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showEcDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Language Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.app_language)) },
            text = {
                Column {
                    listOf(
                        "SYSTEM" to stringResource(R.string.system_default_lang),
                        "tr" to stringResource(R.string.turkish_lang),
                        "en" to stringResource(R.string.english_lang)
                    ).forEach { (code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setAppLanguage(code)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.appLanguage.equals(code, ignoreCase = true),
                                onClick = {
                                    viewModel.setAppLanguage(code)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Clear History Confirm Dialog
    if (showClearHistoryDialog) {
        val clearedMsg = stringResource(R.string.history_cleared_toast)
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text(stringResource(R.string.clear_history_title)) },
            text = { Text(stringResource(R.string.clear_history_confirm_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearHistoryDialog = false
                        Toast.makeText(context, clearedMsg, Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Reset Preferences Dialog
    if (showResetDialog) {
        val resetMsg = stringResource(R.string.preferences_reset_toast)
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.reset_preferences)) },
            text = { Text(stringResource(R.string.reset_preferences_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetToDefaults()
                        showResetDialog = false
                        Toast.makeText(context, resetMsg, Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Add Custom Accent Color Dialog
    if (showAddHexDialog) {
        val isValidHex = newHexInput.matches(Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$"))
        val previewColor = try {
            if (isValidHex) Color(android.graphics.Color.parseColor(newHexInput)) else Color.Transparent
        } catch (e: Exception) {
            Color.Transparent
        }

        AlertDialog(
            onDismissRequest = { showAddHexDialog = false },
            title = { Text(stringResource(R.string.add_custom_color)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.hex_dialog_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = newHexInput,
                        onValueChange = { input ->
                            val clean = if (!input.startsWith("#")) "#$input" else input
                            newHexInput = clean.uppercase()
                        },
                        label = { Text(stringResource(R.string.hex_code_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Live preview & quick gradient suggestions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.color_preview_label), style = MaterialTheme.typography.labelMedium)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(previewColor)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )
                    }

                    // Quick color suggestion row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("#FF5722", "#00BCD4", "#8BC34A", "#FFC107", "#9C27B0").forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .clickable { newHexInput = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isValidHex) {
                            viewModel.addCustomColor(newHexInput)
                            showAddHexDialog = false
                            newHexInput = "#"
                        }
                    },
                    enabled = isValidHex
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddHexDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showSpectrumDialog) {
        val currentLong = try {
            Color(android.graphics.Color.parseColor(uiState.accentColorHex)).value.toLong() shr 32
        } catch (e: Exception) {
            0xFF00677DL
        }
        ColorSpectrumDialog(
            initialColor = currentLong,
            title = stringResource(R.string.accent_spectrum_title),
            onColorSelected = { _, hex ->
                showSpectrumDialog = false
                viewModel.setAccentColorHex(hex)
            },
            onSaveToCustomPalette = { hex ->
                viewModel.addCustomColor(hex)
                viewModel.setAccentColorHex(hex)
            },
            onDismissRequest = { showSpectrumDialog = false }
        )
    }

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
}

