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
import com.example.offlineqr.domain.model.GradientDirection
import com.example.offlineqr.domain.model.GradientType
import com.example.offlineqr.domain.model.ModuleShape
import com.example.offlineqr.domain.model.QrConfig
import com.example.offlineqr.domain.model.QrFrameStyle
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

        val frameStyle = QrFrameStyle.fromString(config.frameStyle)
        val hasFrame = frameStyle != QrFrameStyle.NONE

        val effectiveQuietZone = config.quietZone.coerceIn(0f, 10f)
        val totalGridUnits = totalModsX.toFloat() + (effectiveQuietZone * 2f)

        val qrScaleFactor = config.qrScale.coerceIn(0.50f, 1.0f)
        val badgeHeight = if (hasFrame) targetSize.toFloat() * 0.12f else 0f
        val gap = if (hasFrame) targetSize.toFloat() * 0.035f else 0f
        val frameScale = if (hasFrame) 0.78f else 1.0f

        val contentBoundingSize = targetSize.toFloat() * qrScaleFactor * frameScale
        val modulePixelSize = contentBoundingSize / totalGridUnits

        val combinedHeight = contentBoundingSize + gap + badgeHeight
        val startX = ((targetSize.toFloat() - contentBoundingSize) / 2f) + (effectiveQuietZone * modulePixelSize)
        val startY = if (hasFrame) {
            when (frameStyle) {
                QrFrameStyle.TOP_BADGE -> {
                    val topOffset = (targetSize.toFloat() - combinedHeight) / 2f
                    topOffset + badgeHeight + gap + (effectiveQuietZone * modulePixelSize)
                }
                else -> {
                    val topOffset = (targetSize.toFloat() - combinedHeight) / 2f
                    topOffset + (effectiveQuietZone * modulePixelSize)
                }
            }
        } else {
            ((targetSize.toFloat() - contentBoundingSize) / 2f) + (effectiveQuietZone * modulePixelSize)
        }

        // 1. Draw Background (Solid, Linear, or Radial Gradient)
        val bgPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        val bgPrimary = config.backgroundColor.toInt()
        val bgSecondary = (config.backgroundSecondaryColor ?: config.backgroundColor).toInt()

        val bgGradType = GradientType.fromString(config.backgroundGradientType)
        val bgGradDir = GradientDirection.fromString(config.backgroundGradientDirection)
        if (bgGradType == GradientType.RADIAL || bgGradDir == GradientDirection.RADIAL) {
            bgPaint.shader = RadialGradient(
                targetSize / 2f, targetSize / 2f, targetSize / 1.3f,
                bgPrimary, bgSecondary,
                Shader.TileMode.CLAMP
            )
        } else if (bgGradType == GradientType.LINEAR) {
            val bgW = targetSize.toFloat()
            val bgH = targetSize.toFloat()
            val (x1, y1, x2, y2) = when (bgGradDir) {
                GradientDirection.VERTICAL, GradientDirection.TOP_TO_BOTTOM -> listOf(0f, 0f, 0f, bgH)
                GradientDirection.HORIZONTAL, GradientDirection.LEFT_TO_RIGHT -> listOf(0f, 0f, bgW, 0f)
                GradientDirection.DIAGONAL -> listOf(0f, 0f, bgW, bgH)
                GradientDirection.REVERSE_DIAGONAL -> listOf(bgW, 0f, 0f, bgH)
                GradientDirection.BOTTOM_TO_TOP -> listOf(0f, bgH, 0f, 0f)
                GradientDirection.RIGHT_TO_LEFT -> listOf(bgW, 0f, 0f, 0f)
                GradientDirection.RADIAL -> listOf(0f, 0f, bgW, bgH)
            }
            bgPaint.shader = LinearGradient(
                x1, y1, x2, y2,
                bgPrimary, bgSecondary,
                Shader.TileMode.CLAMP
            )
        } else {
            bgPaint.color = bgPrimary
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

        val fgGradType = GradientType.fromString(config.gradientType)
        val fgGradDir = GradientDirection.fromString(config.gradientDirection)
        if (fgGradType == GradientType.RADIAL || fgGradDir == GradientDirection.RADIAL) {
            val cx = startX + (totalModsX * modulePixelSize) / 2f
            val cy = startY + (totalModsY * modulePixelSize) / 2f
            val r = (totalModsX * modulePixelSize) / 1.35f
            fgPaint.shader = RadialGradient(
                cx, cy, r,
                primaryColor, secondaryColor,
                Shader.TileMode.CLAMP
            )
        } else if (fgGradType == GradientType.LINEAR) {
            val fgLeft = startX
            val fgTop = startY
            val fgRight = startX + totalModsX * modulePixelSize
            val fgBottom = startY + totalModsY * modulePixelSize
            val (x1, y1, x2, y2) = when (fgGradDir) {
                GradientDirection.VERTICAL, GradientDirection.TOP_TO_BOTTOM -> listOf(fgLeft, fgTop, fgLeft, fgBottom)
                GradientDirection.HORIZONTAL, GradientDirection.LEFT_TO_RIGHT -> listOf(fgLeft, fgTop, fgRight, fgTop)
                GradientDirection.DIAGONAL -> listOf(fgLeft, fgTop, fgRight, fgBottom)
                GradientDirection.REVERSE_DIAGONAL -> listOf(fgRight, fgTop, fgLeft, fgBottom)
                GradientDirection.BOTTOM_TO_TOP -> listOf(fgLeft, fgBottom, fgLeft, fgTop)
                GradientDirection.RIGHT_TO_LEFT -> listOf(fgRight, fgTop, fgLeft, fgTop)
                GradientDirection.RADIAL -> listOf(fgLeft, fgTop, fgRight, fgBottom)
            }
            fgPaint.shader = LinearGradient(
                x1, y1, x2, y2,
                primaryColor, secondaryColor,
                Shader.TileMode.CLAMP
            )
        } else {
            fgPaint.color = primaryColor
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
        val leafRadii = FloatArray(8)
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
                            leafRadii[0] = r
                            leafRadii[1] = r
                            leafRadii[2] = 0f
                            leafRadii[3] = 0f
                            leafRadii[4] = r
                            leafRadii[5] = r
                            leafRadii[6] = 0f
                            leafRadii[7] = 0f
                            path.addRoundRect(modRect, leafRadii, Path.Direction.CW)
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

        // 6. Render Frame & Text Badge if enabled
        if (hasFrame) {
            val frameColorInt = (config.frameColor ?: config.foregroundColor).toInt()
            val badgeWidth = (contentBoundingSize * 1.04f).coerceAtMost(targetSize * 0.92f)
            val badgeLeft = (targetSize.toFloat() - badgeWidth) / 2f
            val badgeRight = badgeLeft + badgeWidth
            val badgeTop = when (frameStyle) {
                QrFrameStyle.TOP_BADGE -> (targetSize.toFloat() - combinedHeight) / 2f
                else -> (targetSize.toFloat() - combinedHeight) / 2f + contentBoundingSize + gap
            }
            val badgeBottom = badgeTop + badgeHeight
            val badgeRect = RectF(badgeLeft, badgeTop, badgeRight, badgeBottom)

            val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = frameColorInt
            }

            when (frameStyle) {
                QrFrameStyle.BOTTOM_BADGE, QrFrameStyle.TOP_BADGE -> {
                    val badgeRadius = badgeHeight * 0.35f
                    canvas.drawRoundRect(badgeRect, badgeRadius, badgeRadius, framePaint)
                }
                QrFrameStyle.ROUNDED_FRAME -> {
                    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = (targetSize * 0.012f).coerceAtLeast(6f)
                        color = frameColorInt
                    }
                    val outerMargin = strokePaint.strokeWidth * 1.5f
                    val topBorder = (targetSize.toFloat() - combinedHeight) / 2f - outerMargin
                    val borderRect = RectF(badgeLeft - outerMargin, topBorder, badgeRight + outerMargin, badgeBottom + outerMargin)
                    val borderRadius = badgeHeight * 0.35f
                    canvas.drawRoundRect(borderRect, borderRadius, borderRadius, strokePaint)
                    canvas.drawRoundRect(badgeRect, badgeHeight * 0.25f, badgeHeight * 0.25f, framePaint)
                }
                QrFrameStyle.BOX_BORDER -> {
                    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = (targetSize * 0.012f).coerceAtLeast(6f)
                        color = frameColorInt
                    }
                    val outerMargin = strokePaint.strokeWidth * 1.5f
                    val topBorder = (targetSize.toFloat() - combinedHeight) / 2f - outerMargin
                    val borderRect = RectF(badgeLeft - outerMargin, topBorder, badgeRight + outerMargin, badgeBottom + outerMargin)
                    canvas.drawRect(borderRect, strokePaint)
                    canvas.drawRect(badgeRect, framePaint)
                }
                else -> Unit
            }

            if (config.frameText.isNotBlank()) {
                val text = config.frameText.trim()
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = config.frameTextColor.toInt()
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                val maxTextWidth = badgeRect.width() * 0.88f
                val maxTextHeight = badgeHeight * 0.52f
                val testSize = maxTextHeight
                textPaint.textSize = testSize
                val textWidth = textPaint.measureText(text)
                if (textWidth > maxTextWidth && textWidth > 0f) {
                    textPaint.textSize = (testSize * (maxTextWidth / textWidth)).coerceAtLeast(12f)
                }
                val fontMetrics = textPaint.fontMetrics
                val baselineY = badgeRect.centerY() - (fontMetrics.descent + fontMetrics.ascent) / 2f
                canvas.drawText(text, badgeRect.centerX(), baselineY, textPaint)
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
