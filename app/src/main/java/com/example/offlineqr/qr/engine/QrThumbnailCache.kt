package com.example.offlineqr.qr.engine

import android.graphics.Bitmap
import java.util.Collections

/**
 * High-performance, thread-safe in-memory cache for thumbnails, previews, and exports.
 * Implemented using Java LinkedHashMap with LRU eviction to ensure 100% compatibility
 * across both Android ART and JVM unit test environments.
 */
object QrBitmapCache {

    private const val MAX_ENTRIES = 120

    private val cache = Collections.synchronizedMap(
        object : LinkedHashMap<String, Bitmap>(32, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean {
                return size > MAX_ENTRIES
            }
        }
    )

    fun get(key: String): Bitmap? {
        val bmp = cache[key]
        return if (bmp != null && !bmp.isRecycled) bmp else null
    }

    fun put(key: String, bitmap: Bitmap) {
        if (!bitmap.isRecycled) {
            cache[key] = bitmap
        }
    }

    fun clear() {
        cache.clear()
    }
}

/**
 * Backward-compatible alias delegating to the unified [QrBitmapCache].
 */
object QrThumbnailCache {
    fun get(key: String): Bitmap? = QrBitmapCache.get(key)
    fun put(key: String, bitmap: Bitmap) = QrBitmapCache.put(key, bitmap)
    fun clear() = QrBitmapCache.clear()
}
