package com.example.offlineqr.domain.usecase

import com.example.offlineqr.domain.model.ErrorCorrectionLevel
import com.example.offlineqr.domain.model.ThemeMode
import com.example.offlineqr.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SettingsUseCases @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    val themeMode: Flow<ThemeMode> = settingsRepository.themeMode
    val accentColorHex: Flow<String> = settingsRepository.accentColorHex
    val isDynamicColor: Flow<Boolean> = settingsRepository.isDynamicColor
    val defaultErrorCorrection: Flow<ErrorCorrectionLevel> = settingsRepository.defaultErrorCorrection
    val vibrateOnScan: Flow<Boolean> = settingsRepository.vibrateOnScan
    val beepOnScan: Flow<Boolean> = settingsRepository.beepOnScan
    val autoOpenUrls: Flow<Boolean> = settingsRepository.autoOpenUrls
    val batchScanMode: Flow<Boolean> = settingsRepository.batchScanMode
    val customColors: Flow<List<String>> = settingsRepository.customColors
    val customTemplates: Flow<List<com.example.offlineqr.domain.model.QrTemplate>> = settingsRepository.customTemplates
    val appLanguage: Flow<String> = settingsRepository.appLanguage

    suspend fun setThemeMode(mode: ThemeMode) = settingsRepository.setThemeMode(mode)
    suspend fun setAccentColorHex(hex: String) = settingsRepository.setAccentColorHex(hex)
    suspend fun setDynamicColor(enabled: Boolean) = settingsRepository.setDynamicColor(enabled)
    suspend fun setDefaultErrorCorrection(level: ErrorCorrectionLevel) = settingsRepository.setDefaultErrorCorrection(level)
    suspend fun setVibrateOnScan(enabled: Boolean) = settingsRepository.setVibrateOnScan(enabled)
    suspend fun setBeepOnScan(enabled: Boolean) = settingsRepository.setBeepOnScan(enabled)
    suspend fun setAutoOpenUrls(enabled: Boolean) = settingsRepository.setAutoOpenUrls(enabled)
    suspend fun setBatchScanMode(enabled: Boolean) = settingsRepository.setBatchScanMode(enabled)
    suspend fun addCustomColor(hex: String) = settingsRepository.addCustomColor(hex)
    suspend fun removeCustomColor(hex: String) = settingsRepository.removeCustomColor(hex)
    suspend fun saveCustomTemplate(template: com.example.offlineqr.domain.model.QrTemplate) = settingsRepository.saveCustomTemplate(template)
    suspend fun removeCustomTemplate(id: String) = settingsRepository.removeCustomTemplate(id)
    suspend fun setAppLanguage(language: String) = settingsRepository.setAppLanguage(language)
    suspend fun resetToDefaults() = settingsRepository.resetToDefaults()
}
