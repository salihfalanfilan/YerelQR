package com.example.offlineqr.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.offlineqr.domain.model.ErrorCorrectionLevel
import com.example.offlineqr.domain.model.ThemeMode
import com.example.offlineqr.domain.usecase.SettingsUseCases
import com.example.offlineqr.domain.repository.QrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isDynamicColor: Boolean = false,
    val accentColorHex: String = "#00677D",
    val customColors: List<String> = emptyList(),
    val defaultErrorCorrection: ErrorCorrectionLevel = ErrorCorrectionLevel.H,
    val vibrateOnScan: Boolean = true,
    val beepOnScan: Boolean = true,
    val autoOpenUrls: Boolean = false,
    val batchScanMode: Boolean = false,
    val appLanguage: String = "SYSTEM",
    val searchQuery: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsUseCases: SettingsUseCases,
    private val qrRepository: QrRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            settingsUseCases.themeMode,
            settingsUseCases.isDynamicColor,
            settingsUseCases.accentColorHex,
            settingsUseCases.defaultErrorCorrection,
            settingsUseCases.vibrateOnScan
        ) { theme, dynamic, accent, ec, vibrate ->
            listOf(theme, dynamic, accent, ec, vibrate)
        },
        combine(
            settingsUseCases.beepOnScan,
            settingsUseCases.autoOpenUrls,
            settingsUseCases.batchScanMode,
            settingsUseCases.appLanguage,
            settingsUseCases.customColors
        ) { beep, autoUrl, batch, lang, colors ->
            listOf(beep, autoUrl, batch, lang, colors)
        }
    ) { p1, p2 ->
        SettingsUiState(
            themeMode = p1[0] as ThemeMode,
            isDynamicColor = p1[1] as Boolean,
            accentColorHex = p1[2] as String,
            defaultErrorCorrection = p1[3] as ErrorCorrectionLevel,
            vibrateOnScan = p1[4] as Boolean,
            beepOnScan = p2[0] as Boolean,
            autoOpenUrls = p2[1] as Boolean,
            batchScanMode = p2[2] as Boolean,
            appLanguage = p2[3] as String,
            customColors = (p2[4] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsUseCases.setThemeMode(mode)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsUseCases.setDynamicColor(enabled)
        }
    }

    fun setAccentColorHex(hex: String) {
        viewModelScope.launch {
            settingsUseCases.setAccentColorHex(hex)
        }
    }

    fun addCustomColor(hex: String) {
        viewModelScope.launch {
            if (hex.matches(Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$"))) {
                settingsUseCases.addCustomColor(hex)
                settingsUseCases.setAccentColorHex(hex)
            }
        }
    }

    fun removeCustomColor(hex: String) {
        viewModelScope.launch {
            settingsUseCases.removeCustomColor(hex)
        }
    }

    fun setDefaultErrorCorrection(level: ErrorCorrectionLevel) {
        viewModelScope.launch {
            settingsUseCases.setDefaultErrorCorrection(level)
        }
    }

    fun setVibrateOnScan(enabled: Boolean) {
        viewModelScope.launch {
            settingsUseCases.setVibrateOnScan(enabled)
        }
    }

    fun setBeepOnScan(enabled: Boolean) {
        viewModelScope.launch {
            settingsUseCases.setBeepOnScan(enabled)
        }
    }

    fun setAutoOpenUrls(enabled: Boolean) {
        viewModelScope.launch {
            settingsUseCases.setAutoOpenUrls(enabled)
        }
    }

    fun setBatchScanMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsUseCases.setBatchScanMode(enabled)
        }
    }

    fun setAppLanguage(lang: String) {
        viewModelScope.launch {
            settingsUseCases.setAppLanguage(lang)
        }
    }

    fun clearAllHistory(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            qrRepository.clearAll()
            onComplete()
        }
    }

    fun resetPreferencesToDefault(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            settingsUseCases.resetToDefaults()
            onComplete()
        }
    }

    fun resetToDefaults(onComplete: () -> Unit = {}) = resetPreferencesToDefault(onComplete)
}
