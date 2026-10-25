package com.example.offlineqr.domain.repository

import com.example.offlineqr.domain.model.ErrorCorrectionLevel
import com.example.offlineqr.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    val accentColorHex: Flow<String>
    val isDynamicColor: Flow<Boolean>
    val defaultErrorCorrection: Flow<ErrorCorrectionLevel>
    val vibrateOnScan: Flow<Boolean>
    val beepOnScan: Flow<Boolean>
    val autoOpenUrls: Flow<Boolean>
    val batchScanMode: Flow<Boolean>
    val customColors: Flow<List<String>>
    val customTemplates: Flow<List<com.example.offlineqr.domain.model.QrTemplate>>
    val appLanguage: Flow<String>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setAccentColorHex(hex: String)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setDefaultErrorCorrection(level: ErrorCorrectionLevel)
    suspend fun setVibrateOnScan(enabled: Boolean)
    suspend fun setBeepOnScan(enabled: Boolean)
    suspend fun setAutoOpenUrls(enabled: Boolean)
    suspend fun setBatchScanMode(enabled: Boolean)
    suspend fun addCustomColor(hex: String)
    suspend fun removeCustomColor(hex: String)
    suspend fun saveCustomTemplate(template: com.example.offlineqr.domain.model.QrTemplate)
    suspend fun removeCustomTemplate(id: String)
    suspend fun setAppLanguage(language: String)
    suspend fun resetToDefaults()
}
