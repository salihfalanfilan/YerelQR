package com.example.offlineqr.data.repository

import com.example.offlineqr.data.local.QrCodeDao
import com.example.offlineqr.data.mapper.toDomain
import com.example.offlineqr.data.mapper.toEntity
import com.example.offlineqr.domain.model.QrCode
import com.example.offlineqr.domain.repository.QrRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QrRepositoryImpl @Inject constructor(
    private val qrCodeDao: QrCodeDao
) : QrRepository {

    override fun getAllQrCodes(): Flow<List<QrCode>> {
        return qrCodeDao.getAllQrCodes().map { list -> list.map { it.toDomain() } }
    }

    override fun getCreatedQrCodes(): Flow<List<QrCode>> {
        return qrCodeDao.getCreatedQrCodes().map { list -> list.map { it.toDomain() } }
    }

    override fun getScannedQrCodes(): Flow<List<QrCode>> {
        return qrCodeDao.getScannedQrCodes().map { list -> list.map { it.toDomain() } }
    }

    override fun getFavoriteQrCodes(): Flow<List<QrCode>> {
        return qrCodeDao.getFavoriteQrCodes().map { list -> list.map { it.toDomain() } }
    }

    override fun getQrCodesByFolder(folderId: Long): Flow<List<QrCode>> {
        return qrCodeDao.getQrCodesByFolder(folderId).map { list -> list.map { it.toDomain() } }
    }

    override fun getUncategorizedQrCodes(): Flow<List<QrCode>> {
        return qrCodeDao.getUncategorizedQrCodes().map { list -> list.map { it.toDomain() } }
    }

    override fun getQrCodeById(id: Long): Flow<QrCode?> {
        return qrCodeDao.getQrCodeById(id).map { it?.toDomain() }
    }

    override suspend fun saveQrCode(qrCode: QrCode): Long {
        return qrCodeDao.insert(qrCode.toEntity())
    }

    override suspend fun updateQrCode(qrCode: QrCode) {
        qrCodeDao.update(qrCode.toEntity())
    }

    override suspend fun deleteQrCode(id: Long) {
        qrCodeDao.delete(id)
    }

    override suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        qrCodeDao.setFavorite(id, isFavorite)
    }

    override suspend fun moveToFolder(id: Long, folderId: Long?) {
        qrCodeDao.updateFolder(id, folderId)
    }

    override suspend fun clearAll() {
        qrCodeDao.clearAll()
    }

    override fun getTotalCount(): Flow<Int> {
        return qrCodeDao.getTotalCount()
    }

    override fun getFavoriteCount(): Flow<Int> {
        return qrCodeDao.getFavoriteCount()
    }
}
