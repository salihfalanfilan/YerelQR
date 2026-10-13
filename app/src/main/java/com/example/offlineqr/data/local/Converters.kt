package com.example.offlineqr.data.local

import androidx.room.TypeConverter
import com.example.offlineqr.domain.model.QrConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @TypeConverter
    fun fromQrConfig(config: QrConfig?): String {
        return if (config == null) {
            json.encodeToString(QrConfig())
        } else {
            json.encodeToString(config)
        }
    }

    @TypeConverter
    fun toQrConfig(value: String?): QrConfig {
        return if (value.isNullOrBlank()) {
            QrConfig()
        } else {
            try {
                json.decodeFromString(value)
            } catch (e: Exception) {
                QrConfig()
            }
        }
    }
}
