package com.example.offlineqr.domain.repository

import com.example.offlineqr.domain.model.QrCode
import kotlinx.coroutines.flow.Flow

interface QrRepository {
    fun getAllQrCodes(): Flow<List<QrCode>>
    fun getCreatedQrCodes(): Flow<List<QrCode>>
    fun getScannedQrCodes(): Flow<List<QrCode>>
    fun getFavoriteQrCodes(): Flow<List<QrCode>>
    fun getQrCodesByFolder(folderId: Long): Flow<List<QrCode>>
    fun getUncategorizedQrCodes(): Flow<List<QrCode>>
    fun getQrCodeById(id: Long): Flow<QrCode?>
    suspend fun saveQrCode(qrCode: QrCode): Long
    suspend fun updateQrCode(qrCode: QrCode)
    suspend fun deleteQrCode(id: Long)
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)
    suspend fun moveToFolder(id: Long, folderId: Long?)
    suspend fun clearAll()
    fun getTotalCount(): Flow<Int>
    fun getFavoriteCount(): Flow<Int>
}
