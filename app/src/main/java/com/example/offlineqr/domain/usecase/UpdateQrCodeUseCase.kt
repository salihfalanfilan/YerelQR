package com.example.offlineqr.domain.usecase

import com.example.offlineqr.domain.model.QrCode
import com.example.offlineqr.domain.repository.QrRepository
import javax.inject.Inject

class UpdateQrCodeUseCase @Inject constructor(
    private val qrRepository: QrRepository
) {
    suspend operator fun invoke(qrCode: QrCode) {
        qrRepository.updateQrCode(qrCode)
    }
}
