package com.example.offlineqr.data.mapper

import com.example.offlineqr.data.entity.QRCodeEntity
import com.example.offlineqr.domain.model.QrCode
import com.example.offlineqr.domain.model.QrPayloadType
import com.example.offlineqr.domain.model.QrType

fun QRCodeEntity.toDomain(): QrCode {
    val qType = try {
        QrType.valueOf(type)
    } catch (e: Exception) {
        QrType.OLUSTURULAN
    }
    return QrCode(
        id = id,
        content = content,
        type = qType,
        createdAt = createdAt,
        isFavorite = isFavorite,
        folderId = folderId,
        qrConfig = qrConfig,
        payloadType = QrPayloadType.fromContent(content)
    )
}

fun QrCode.toEntity(): QRCodeEntity {
    return QRCodeEntity(
        id = id,
        content = content,
        type = type.name,
        createdAt = createdAt,
        isFavorite = isFavorite,
        folderId = folderId,
        qrConfig = qrConfig
    )
}
