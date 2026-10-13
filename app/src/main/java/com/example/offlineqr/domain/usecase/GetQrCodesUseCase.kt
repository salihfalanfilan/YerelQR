package com.example.offlineqr.domain.usecase

import com.example.offlineqr.domain.model.QrCode
import com.example.offlineqr.domain.repository.QrRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

enum class QrFilterTab {
    ALL,
    CREATED,
    SCANNED,
    FAVORITES,
    FOLDERS
}

class GetQrCodesUseCase @Inject constructor(
    private val qrRepository: QrRepository
) {
    operator fun invoke(tab: QrFilterTab): Flow<List<QrCode>> {
        return when (tab) {
            QrFilterTab.ALL -> qrRepository.getAllQrCodes()
            QrFilterTab.CREATED -> qrRepository.getCreatedQrCodes()
            QrFilterTab.SCANNED -> qrRepository.getScannedQrCodes()
            QrFilterTab.FAVORITES -> qrRepository.getFavoriteQrCodes()
            QrFilterTab.FOLDERS -> qrRepository.getAllQrCodes()
        }
    }

    fun byFolder(folderId: Long): Flow<List<QrCode>> {
        return qrRepository.getQrCodesByFolder(folderId)
    }

    fun getById(id: Long): Flow<QrCode?> {
        return qrRepository.getQrCodeById(id)
    }

    fun getTotalCount(): Flow<Int> = qrRepository.getTotalCount()
    fun getFavoriteCount(): Flow<Int> = qrRepository.getFavoriteCount()
}
