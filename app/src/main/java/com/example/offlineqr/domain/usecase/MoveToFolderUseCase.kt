package com.example.offlineqr.domain.usecase

import com.example.offlineqr.domain.repository.QrRepository
import javax.inject.Inject

class MoveToFolderUseCase @Inject constructor(
    private val qrRepository: QrRepository
) {
    suspend operator fun invoke(id: Long, folderId: Long?) {
        qrRepository.moveToFolder(id, folderId)
    }
}
