package com.example.offlineqr.domain.usecase

import com.example.offlineqr.domain.model.QrCode
import com.example.offlineqr.domain.repository.QrRepository
import javax.inject.Inject

class SaveQrCodeUseCase @Inject constructor(
    private val qrRepository: QrRepository
) {
    suspend operator fun invoke(qrCode: QrCode): Long {
        require(qrCode.content.isNotBlank()) { "QR content cannot be empty" }
        return qrRepository.saveQrCode(qrCode)
    }
}
