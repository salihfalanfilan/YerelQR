package com.example.offlineqr.domain.model

enum class ErrorCorrectionLevel(val percentage: Int) {
    L(7),
    M(15),
    Q(25),
    H(30);

    companion object {
        fun fromString(value: String): ErrorCorrectionLevel {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: H
        }
    }
}
