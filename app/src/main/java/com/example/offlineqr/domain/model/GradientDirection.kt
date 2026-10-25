package com.example.offlineqr.domain.model

enum class GradientDirection {
    VERTICAL,           // Dikey (Yukarıdan aşağıya)
    HORIZONTAL,         // Yatay (Soldan sağa)
    DIAGONAL,           // Çapraz (Sol üstten sağ alta)
    REVERSE_DIAGONAL,   // Ters Çapraz (Sağ üstten sol alta)
    RADIAL,             // Dairesel

    // Backward compatibility aliases:
    TOP_TO_BOTTOM,
    BOTTOM_TO_TOP,
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT;

    fun getLabelResId(): Int = when (this) {
        VERTICAL, TOP_TO_BOTTOM -> com.example.offlineqr.R.string.grad_dir_vertical
        HORIZONTAL, LEFT_TO_RIGHT -> com.example.offlineqr.R.string.grad_dir_horizontal
        DIAGONAL -> com.example.offlineqr.R.string.grad_dir_diagonal
        REVERSE_DIAGONAL -> com.example.offlineqr.R.string.grad_dir_reverse_diagonal
        RADIAL -> com.example.offlineqr.R.string.grad_dir_radial
        BOTTOM_TO_TOP -> com.example.offlineqr.R.string.grad_dir_bottom_to_top
        RIGHT_TO_LEFT -> com.example.offlineqr.R.string.grad_dir_right_to_left
    }

    companion object {
        val selectableDirections = listOf(
            VERTICAL,
            HORIZONTAL,
            DIAGONAL,
            REVERSE_DIAGONAL,
            RADIAL
        )

        fun fromString(value: String?): GradientDirection = try {
            if (value != null) valueOf(value) else DIAGONAL
        } catch (e: Exception) {
            DIAGONAL
        }
    }
}
