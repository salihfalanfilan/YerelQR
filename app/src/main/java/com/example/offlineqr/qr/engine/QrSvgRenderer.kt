package com.example.offlineqr.qr.engine

import com.example.offlineqr.domain.model.FinderShape
import com.example.offlineqr.domain.model.GradientType
import com.example.offlineqr.domain.model.ModuleShape
import com.example.offlineqr.domain.model.QrConfig
import com.google.zxing.common.BitMatrix
import java.util.Locale

object QrSvgRenderer {

    fun renderToSvg(
        bitMatrix: BitMatrix,
        config: QrConfig,
        targetSize: Int = 1024
    ): String {
        val totalModsX = bitMatrix.width
        val totalModsY = bitMatrix.height

        val effectiveQuietZone = config.quietZone.coerceIn(0f, 10f)
        val totalGridUnits = totalModsX.toFloat() + (effectiveQuietZone * 2f)

        val qrScaleFactor = config.qrScale.coerceIn(0.50f, 1.0f)
        val contentBoundingSize = targetSize.toFloat() * qrScaleFactor
        val modulePixelSize = contentBoundingSize / totalGridUnits

        val startX = ((targetSize.toFloat() - contentBoundingSize) / 2f) + (effectiveQuietZone * modulePixelSize)
        val startY = ((targetSize.toFloat() - contentBoundingSize) / 2f) + (effectiveQuietZone * modulePixelSize)

        val fgPrimaryHex = String.format(Locale.US, "#%06X", 0xFFFFFF and config.foregroundColor.toInt())
        val fgSecondaryHex = String.format(Locale.US, "#%06X", 0xFFFFFF and (config.secondaryColor ?: config.foregroundColor).toInt())
        val bgPrimaryHex = String.format(Locale.US, "#%06X", 0xFFFFFF and config.backgroundColor.toInt())
        val bgSecondaryHex = String.format(Locale.US, "#%06X", 0xFFFFFF and (config.backgroundSecondaryColor ?: config.backgroundColor).toInt())

        val fgGradType = GradientType.fromString(config.gradientType)
        val bgGradType = GradientType.fromString(config.backgroundGradientType)

        val sb = StringBuilder()
        sb.append("""<svg xmlns="http://www.w3.org/2000/svg" width="$targetSize" height="$targetSize" viewBox="0 0 $targetSize $targetSize">""").append("\n")

        // Definitions: Gradients
        sb.append("<defs>\n")
        if (fgGradType == GradientType.LINEAR) {
            sb.append("""  <linearGradient id="fgGrad" x1="0%" y1="0%" x2="100%" y2="100%">""").append("\n")
            sb.append("""    <stop offset="0%" stop-color="$fgPrimaryHex" />""").append("\n")
            sb.append("""    <stop offset="100%" stop-color="$fgSecondaryHex" />""").append("\n")
            sb.append("  </linearGradient>\n")
        } else if (fgGradType == GradientType.RADIAL) {
            sb.append("""  <radialGradient id="fgGrad" cx="50%" cy="50%" r="70%">""").append("\n")
            sb.append("""    <stop offset="0%" stop-color="$fgPrimaryHex" />""").append("\n")
            sb.append("""    <stop offset="100%" stop-color="$fgSecondaryHex" />""").append("\n")
            sb.append("  </radialGradient>\n")
        }

        if (bgGradType == GradientType.LINEAR) {
            sb.append("""  <linearGradient id="bgGrad" x1="0%" y1="0%" x2="100%" y2="100%">""").append("\n")
            sb.append("""    <stop offset="0%" stop-color="$bgPrimaryHex" />""").append("\n")
            sb.append("""    <stop offset="100%" stop-color="$bgSecondaryHex" />""").append("\n")
            sb.append("  </linearGradient>\n")
        } else if (bgGradType == GradientType.RADIAL) {
            sb.append("""  <radialGradient id="bgGrad" cx="50%" cy="50%" r="70%">""").append("\n")
            sb.append("""    <stop offset="0%" stop-color="$bgPrimaryHex" />""").append("\n")
            sb.append("""    <stop offset="100%" stop-color="$bgSecondaryHex" />""").append("\n")
            sb.append("  </radialGradient>\n")
        }
        val bgCornerPx = targetSize * config.backgroundCornerRadius.coerceIn(0f, 0.5f)
        if (bgCornerPx > 0f) {
            sb.append(String.format(Locale.US, """  <clipPath id="cardClip"><rect width="100%%" height="100%%" rx="%.2f" ry="%.2f" /></clipPath>%n""", bgCornerPx, bgCornerPx))
        }
        sb.append("</defs>\n")

        if (bgCornerPx > 0f) {
            sb.append("""<g clip-path="url(#cardClip)">""").append("\n")
        }

        // Background
        val bgFill = if (bgGradType != GradientType.NONE) "url(#bgGrad)" else bgPrimaryHex
        val fgFill = if (fgGradType != GradientType.NONE) "url(#fgGrad)" else fgPrimaryHex
        if (bgCornerPx > 0f) {
            sb.append(String.format(Locale.US, """<rect width="100%%" height="100%%" rx="%.2f" ry="%.2f" fill="%s" />%n""", bgCornerPx, bgCornerPx, bgFill))
        } else {
            sb.append("""<rect width="100%" height="100%" fill="$bgFill" />""").append("\n")
        }

        // Finder checks
        fun inFinderArea(col: Int, row: Int): Boolean {
            val inTopLeft = col in 0..6 && row in 0..6
            val inTopRight = col in (totalModsX - 7)..<totalModsX && row in 0..6
            val inBottomLeft = col in 0..6 && row in (totalModsY - 7)..<totalModsY
            return inTopLeft || inTopRight || inBottomLeft
        }

        // Center Logo bounds
        val hasLogo = !config.logoPath.isNullOrBlank()
        val logoFraction = config.logoScale.coerceIn(0.08f, 0.35f)
        val qrPixelWidth = totalModsX * modulePixelSize
        val logoBoxSize = qrPixelWidth * logoFraction
        val logoBoxLeft = startX + (qrPixelWidth - logoBoxSize) / 2f
        val logoBoxTop = startY + (qrPixelWidth - logoBoxSize) / 2f
        val logoBoxRight = logoBoxLeft + logoBoxSize
        val logoBoxBottom = logoBoxTop + logoBoxSize
        val logoCornerRadiusPx = if (hasLogo) {
            logoBoxSize * config.logoCornerRadius.coerceIn(0f, 0.5f)
        } else 0f

        fun isPointInRoundedRectSvg(px: Float, py: Float, left: Float, top: Float, right: Float, bottom: Float, radius: Float): Boolean {
            if (px < left || px > right || py < top || py > bottom) return false
            if (radius <= 0f) return true

            val inLeftCorner = px < left + radius
            val inRightCorner = px > right - radius
            val inTopCorner = py < top + radius
            val inBottomCorner = py > bottom - radius

            return when {
                inLeftCorner && inTopCorner -> {
                    val dx = px - (left + radius)
                    val dy = py - (top + radius)
                    dx * dx + dy * dy <= radius * radius
                }
                inRightCorner && inTopCorner -> {
                    val dx = px - (right - radius)
                    val dy = py - (top + radius)
                    dx * dx + dy * dy <= radius * radius
                }
                inLeftCorner && inBottomCorner -> {
                    val dx = px - (left + radius)
                    val dy = py - (bottom - radius)
                    dx * dx + dy * dy <= radius * radius
                }
                inRightCorner && inBottomCorner -> {
                    val dx = px - (right - radius)
                    val dy = py - (bottom - radius)
                    dx * dx + dy * dy <= radius * radius
                }
                else -> true
            }
        }

        fun inLogoArea(x: Float, y: Float, w: Float, h: Float): Boolean {
            if (!hasLogo) return false
            if (config.logoCutoutFillBackground) {
                return x < logoBoxRight && x + w > logoBoxLeft && y < logoBoxBottom && y + h > logoBoxTop
            } else {
                val cx = x + w / 2f
                val cy = y + h / 2f
                return isPointInRoundedRectSvg(cx, cy, logoBoxLeft, logoBoxTop, logoBoxRight, logoBoxBottom, logoCornerRadiusPx)
            }
        }

        val moduleShape = ModuleShape.fromString(config.moduleShape)

        // Draw Data Modules
        for (row in 0 until totalModsY) {
            for (col in 0 until totalModsX) {
                if (inFinderArea(col, row)) continue

                if (bitMatrix.get(col, row)) {
                    val left = startX + col * modulePixelSize
                    val top = startY + row * modulePixelSize

                    if (inLogoArea(left, top, modulePixelSize, modulePixelSize)) continue

                    when (moduleShape) {
                        ModuleShape.SQUARE -> {
                            sb.append(String.format(Locale.US, """<rect x="%.2f" y="%.2f" width="%.2f" height="%.2f" fill="%s" shape-rendering="crispEdges" />%n""",
                                left, top, modulePixelSize + 0.3f, modulePixelSize + 0.3f, fgFill))
                        }
                        ModuleShape.PILLS -> {
                            val r = modulePixelSize * 0.35f
                            sb.append(String.format(Locale.US, """<rect x="%.2f" y="%.2f" width="%.2f" height="%.2f" rx="%.2f" ry="%.2f" fill="%s" />%n""",
                                left, top, modulePixelSize, modulePixelSize, r, r, fgFill))
                        }
                        ModuleShape.CIRCLES -> {
                            val cx = left + modulePixelSize / 2f
                            val cy = top + modulePixelSize / 2f
                            val r = (modulePixelSize / 2f) * 0.90f
                            sb.append(String.format(Locale.US, """<circle cx="%.2f" cy="%.2f" r="%.2f" fill="%s" />%n""", cx, cy, r, fgFill))
                        }
                        ModuleShape.DIAMOND -> {
                            val cx = left + modulePixelSize / 2f
                            val cy = top + modulePixelSize / 2f
                            val r = modulePixelSize / 2f
                            sb.append(String.format(Locale.US, """<polygon points="%.2f,%.2f %.2f,%.2f %.2f,%.2f %.2f,%.2f" fill="%s" />%n""",
                                cx, cy - r, cx + r, cy, cx, cy + r, cx - r, cy, fgFill))
                        }
                        ModuleShape.STAR -> {
                            val cx = left + modulePixelSize / 2f
                            val cy = top + modulePixelSize / 2f
                            val r = modulePixelSize / 2f
                            val ir = r * 0.38f
                            sb.append(String.format(Locale.US, """<polygon points="%.2f,%.2f %.2f,%.2f %.2f,%.2f %.2f,%.2f %.2f,%.2f %.2f,%.2f %.2f,%.2f %.2f,%.2f" fill="%s" />%n""",
                                cx, cy - r, cx + ir, cy - ir, cx + r, cy, cx + ir, cy + ir, cx, cy + r, cx - ir, cy + ir, cx - r, cy, cx - ir, cy - ir, fgFill))
                        }
                        ModuleShape.HEXAGON -> {
                            val cx = left + modulePixelSize / 2f
                            val cy = top + modulePixelSize / 2f
                            val r = modulePixelSize / 2f
                            val hr = r * 0.866f
                            val qr = r * 0.5f
                            sb.append(String.format(Locale.US, """<polygon points="%.2f,%.2f %.2f,%.2f %.2f,%.2f %.2f,%.2f %.2f,%.2f %.2f,%.2f" fill="%s" />%n""",
                                cx, cy - r, cx + hr, cy - qr, cx + hr, cy + qr, cx, cy + r, cx - hr, cy + qr, cx - hr, cy - qr, fgFill))
                        }
                        ModuleShape.LEAF -> {
                            val r = modulePixelSize * 0.45f
                            sb.append(String.format(Locale.US, """<path d="M %.2f,%.2f a %.2f,%.2f 0 0 1 %.2f,%.2f l 0,%.2f a %.2f,%.2f 0 0 1 -%.2f,-%.2f z" fill="%s" />%n""",
                                left + r, top, r, r, -r, r, modulePixelSize - r, r, r, -r, -r, fgFill))
                        }
                    }
                }
            }
        }

        // Render Finder Patterns
        val finderShape = FinderShape.fromString(config.finderShape)
        val finderDim = 7 * modulePixelSize
        val finders = listOf(
            Triple(startX, startY, finderDim),
            Triple(startX + (totalModsX - 7) * modulePixelSize, startY, finderDim),
            Triple(startX, startY + (totalModsY - 7) * modulePixelSize, finderDim)
        )

        for ((fx, fy, fdim) in finders) {
            val mod = fdim / 7f
            // 7x7 outer
            appendFinderComponentSvg(sb, fx, fy, fdim, finderShape, fgFill)
            // 5x5 cutout
            appendFinderComponentSvg(sb, fx + mod, fy + mod, fdim - 2 * mod, finderShape, bgFill)
            // 3x3 core
            appendFinderComponentSvg(sb, fx + 2 * mod, fy + 2 * mod, fdim - 4 * mod, finderShape, fgFill)
        }

        if (bgCornerPx > 0f) {
            sb.append("</g>\n")
        }

        sb.append("</svg>")
        return sb.toString()
    }

    private fun appendFinderComponentSvg(
        sb: StringBuilder,
        x: Float,
        y: Float,
        dim: Float,
        shape: FinderShape,
        fill: String
    ) {
        when (shape) {
            FinderShape.SQUARE -> {
                sb.append(String.format(Locale.US, """  <rect x="%.2f" y="%.2f" width="%.2f" height="%.2f" fill="%s" />%n""", x, y, dim, dim, fill))
            }
            FinderShape.ROUNDED -> {
                val r = dim * 0.32f
                sb.append(String.format(Locale.US, """  <rect x="%.2f" y="%.2f" width="%.2f" height="%.2f" rx="%.2f" ry="%.2f" fill="%s" />%n""", x, y, dim, dim, r, r, fill))
            }
            FinderShape.CIRCLE, FinderShape.DOUBLE_CIRCLE -> {
                val cx = x + dim / 2f
                val cy = y + dim / 2f
                val r = dim / 2f
                sb.append(String.format(Locale.US, """  <circle cx="%.2f" cy="%.2f" r="%.2f" fill="%s" />%n""", cx, cy, r, fill))
            }
            FinderShape.DOT -> {
                val r = dim / 2f
                sb.append(String.format(Locale.US, """  <rect x="%.2f" y="%.2f" width="%.2f" height="%.2f" rx="%.2f" ry="%.2f" fill="%s" />%n""", x, y, dim, dim, r, r, fill))
            }
            FinderShape.DIAMOND -> {
                val cx = x + dim / 2f
                val cy = y + dim / 2f
                val r = dim / 2f
                sb.append(String.format(Locale.US, """  <polygon points="%.2f,%.2f %.2f,%.2f %.2f,%.2f %.2f,%.2f" fill="%s" />%n""",
                    cx, cy - r, cx + r, cy, cx, cy + r, cx - r, cy, fill))
            }
            FinderShape.HEXAGON -> {
                val cx = x + dim / 2f
                val cy = y + dim / 2f
                val r = dim / 2f
                val hr = r * 0.866f
                val qr = r * 0.5f
                sb.append(String.format(Locale.US, """  <polygon points="%.2f,%.2f %.2f,%.2f %.2f,%.2f %.2f,%.2f %.2f,%.2f %.2f,%.2f" fill="%s" />%n""",
                    cx, cy - r, cx + hr, cy - qr, cx + hr, cy + qr, cx, cy + r, cx - hr, cy + qr, cx - hr, cy - qr, fill))
            }
            FinderShape.LEAF -> {
                val r = dim * 0.45f
                sb.append(String.format(Locale.US, """  <rect x="%.2f" y="%.2f" width="%.2f" height="%.2f" rx="%.2f" ry="%.2f" fill="%s" />%n""", x, y, dim, dim, r, r, fill))
            }
        }
    }
}
