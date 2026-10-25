package com.example.offlineqr

import com.example.offlineqr.data.local.Converters
import com.example.offlineqr.domain.model.ErrorCorrectionLevel
import com.example.offlineqr.domain.model.FinderShape
import com.example.offlineqr.domain.model.GradientType
import com.example.offlineqr.domain.model.ModuleShape
import com.example.offlineqr.domain.model.QrConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrConfigSerializationTest {

    private val converters = Converters()

    @Test
    fun `test QrConfig serialization and deserialization roundtrip`() {
        val originalConfig = QrConfig(
            backgroundColor = 0xFFFFFFFFL,
            backgroundSecondaryColor = 0xFFEEEEEE,
            backgroundGradientType = GradientType.LINEAR.name,
            foregroundColor = 0xFF00677DL,
            secondaryColor = 0xFF004E5FL,
            gradientType = GradientType.LINEAR.name,
            moduleShape = ModuleShape.PILLS.name,
            finderShape = FinderShape.ROUNDED.name,
            errorCorrectionLevel = ErrorCorrectionLevel.H.name,
            logoPath = "/cache/logo.png",
            logoScale = 0.25f,
            quietZone = 2f,
            backgroundCornerRadius = 0.15f,
            frameStyle = com.example.offlineqr.domain.model.QrFrameStyle.BOTTOM_BADGE.name,
            frameText = "BENİ TARA",
            frameColor = 0xFF1976D2L,
            frameTextColor = 0xFFFFFFFFL
        )

        val jsonString = converters.fromQrConfig(originalConfig)
        assertNotNull(jsonString)

        val restoredConfig = converters.toQrConfig(jsonString)
        assertEquals(originalConfig.backgroundColor, restoredConfig.backgroundColor)
        assertEquals(originalConfig.backgroundSecondaryColor, restoredConfig.backgroundSecondaryColor)
        assertEquals(originalConfig.backgroundGradientType, restoredConfig.backgroundGradientType)
        assertEquals(originalConfig.foregroundColor, restoredConfig.foregroundColor)
        assertEquals(originalConfig.secondaryColor, restoredConfig.secondaryColor)
        assertEquals(originalConfig.gradientType, restoredConfig.gradientType)
        assertEquals(originalConfig.moduleShape, restoredConfig.moduleShape)
        assertEquals(originalConfig.finderShape, restoredConfig.finderShape)
        assertEquals(originalConfig.errorCorrectionLevel, restoredConfig.errorCorrectionLevel)
        assertEquals(originalConfig.logoPath, restoredConfig.logoPath)
        assertEquals(originalConfig.logoScale, restoredConfig.logoScale, 0.001f)
        assertEquals(originalConfig.quietZone, restoredConfig.quietZone, 0.001f)
        assertEquals(originalConfig.qrScale, restoredConfig.qrScale, 0.001f)
        assertEquals(originalConfig.backgroundCornerRadius, restoredConfig.backgroundCornerRadius, 0.001f)
        assertEquals(originalConfig.frameStyle, restoredConfig.frameStyle)
        assertEquals(originalConfig.frameText, restoredConfig.frameText)
        assertEquals(originalConfig.frameColor, restoredConfig.frameColor)
        assertEquals(originalConfig.frameTextColor, restoredConfig.frameTextColor)
        assertEquals(originalConfig.gradientDirection, restoredConfig.gradientDirection)
        assertEquals(originalConfig.backgroundGradientDirection, restoredConfig.backgroundGradientDirection)
    }

    @Test
    fun `test null and empty string handling in Converters`() {
        val defaultConfigFromNull = converters.toQrConfig(null)
        assertNotNull(defaultConfigFromNull)
        assertEquals(0xFFFFFFFFL, defaultConfigFromNull.backgroundColor)

        val defaultConfigFromBlank = converters.toQrConfig("   ")
        assertNotNull(defaultConfigFromBlank)
        assertEquals(0xFFFFFFFFL, defaultConfigFromBlank.backgroundColor)

        val jsonFromNull = converters.fromQrConfig(null)
        assertNotNull(jsonFromNull)
        val decoded = converters.toQrConfig(jsonFromNull)
        assertNotNull(decoded)
    }

    @Test
    fun `test all gradient directions serialization and SVG rendering`() = kotlinx.coroutines.runBlocking {
        val directions = listOf(
            com.example.offlineqr.domain.model.GradientDirection.VERTICAL,
            com.example.offlineqr.domain.model.GradientDirection.HORIZONTAL,
            com.example.offlineqr.domain.model.GradientDirection.DIAGONAL,
            com.example.offlineqr.domain.model.GradientDirection.REVERSE_DIAGONAL,
            com.example.offlineqr.domain.model.GradientDirection.RADIAL
        )

        for (dir in directions) {
            val cfg = QrConfig(
                gradientType = if (dir == com.example.offlineqr.domain.model.GradientDirection.RADIAL) GradientType.RADIAL.name else GradientType.LINEAR.name,
                gradientDirection = dir.name,
                backgroundGradientType = if (dir == com.example.offlineqr.domain.model.GradientDirection.RADIAL) GradientType.RADIAL.name else GradientType.LINEAR.name,
                backgroundGradientDirection = dir.name
            )

            // Serialization check
            val json = converters.fromQrConfig(cfg)
            val restored = converters.toQrConfig(json)
            assertEquals(dir.name, restored.gradientDirection)
            assertEquals(dir.name, restored.backgroundGradientDirection)

            // SVG rendering check
            val matrix = com.example.offlineqr.qr.engine.QrMatrixGenerator.generateMatrix("https://github.com/salihfalanfilan/", cfg, 0)
            val svg = com.example.offlineqr.qr.engine.QrSvgRenderer.renderToSvg(matrix, cfg, 512)
            assertNotNull(svg)
            assertTrue(svg.contains("<svg"))
            if (dir == com.example.offlineqr.domain.model.GradientDirection.RADIAL) {
                assertTrue(svg.contains("<radialGradient"))
            } else {
                assertTrue(svg.contains("<linearGradient"))
            }
        }
    }
}
