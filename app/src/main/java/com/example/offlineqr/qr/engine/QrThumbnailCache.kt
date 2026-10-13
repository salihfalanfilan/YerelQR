package com.example.offlineqr.qr.engine

import android.graphics.Bitmap
import android.util.LruCache

object QrThumbnailCache {
    private val cache = object : LruCache<String, Bitmap>(64) {}

    @Synchronized
    fun get(key: String): Bitmap? = cache.get(key)

    @Synchronized
    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }

    @Synchronized
    fun clear() {
        cache.evictAll()
    }
}
