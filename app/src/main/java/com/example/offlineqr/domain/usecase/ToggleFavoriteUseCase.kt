package com.example.offlineqr.domain.usecase

import com.example.offlineqr.domain.repository.QrRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val qrRepository: QrRepository
) {
    suspend operator fun invoke(id: Long, isFavorite: Boolean) {
        qrRepository.toggleFavorite(id, isFavorite)
    }
}
