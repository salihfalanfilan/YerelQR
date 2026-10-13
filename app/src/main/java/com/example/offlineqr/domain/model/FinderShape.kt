package com.example.offlineqr.domain.model

enum class FinderShape {
    SQUARE,
    ROUNDED,
    CIRCLE,
    DOT,
    LEAF,
    DIAMOND,
    HEXAGON,
    DOUBLE_CIRCLE;

    companion object {
        fun fromString(value: String): FinderShape {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: ROUNDED
        }
    }
}
