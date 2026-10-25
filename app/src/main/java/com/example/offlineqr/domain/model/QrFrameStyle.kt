package com.example.offlineqr.domain.model

enum class QrFrameStyle {
    NONE,
    BOTTOM_BADGE,
    TOP_BADGE,
    ROUNDED_FRAME,
    BOX_BORDER;

    companion object {
        fun fromString(value: String?): QrFrameStyle = try {
            if (value != null) valueOf(value) else NONE
        } catch (e: Exception) {
            NONE
        }
    }
}
