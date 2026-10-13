package com.example.offlineqr.domain.model

enum class GradientType {
    NONE,
    LINEAR,
    RADIAL;

    companion object {
        fun fromString(value: String): GradientType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: LINEAR
        }
    }
}
