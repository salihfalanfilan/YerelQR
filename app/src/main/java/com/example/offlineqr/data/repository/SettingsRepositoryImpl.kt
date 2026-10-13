package com.example.offlineqr.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.offlineqr.domain.model.ErrorCorrectionLevel
import com.example.offlineqr.domain.model.ThemeMode
import com.example.offlineqr.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DEFAULT_ERROR_CORRECTION = stringPreferencesKey("default_error_correction")
        val VIBRATE_ON_SCAN = booleanPreferencesKey("vibrate_on_scan")
        val BEEP_ON_SCAN = booleanPreferencesKey("beep_on_scan")
        val AUTO_OPEN_URLS = booleanPreferencesKey("auto_open_urls")
        val BATCH_SCAN_MODE = booleanPreferencesKey("batch_scan_mode")
        val CUSTOM_COLORS = stringPreferencesKey("custom_colors")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    private val defaultPalette = listOf(
        "#00677D", // Teal
        "#5B5B7E", // Indigo
        "#4B626A", // Emerald / Slate
        "#D97706", // Amber
        "#E11D48", // Rose
        "#475569"  // Slate
    )

    override val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        val raw = prefs[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(raw)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    override val accentColorHex: Flow<String> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.ACCENT_COLOR] ?: "#00677D"
    }

    override val isDynamicColor: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.DYNAMIC_COLOR] ?: true
    }

    override val defaultErrorCorrection: Flow<ErrorCorrectionLevel> = dataStore.data.map { prefs ->
        val raw = prefs[PreferencesKeys.DEFAULT_ERROR_CORRECTION] ?: ErrorCorrectionLevel.H.name
        ErrorCorrectionLevel.fromString(raw)
    }

    override val vibrateOnScan: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.VIBRATE_ON_SCAN] ?: true
    }

    override val beepOnScan: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.BEEP_ON_SCAN] ?: true
    }

    override val autoOpenUrls: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.AUTO_OPEN_URLS] ?: false
    }

    override val batchScanMode: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.BATCH_SCAN_MODE] ?: false
    }

    override val customColors: Flow<List<String>> = dataStore.data.map { prefs ->
        val raw = prefs[PreferencesKeys.CUSTOM_COLORS]
        if (raw.isNullOrBlank()) {
            emptyList()
        } else {
            raw.split(",").filter { it.isNotBlank() && !defaultPalette.contains(it) }
        }
    }

    override val appLanguage: Flow<String> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.APP_LANGUAGE] ?: "SYSTEM"
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[PreferencesKeys.THEME_MODE] = mode.name }
    }

    override suspend fun setAccentColorHex(hex: String) {
        dataStore.edit { it[PreferencesKeys.ACCENT_COLOR] = hex }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.DYNAMIC_COLOR] = enabled }
    }

    override suspend fun setDefaultErrorCorrection(level: ErrorCorrectionLevel) {
        dataStore.edit { it[PreferencesKeys.DEFAULT_ERROR_CORRECTION] = level.name }
    }

    override suspend fun setVibrateOnScan(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.VIBRATE_ON_SCAN] = enabled }
    }

    override suspend fun setBeepOnScan(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.BEEP_ON_SCAN] = enabled }
    }

    override suspend fun setAutoOpenUrls(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.AUTO_OPEN_URLS] = enabled }
    }

    override suspend fun setBatchScanMode(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.BATCH_SCAN_MODE] = enabled }
    }

    override suspend fun addCustomColor(hex: String) {
        if (defaultPalette.contains(hex)) return // Do not add presets as custom
        dataStore.edit { prefs ->
            val raw = prefs[PreferencesKeys.CUSTOM_COLORS]
            val current = if (raw.isNullOrBlank()) emptyList() else raw.split(",").filter { it.isNotBlank() && !defaultPalette.contains(it) }
            if (!current.contains(hex)) {
                val updated = current + hex
                prefs[PreferencesKeys.CUSTOM_COLORS] = updated.joinToString(",")
            }
        }
    }

    override suspend fun removeCustomColor(hex: String) {
        if (defaultPalette.contains(hex)) return // Cannot remove system colors!
        dataStore.edit { prefs ->
            val raw = prefs[PreferencesKeys.CUSTOM_COLORS]
            val current = if (raw.isNullOrBlank()) emptyList() else raw.split(",").filter { it.isNotBlank() }
            val updated = current.filter { it != hex && !defaultPalette.contains(it) }
            prefs[PreferencesKeys.CUSTOM_COLORS] = updated.joinToString(",")
        }
    }

    override suspend fun setAppLanguage(language: String) {
        dataStore.edit { it[PreferencesKeys.APP_LANGUAGE] = language }
    }

    override suspend fun resetToDefaults() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
