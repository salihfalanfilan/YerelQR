package com.example.offlineqr.qr.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.example.offlineqr.domain.model.FinderShape
import com.example.offlineqr.domain.model.GradientType
import com.example.offlineqr.domain.model.ModuleShape
import com.example.offlineqr.domain.model.QrConfig
import com.google.zxing.common.BitMatrix
import java.io.File

object QrCanvasRenderer {

    fun renderToBitmap(
        bitMatrix: BitMatrix,
        config: QrConfig,
        targetSize: Int = 1024,
        reuseBitmap: Bitmap? = null
    ): Bitmap {
        val bitmap = if (reuseBitmap != null && reuseBitmap.width == targetSize && reuseBitmap.height == targetSize && !reuseBitmap.isRecycled && reuseBitmap.isMutable) {
            reuseBitmap.eraseColor(android.graphics.Color.TRANSPARENT)
            reuseBitmap
        } else {
            Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        }
        val canvas = Canvas(bitmap)

        val totalModsX = bitMatrix.width
        val totalModsY = bitMatrix.height

        val effectiveQuietZone = config.quietZone.coerceIn(0f, 10f)
        val totalGridUnits = totalModsX.toFloat() + (effectiveQuietZone * 2f)

        val qrScaleFactor = config.qrScale.coerceIn(0.50f, 1.0f)
        val contentBoundingSize = targetSize.toFloat() * qrScaleFactor
        val modulePixelSize = contentBoundingSize / totalGridUnits

        val startX = ((targetSize.toFloat() - contentBoundingSize) / 2f) + (effectiveQuietZone * modulePixelSize)
        val startY = ((targetSize.toFloat() - contentBoundingSize) / 2f) + (effectiveQuietZone * modulePixelSize)

        // 1. Draw Background (Solid, Linear, or Radial Gradient)
        val bgPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        val bgPrimary = config.backgroundColor.toInt()
        val bgSecondary = (config.backgroundSecondaryColor ?: config.backgroundColor).toInt()

        when (GradientType.fromString(config.backgroundGradientType)) {
            GradientType.LINEAR -> {
                bgPaint.shader = LinearGradient(
                    0f, 0f, targetSize.toFloat(), targetSize.toFloat(),
                    bgPrimary, bgSecondary,
                    Shader.TileMode.CLAMP
                )
            }
            GradientType.RADIAL -> {
                bgPaint.shader = RadialGradient(
                    targetSize / 2f, targetSize / 2f, targetSize / 1.3f,
                    bgPrimary, bgSecondary,
                    Shader.TileMode.CLAMP
                )
            }
            GradientType.NONE -> {
                bgPaint.color = bgPrimary
            }
        }
        val cornerPx = targetSize.toFloat() * config.backgroundCornerRadius.coerceIn(0f, 0.5f)
        val bgRect = RectF(0f, 0f, targetSize.toFloat(), targetSize.toFloat())
        val cardPath = Path().apply {
            if (cornerPx > 0f) {
                addRoundRect(bgRect, cornerPx, cornerPx, Path.Direction.CW)
            } else {
                addRect(bgRect, Path.Direction.CW)
            }
        }
        canvas.save()
        if (cornerPx > 0f) {
            canvas.clipPath(cardPath)
        }
        canvas.drawPath(cardPath, bgPaint)

        // 2. Setup Foreground Shader / Paint
        val fgPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        val primaryColor = config.foregroundColor.toInt()
        val secondaryColor = (config.secondaryColor ?: config.foregroundColor).toInt()

        when (GradientType.fromString(config.gradientType)) {
            GradientType.LINEAR -> {
                fgPaint.shader = LinearGradient(
                    startX, startY,
                    startX + totalModsX * modulePixelSize,
                    startY + totalModsY * modulePixelSize,
                    primaryColor, secondaryColor,
                    Shader.TileMode.CLAMP
                )
            }
            GradientType.RADIAL -> {
                val cx = startX + (totalModsX * modulePixelSize) / 2f
                val cy = startY + (totalModsY * modulePixelSize) / 2f
                val r = (totalModsX * modulePixelSize) / 1.35f
                fgPaint.shader = RadialGradient(
                    cx, cy, r,
                    primaryColor, secondaryColor,
                    Shader.TileMode.CLAMP
                )
            }
            GradientType.NONE -> {
                fgPaint.color = primaryColor
            }
        }

        val moduleShape = ModuleShape.fromString(config.moduleShape)
        val finderShape = FinderShape.fromString(config.finderShape)

        fun isFinderModule(col: Int, row: Int): Boolean {
            val inTopLeft = col in 0..6 && row in 0..6
            val inTopRight = col in (totalModsX - 7)..<totalModsX && row in 0..6
            val inBottomLeft = col in 0..6 && row in (totalModsY - 7)..<totalModsY
            return inTopLeft || inTopRight || inBottomLeft
        }

        // Center Logo Boundary calculation
        val hasLogo = !config.logoPath.isNullOrBlank() && File(config.logoPath).exists()
        val logoFraction = config.logoScale.coerceIn(0.08f, 0.35f)
        val qrPixelWidth = totalModsX * modulePixelSize
        val logoBoxSize = qrPixelWidth * logoFraction
        val logoBoxLeft = startX + (qrPixelWidth - logoBoxSize) / 2f
        val logoBoxTop = startY + (qrPixelWidth - logoBoxSize) / 2f
        val logoBoxRect = RectF(logoBoxLeft, logoBoxTop, logoBoxLeft + logoBoxSize, logoBoxTop + logoBoxSize)

        val clearedAreaRect: RectF = if (hasLogo) {
            var minC = totalModsX
            var maxC = -1
            for (c in 0 until totalModsX) {
                val l = startX + c * modulePixelSize
                val r = l + modulePixelSize
                if (l < logoBoxRect.right && r > logoBoxRect.left) {
                    if (c < minC) minC = c
                    if (c > maxC) maxC = c
                }
            }
            var minR = totalModsY
            var maxR = -1
            for (r in 0 until totalModsY) {
                val t = startY + r * modulePixelSize
                val b = t + modulePixelSize
                if (t < logoBoxRect.bottom && b > logoBoxRect.top) {
                    if (r < minR) minR = r
                    if (r > maxR) maxR = r
                }
            }
            if (minC <= maxC && minR <= maxR) {
                RectF(
                    startX + minC * modulePixelSize,
                    startY + minR * modulePixelSize,
                    startX + (maxC + 1) * modulePixelSize,
                    startY + (maxR + 1) * modulePixelSize
                )
            } else {
                logoBoxRect
            }
        } else {
            logoBoxRect
        }

        val logoCornerRadiusPx = if (hasLogo) {
            clearedAreaRect.width() * config.logoCornerRadius.coerceIn(0f, 0.5f)
        } else 0f

        fun isPointInRoundedRect(px: Float, py: Float, rect: RectF, radius: Float): Boolean {
            if (px < rect.left || px > rect.right || py < rect.top || py > rect.bottom) {
                return false
            }
            if (radius <= 0f) return true

            val inLeftCorner = px < rect.left + radius
            val inRightCorner = px > rect.right - radius
            val inTopCorner = py < rect.top + radius
            val inBottomCorner = py > rect.bottom - radius

            return when {
                inLeftCorner && inTopCorner -> {
                    val dx = px - (rect.left + radius)
                    val dy = py - (rect.top + radius)
                    dx * dx + dy * dy <= radius * radius
                }
                inRightCorner && inTopCorner -> {
                    val dx = px - (rect.right - radius)
                    val dy = py - (rect.top + radius)
                    dx * dx + dy * dy <= radius * radius
                }
                inLeftCorner && inBottomCorner -> {
                    val dx = px - (rect.left + radius)
                    val dy = py - (rect.bottom - radius)
                    dx * dx + dy * dy <= radius * radius
                }
                inRightCorner && inBottomCorner -> {
                    val dx = px - (rect.right - radius)
                    val dy = py - (rect.bottom - radius)
                    dx * dx + dy * dy <= radius * radius
                }
                else -> true
            }
        }

        // 3. Render Data Modules (Skip Finders & Logo)
        val path = Path()
        val modRect = RectF()
        for (row in 0 until totalModsY) {
            for (col in 0 until totalModsX) {
                if (isFinderModule(col, row)) {
                    continue
                }

                if (bitMatrix.get(col, row)) {
                    val left = startX + col * modulePixelSize
                    val top = startY + row * modulePixelSize
                    val right = left + modulePixelSize
                    val bottom = top + modulePixelSize
                    modRect.set(left, top, right, bottom)

                    if (hasLogo) {
                        if (config.logoCutoutFillBackground) {
                            // Shaved corners take background color: skip all modules in entire logo bounding box
                            if (RectF.intersects(logoBoxRect, modRect)) {
                                continue
                            }
                        } else {
                            // Transparent mode: skip module ONLY if its center is inside the rounded logo area
                            val cx = modRect.centerX()
                            val cy = modRect.centerY()
                            if (isPointInRoundedRect(cx, cy, clearedAreaRect, logoCornerRadiusPx)) {
                                continue
                            }
                        }
                    }

                    when (moduleShape) {
                        ModuleShape.SQUARE -> {
                            // Slight 0.5f overlap to completely eliminate hairline cracks/lines between adjacent squares
                            modRect.right += 0.5f
                            modRect.bottom += 0.5f
                            canvas.drawRect(modRect, fgPaint)
                        }
                        ModuleShape.PILLS -> {
                            val radius = modulePixelSize * 0.35f
                            canvas.drawRoundRect(modRect, radius, radius, fgPaint)
                        }
                        ModuleShape.CIRCLES -> {
                            val cx = modRect.centerX()
                            val cy = modRect.centerY()
                            val r = (modulePixelSize / 2f) * 0.90f
                            canvas.drawCircle(cx, cy, r, fgPaint)
                        }
                        ModuleShape.DIAMOND -> {
                            path.reset()
                            val cx = modRect.centerX()
                            val cy = modRect.centerY()
                            val r = modulePixelSize / 2f
                            path.moveTo(cx, cy - r)
                            path.lineTo(cx + r, cy)
                            path.lineTo(cx, cy + r)
                            path.lineTo(cx - r, cy)
                            path.close()
                            canvas.drawPath(path, fgPaint)
                        }
                        ModuleShape.STAR -> {
                            path.reset()
                            val cx = modRect.centerX()
                            val cy = modRect.centerY()
                            val r = modulePixelSize / 2f
                            val ir = r * 0.38f
                            path.moveTo(cx, cy - r)
                            path.lineTo(cx + ir, cy - ir)
                            path.lineTo(cx + r, cy)
                            path.lineTo(cx + ir, cy + ir)
                            path.lineTo(cx, cy + r)
                            path.lineTo(cx - ir, cy + ir)
                            path.lineTo(cx - r, cy)
                            path.lineTo(cx - ir, cy - ir)
                            path.close()
                            canvas.drawPath(path, fgPaint)
                        }
                        ModuleShape.HEXAGON -> {
                            path.reset()
                            val cx = modRect.centerX()
                            val cy = modRect.centerY()
                            val r = modulePixelSize / 2f
                            val hr = r * 0.866f
                            val qr = r * 0.5f
                            path.moveTo(cx, cy - r)
                            path.lineTo(cx + hr, cy - qr)
                            path.lineTo(cx + hr, cy + qr)
                            path.lineTo(cx, cy + r)
                            path.lineTo(cx - hr, cy + qr)
                            path.lineTo(cx - hr, cy - qr)
                            path.close()
                            canvas.drawPath(path, fgPaint)
                        }
                        ModuleShape.LEAF -> {
                            path.reset()
                            val r = modulePixelSize * 0.45f
                            val radii = floatArrayOf(r, r, 0f, 0f, r, r, 0f, 0f)
                            path.addRoundRect(modRect, radii, Path.Direction.CW)
                            canvas.drawPath(path, fgPaint)
                        }
                    }
                }
            }
        }

        // 4. Render Finder Patterns with exact vector shapes aligned to module coordinates
        val finderPixelDim = 7 * modulePixelSize
        val finderBounds = RectF()

        // Top-Left Finder
        finderBounds.set(startX, startY, startX + finderPixelDim, startY + finderPixelDim)
        renderFinderPattern(canvas, finderBounds, finderShape, fgPaint, bgPaint, path)

        // Top-Right Finder
        finderBounds.set(startX + (totalModsX - 7) * modulePixelSize, startY, startX + totalModsX * modulePixelSize, startY + finderPixelDim)
        renderFinderPattern(canvas, finderBounds, finderShape, fgPaint, bgPaint, path)

        // Bottom-Left Finder
        finderBounds.set(startX, startY + (totalModsY - 7) * modulePixelSize, startX + finderPixelDim, startY + totalModsY * modulePixelSize)
        renderFinderPattern(canvas, finderBounds, finderShape, fgPaint, bgPaint, path)

        // 5. Render Center Logo (NO white background box! Fast cached in-memory drawing)
        if (hasLogo && !config.logoPath.isNullOrBlank()) {
            val logoBmp = LogoBitmapCache.getOrDecode(config.logoPath)
            if (logoBmp != null) {
                if (logoCornerRadiusPx > 0f) {
                    path.reset()
                    path.addRoundRect(clearedAreaRect, logoCornerRadiusPx, logoCornerRadiusPx, Path.Direction.CW)
                    canvas.save()
                    canvas.clipPath(path)
                    canvas.drawBitmap(logoBmp, null, clearedAreaRect, logoPaint)
                    canvas.restore()
                } else {
                    canvas.drawBitmap(logoBmp, null, clearedAreaRect, logoPaint)
                }
            }
        }

        canvas.restore()
        return bitmap
    }

    private val logoPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val subFinderRect = RectF()

    private fun renderFinderPattern(
        canvas: Canvas,
        bounds: RectF,
        shape: FinderShape,
        fgPaint: Paint,
        bgPaint: Paint,
        path: Path
    ) {
        val outerDim = bounds.width()
        val modUnit = outerDim / 7f

        // First paint the 7x7 bounds with background to clear module area
        canvas.drawRect(bounds, bgPaint)

        // Outer 7x7 shape
        drawFinderComponent(canvas, bounds, shape, fgPaint, path)

        // Middle 5x5 cutout
        subFinderRect.set(
            bounds.left + modUnit,
            bounds.top + modUnit,
            bounds.right - modUnit,
            bounds.bottom - modUnit
        )
        drawFinderComponent(canvas, subFinderRect, shape, bgPaint, path)

        // Inner 3x3 core
        subFinderRect.set(
            bounds.left + modUnit * 2f,
            bounds.top + modUnit * 2f,
            bounds.right - modUnit * 2f,
            bounds.bottom - modUnit * 2f
        )
        drawFinderComponent(canvas, subFinderRect, shape, fgPaint, path)
    }

    private fun drawFinderComponent(
        canvas: Canvas,
        rect: RectF,
        shape: FinderShape,
        paint: Paint,
        path: Path
    ) {
        when (shape) {
            FinderShape.SQUARE -> {
                canvas.drawRect(rect, paint)
            }
            FinderShape.ROUNDED -> {
                val radius = rect.width() * 0.32f
                canvas.drawRoundRect(rect, radius, radius, paint)
            }
            FinderShape.CIRCLE -> {
                canvas.drawCircle(rect.centerX(), rect.centerY(), rect.width() / 2f, paint)
            }
            FinderShape.DOT -> {
                canvas.drawRoundRect(rect, rect.width() / 2f, rect.height() / 2f, paint)
            }
            FinderShape.DOUBLE_CIRCLE -> {
                canvas.drawCircle(rect.centerX(), rect.centerY(), rect.width() / 2f, paint)
            }
            FinderShape.LEAF -> {
                val r = rect.width() * 0.45f
                val radii = floatArrayOf(r, r, 0f, 0f, r, r, 0f, 0f)
                path.reset()
                path.addRoundRect(rect, radii, Path.Direction.CW)
                canvas.drawPath(path, paint)
            }
            FinderShape.DIAMOND -> {
                path.reset()
                val cx = rect.centerX()
                val cy = rect.centerY()
                val r = rect.width() / 2f
                path.moveTo(cx, cy - r)
                path.lineTo(cx + r, cy)
                path.lineTo(cx, cy + r)
                path.lineTo(cx - r, cy)
                path.close()
                canvas.drawPath(path, paint)
            }
            FinderShape.HEXAGON -> {
                path.reset()
                val cx = rect.centerX()
                val cy = rect.centerY()
                val r = rect.width() / 2f
                val hr = r * 0.866f
                val qr = r * 0.5f
                path.moveTo(cx, cy - r)
                path.lineTo(cx + hr, cy - qr)
                path.lineTo(cx + hr, cy + qr)
                path.lineTo(cx, cy + r)
                path.lineTo(cx - hr, cy + qr)
                path.lineTo(cx - hr, cy - qr)
                path.close()
                canvas.drawPath(path, paint)
            }
        }
    }
}

object LogoBitmapCache {
    private var cachedPath: String? = null
    private var cachedBitmap: Bitmap? = null

    @Synchronized
    fun getOrDecode(path: String, maxDim: Int = 512): Bitmap? {
        if (path == cachedPath && cachedBitmap != null && !cachedBitmap!!.isRecycled) {
            return cachedBitmap
        }
        return try {
            val file = File(path)
            if (!file.exists()) return null
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, opts)
            var sampleSize = 1
            while (opts.outWidth / (sampleSize * 2) >= maxDim && opts.outHeight / (sampleSize * 2) >= maxDim) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = BitmapFactory.decodeFile(path, decodeOpts)
            cachedPath = path
            cachedBitmap = decoded
            decoded
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @Synchronized
    fun clear() {
        cachedBitmap = null
        cachedPath = null
    }
}
