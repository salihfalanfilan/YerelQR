package com.example.offlineqr

import com.example.offlineqr.domain.model.ErrorCorrectionLevel
import com.example.offlineqr.domain.model.QrConfig
import com.example.offlineqr.qr.engine.QrMatrixGenerator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrGeneratorTest {

    @Test
    fun `test matrix generation for valid URL with Level H`() = runBlocking {
        val config = QrConfig(errorCorrectionLevel = ErrorCorrectionLevel.H.name)
        val matrix = QrMatrixGenerator.generateMatrix("https://developer.android.com", config, 256)

        assertNotNull(matrix)
        assertEquals(256, matrix.width)
        assertEquals(256, matrix.height)
    }

    @Test
    fun `test module matrix with size 0 returns raw module dimensions`() = runBlocking {
        val config = QrConfig(quietZone = 2f)
        val matrix = QrMatrixGenerator.generateMatrix("https://developer.android.com", config, 0)
        assertNotNull(matrix)
        assertTrue(matrix.width < 100)
        // Top-left finder at (0, 0)
        assertTrue(matrix.get(0, 0))
        assertTrue(matrix.get(6, 0))
        assertTrue(matrix.get(0, 6))
        assertTrue(matrix.get(6, 6))
        // Top-right finder
        assertTrue(matrix.get(matrix.width - 7, 0))
        assertTrue(matrix.get(matrix.width - 1, 0))
        // Bottom-left finder
        assertTrue(matrix.get(0, matrix.height - 7))
        assertTrue(matrix.get(0, matrix.height - 1))
    }

    @Test
    fun `test matrix generation for all error correction levels`() = runBlocking {
        val levels = listOf(
            ErrorCorrectionLevel.L,
            ErrorCorrectionLevel.M,
            ErrorCorrectionLevel.Q,
            ErrorCorrectionLevel.H
        )

        for (lvl in levels) {
            val config = QrConfig(errorCorrectionLevel = lvl.name)
            val matrix = QrMatrixGenerator.generateMatrix("WIFI:S:MyNetwork;T:WPA;P:Secret;;", config, 200)
            assertNotNull(matrix)
            assertEquals(200, matrix.width)
            assertEquals(200, matrix.height)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test empty content throws IllegalArgumentException`(): Unit = runBlocking {
        QrMatrixGenerator.generateMatrix("", QrConfig(), 256)
    }
}
