package com.example.offlineqr.qr.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

enum class ExportFormat(val extension: String, val mimeType: String, val label: String) {
    PNG("png", "image/png", "PNG (Kayıpsız / HD)"),
    JPG("jpg", "image/jpeg", "JPG (Sıkıştırılmış)"),
    SVG("svg", "image/svg+xml", "SVG (Vektörel Grafik)")
}

object QrExporter {

    /** JPEG has no alpha channel: transparent (rounded) corners would turn black, so composite onto white. */
    private fun flattenForJpeg(src: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(out)
        canvas.drawColor(android.graphics.Color.WHITE)
        canvas.drawBitmap(src, 0f, 0f, null)
        return out
    }

    suspend fun saveToStorage(
        context: Context,
        bitmap: Bitmap?,
        svgString: String?,
        format: ExportFormat,
        title: String = "QR_${System.currentTimeMillis()}"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanTitle = title.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val filename = "$cleanTitle.${format.extension}"
            val resolver = context.contentResolver

            when (format) {
                ExportFormat.PNG, ExportFormat.JPG -> {
                    requireNotNull(bitmap) { "Bitmap required for raster export" }
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                        put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/OfflineQR")
                            put(MediaStore.Images.Media.IS_PENDING, 1)
                        }
                    }

                    val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        ?: throw IllegalStateException("Could not create MediaStore entry")

                    val compressFormat = if (format == ExportFormat.PNG) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                    val quality = if (format == ExportFormat.PNG) 100 else 95
                    val output = if (format == ExportFormat.JPG) flattenForJpeg(bitmap) else bitmap

                    resolver.openOutputStream(imageUri).use { stream ->
                        if (stream == null || !output.compress(compressFormat, quality, stream)) {
                            throw IllegalStateException("Failed to write image to storage")
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(imageUri, contentValues, null, null)
                    }

                    imageUri
                }
                ExportFormat.SVG -> {
                    requireNotNull(svgString) { "SVG string required for vector export" }
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/OfflineQR")
                            put(MediaStore.MediaColumns.IS_PENDING, 1)
                        }
                    }

                    val targetUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    } else {
                        resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                    } ?: throw IllegalStateException("Could not create MediaStore entry for SVG")

                    resolver.openOutputStream(targetUri).use { stream ->
                        if (stream == null) throw IllegalStateException("Failed to open stream for SVG")
                        stream.write(svgString.toByteArray(Charsets.UTF_8))
                        stream.flush()
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(targetUri, contentValues, null, null)
                    }

                    targetUri
                }
            }
        }
    }

    suspend fun saveToGallery(
        context: Context,
        bitmap: Bitmap,
        title: String = "QR_${System.currentTimeMillis()}"
    ): Result<Uri> = saveToStorage(context, bitmap, null, ExportFormat.PNG, title)

    suspend fun shareExport(
        context: Context,
        bitmap: Bitmap?,
        svgString: String?,
        format: ExportFormat,
        textMessage: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cacheFolder = File(context.cacheDir, "shared_qr").apply { mkdirs() }
            val file = File(cacheFolder, "qr_share_${System.currentTimeMillis()}.${format.extension}")

            when (format) {
                ExportFormat.PNG, ExportFormat.JPG -> {
                    requireNotNull(bitmap) { "Bitmap required for raster share" }
                    val compressFormat = if (format == ExportFormat.PNG) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                    val output = if (format == ExportFormat.JPG) flattenForJpeg(bitmap) else bitmap
                    FileOutputStream(file).use { out ->
                        output.compress(compressFormat, 100, out)
                    }
                }
                ExportFormat.SVG -> {
                    requireNotNull(svgString) { "SVG string required for vector share" }
                    FileOutputStream(file).use { out ->
                        out.write(svgString.toByteArray(Charsets.UTF_8))
                    }
                }
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = format.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                if (textMessage.isNotBlank()) {
                    putExtra(Intent.EXTRA_TEXT, textMessage)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "QR Kod Paylaş ($format)").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    suspend fun shareBitmap(
        context: Context,
        bitmap: Bitmap,
        textMessage: String = ""
    ): Result<Unit> = shareExport(context, bitmap, null, ExportFormat.PNG, textMessage)
}
