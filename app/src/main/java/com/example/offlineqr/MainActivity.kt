package com.example.offlineqr

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.example.offlineqr.domain.model.ThemeMode
import com.example.offlineqr.domain.usecase.SettingsUseCases
import com.example.offlineqr.ui.navigation.MainNavGraph
import com.example.offlineqr.ui.theme.OfflineQRTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsUseCases: SettingsUseCases

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val appLanguage by settingsUseCases.appLanguage.collectAsState(initial = "SYSTEM")
            val themeMode by settingsUseCases.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val dynamicColor by settingsUseCases.isDynamicColor.collectAsState(initial = true)
            val accentColorHex by settingsUseCases.accentColorHex.collectAsState(initial = "#00677D")

            val baseConfiguration = LocalConfiguration.current
            val systemLocale = remember(baseConfiguration) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    baseConfiguration.locales[0]
                } else {
                    @Suppress("DEPRECATION")
                    baseConfiguration.locale
                }
            }

            val targetLocale = remember(appLanguage, systemLocale) {
                when (appLanguage.lowercase()) {
                    "tr" -> Locale("tr")
                    "en" -> Locale("en")
                    else -> systemLocale
                }
            }

            // Synchronously apply target locale to activity resources
            remember(targetLocale) {
                Locale.setDefault(targetLocale)
                val actConfig = Configuration(resources.configuration)
                actConfig.setLocale(targetLocale)
                actConfig.setLayoutDirection(targetLocale)
                @Suppress("DEPRECATION")
                resources.updateConfiguration(actConfig, resources.displayMetrics)
                targetLocale
            }

            val localizedConfiguration = remember(baseConfiguration, targetLocale) {
                Configuration(baseConfiguration).apply {
                    setLocale(targetLocale)
                    setLayoutDirection(targetLocale)
                }
            }

            val accentColor = try {
                Color(android.graphics.Color.parseColor(accentColorHex))
            } catch (e: Exception) {
                null
            }

            // CRITICAL: NEVER override LocalContext with a ContextImpl,
            // as Hilt, Navigation, and ViewModelStore require LocalContext to remain the Activity!
            CompositionLocalProvider(
                LocalConfiguration provides localizedConfiguration
            ) {
                OfflineQRTheme(
                    themeMode = themeMode,
                    accentColor = accentColor,
                    dynamicColor = false
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val navController = rememberNavController()
                        MainNavGraph(navController = navController)
                    }
                }
            }
        }
    }
}
