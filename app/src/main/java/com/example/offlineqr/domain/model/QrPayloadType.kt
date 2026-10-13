package com.example.offlineqr.domain.model

enum class QrPayloadType {
    URL,
    WIFI,
    TEXT,
    CONTACT,
    EMAIL,
    EVENT,
    BARCODE;

    companion object {
        fun fromContent(content: String): QrPayloadType {
            val trimmed = content.trim()
            return when {
                trimmed.startsWith("WIFI:", ignoreCase = true) -> WIFI
                trimmed.startsWith("BEGIN:VCARD", ignoreCase = true) -> CONTACT
                trimmed.startsWith("mailto:", ignoreCase = true) || trimmed.startsWith("MATMSG:", ignoreCase = true) -> EMAIL
                trimmed.startsWith("BEGIN:VEVENT", ignoreCase = true) -> EVENT
                trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> URL
                else -> TEXT
            }
        }
    }
}
