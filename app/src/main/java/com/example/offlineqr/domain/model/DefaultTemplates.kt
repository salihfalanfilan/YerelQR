package com.example.offlineqr.domain.model

object DefaultTemplates {
    val list: List<QrTemplate> = listOf(
        QrTemplate(
            id = "tpl_classic",
            name = "Klasik Siyah",
            qrConfig = QrConfig(
                foregroundColor = 0xFF000000L,
                backgroundColor = 0xFFFFFFFFL,
                moduleShape = ModuleShape.SQUARE.name,
                finderShape = FinderShape.SQUARE.name
            ),
            isBuiltIn = true
        ),
        QrTemplate(
            id = "tpl_modern_cyan",
            name = "Modern Mavi",
            qrConfig = QrConfig(
                foregroundColor = 0xFF00677DL,
                secondaryColor = 0xFF0284C7L,
                gradientType = GradientType.LINEAR.name,
                gradientDirection = GradientDirection.DIAGONAL.name,
                backgroundColor = 0xFFF8FAFCL,
                moduleShape = ModuleShape.PILLS.name,
                finderShape = FinderShape.ROUNDED.name
            ),
            isBuiltIn = true
        ),
        QrTemplate(
            id = "tpl_neon_dark",
            name = "Neon Gece",
            qrConfig = QrConfig(
                foregroundColor = 0xFF00E5FFL,
                backgroundColor = 0xFF121212L,
                moduleShape = ModuleShape.CIRCLES.name,
                finderShape = FinderShape.CIRCLE.name
            ),
            isBuiltIn = true
        ),
        QrTemplate(
            id = "tpl_sunset",
            name = "Gün Batımı",
            qrConfig = QrConfig(
                foregroundColor = 0xFFE11D48L,
                secondaryColor = 0xFFF59E0BL,
                gradientType = GradientType.LINEAR.name,
                gradientDirection = GradientDirection.TOP_TO_BOTTOM.name,
                backgroundColor = 0xFFFFFBEBL,
                moduleShape = ModuleShape.PILLS.name,
                finderShape = FinderShape.DOUBLE_CIRCLE.name
            ),
            isBuiltIn = true
        ),
        QrTemplate(
            id = "tpl_nature",
            name = "Doğa Yeşili",
            qrConfig = QrConfig(
                foregroundColor = 0xFF15803DL,
                secondaryColor = 0xFF22C55EL,
                gradientType = GradientType.LINEAR.name,
                gradientDirection = GradientDirection.DIAGONAL.name,
                backgroundColor = 0xFFF0FDF4L,
                moduleShape = ModuleShape.LEAF.name,
                finderShape = FinderShape.LEAF.name
            ),
            isBuiltIn = true
        ),
        QrTemplate(
            id = "tpl_badge_menu",
            name = "Rozetli Menü",
            qrConfig = QrConfig(
                foregroundColor = 0xFF00677DL,
                backgroundColor = 0xFFFFFFFFL,
                moduleShape = ModuleShape.PILLS.name,
                finderShape = FinderShape.ROUNDED.name,
                frameStyle = QrFrameStyle.BOTTOM_BADGE.name,
                frameText = "MENÜ",
                frameColor = 0xFF00677DL,
                frameTextColor = 0xFFFFFFFFL
            ),
            isBuiltIn = true
        )
    )
}
