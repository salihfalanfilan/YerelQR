package com.example.offlineqr.domain.model

enum class ModuleShape {
    SQUARE,
    PILLS,
    CIRCLES,
    DIAMOND,
    STAR,
    HEXAGON,
    LEAF;

    companion object {
        fun fromString(value: String): ModuleShape {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SQUARE
        }
    }
}
