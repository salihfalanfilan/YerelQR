package com.example.offlineqr

import com.example.offlineqr.domain.model.QrCode
import com.example.offlineqr.domain.model.QrType
import com.example.offlineqr.domain.repository.FolderRepository
import com.example.offlineqr.domain.repository.QrRepository
import com.example.offlineqr.domain.usecase.DeleteQrCodeUseCase
import com.example.offlineqr.domain.usecase.FolderUseCases
import com.example.offlineqr.domain.usecase.GetQrCodesUseCase
import com.example.offlineqr.domain.usecase.MoveToFolderUseCase
import com.example.offlineqr.domain.usecase.QrFilterTab
import com.example.offlineqr.domain.usecase.SaveQrCodeUseCase
import com.example.offlineqr.domain.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class UseCaseTest {

    private lateinit var qrRepository: QrRepository
    private lateinit var folderRepository: FolderRepository

    @Before
    fun setup() {
        qrRepository = mock()
        folderRepository = mock()
    }

    @Test
    fun `test SaveQrCodeUseCase saves valid QR code`() {
        runBlocking {
            val qr = QrCode(content = "https://example.com", type = QrType.OLUSTURULAN)
            whenever(qrRepository.saveQrCode(qr)).thenReturn(101L)

            val useCase = SaveQrCodeUseCase(qrRepository)
            val id = useCase(qr)

            assertEquals(101L, id)
            verify(qrRepository).saveQrCode(qr)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test SaveQrCodeUseCase rejects blank content`() {
        runBlocking {
            val qr = QrCode(content = "   ", type = QrType.OLUSTURULAN)
            val useCase = SaveQrCodeUseCase(qrRepository)
            useCase(qr)
        }
    }

    @Test
    fun `test ToggleFavoriteUseCase calls repository`() {
        runBlocking {
            val useCase = ToggleFavoriteUseCase(qrRepository)
            useCase(42L, true)
            verify(qrRepository).toggleFavorite(42L, true)
        }
    }

    @Test
    fun `test DeleteQrCodeUseCase calls repository`() {
        runBlocking {
            val useCase = DeleteQrCodeUseCase(qrRepository)
            useCase(42L)
            verify(qrRepository).deleteQrCode(42L)
        }
    }

    @Test
    fun `test MoveToFolderUseCase calls repository`() {
        runBlocking {
            val useCase = MoveToFolderUseCase(qrRepository)
            useCase(42L, 7L)
            verify(qrRepository).moveToFolder(42L, 7L)
        }
    }

    @Test
    fun `test GetQrCodesUseCase delegates to proper repository method based on tab`() {
        runBlocking {
            val dummyList = listOf(QrCode(id = 1L, content = "test", type = QrType.OLUSTURULAN))
            whenever(qrRepository.getAllQrCodes()).thenReturn(flowOf(dummyList))
            whenever(qrRepository.getCreatedQrCodes()).thenReturn(flowOf(dummyList))

            val useCase = GetQrCodesUseCase(qrRepository)
            val allResult = useCase(QrFilterTab.ALL).first()
            val createdResult = useCase(QrFilterTab.CREATED).first()

            assertEquals(1, allResult.size)
            assertEquals(1, createdResult.size)
            verify(qrRepository).getAllQrCodes()
            verify(qrRepository).getCreatedQrCodes()
        }
    }

    @Test
    fun `test FolderUseCases delete folder delegates safe deletion`() {
        runBlocking {
            val folderUseCases = FolderUseCases(folderRepository)
            folderUseCases.deleteFolder(5L)
            verify(folderRepository).deleteFolder(5L)
        }
    }

    @Test
    fun `test UpdateQrCodeUseCase delegates to repository`() {
        runBlocking {
            val qr = QrCode(id = 10L, content = "https://updated.com", type = QrType.OLUSTURULAN)
            val useCase = com.example.offlineqr.domain.usecase.UpdateQrCodeUseCase(qrRepository)
            useCase(qr)
            verify(qrRepository).updateQrCode(qr)
        }
    }
}
