package com.example.offlineqr.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.offlineqr.domain.model.QrConfig

@Entity(
    tableName = "qr_codes",
    indices = [
        Index("createdAt"),
        Index("type"),
        Index("isFavorite"),
        Index("folderId")
    ]
)
data class QRCodeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val type: String, // "OLUSTURULAN" or "TARANAN"
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val folderId: Long? = null,
    val qrConfig: QrConfig
)
