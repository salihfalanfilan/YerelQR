package com.example.offlineqr.ui.detail

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.offlineqr.domain.model.Folder
import com.example.offlineqr.domain.model.QrCode
import com.example.offlineqr.domain.model.QrType
import com.example.offlineqr.domain.usecase.DeleteQrCodeUseCase
import com.example.offlineqr.domain.usecase.FolderUseCases
import com.example.offlineqr.domain.usecase.GetQrCodesUseCase
import com.example.offlineqr.domain.usecase.MoveToFolderUseCase
import com.example.offlineqr.domain.usecase.ToggleFavoriteUseCase
import com.example.offlineqr.qr.engine.QrCanvasRenderer
import com.example.offlineqr.qr.engine.QrMatrixGenerator
import com.example.offlineqr.qr.engine.QrSvgRenderer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QrDetailUiState(
    val qrCode: QrCode? = null,
    val folders: List<Folder> = emptyList(),
    val bitmap: Bitmap? = null,
    val svgString: String? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class QrDetailViewModel @Inject constructor(
    private val getQrCodesUseCase: GetQrCodesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deleteQrCodeUseCase: DeleteQrCodeUseCase,
    private val moveToFolderUseCase: MoveToFolderUseCase,
    private val folderUseCases: FolderUseCases,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val qrId: Long = savedStateHandle["qrId"] ?: 0L
    private val _bitmap = MutableStateFlow<Bitmap?>(null)
    private val _svgString = MutableStateFlow<String?>(null)

    private val _events = MutableSharedFlow<Unit>()
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    val uiState: StateFlow<QrDetailUiState> = combine(
        getQrCodesUseCase.getById(qrId),
        folderUseCases.getAllFolders(),
        _bitmap,
        _svgString
    ) { qr, folders, bmp, svg ->
        if (qr != null && _bitmap.value == null && qr.type == QrType.OLUSTURULAN) {
            renderBitmap(qr)
        }
        QrDetailUiState(
            qrCode = qr,
            folders = folders,
            bitmap = bmp,
            svgString = svg,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QrDetailUiState(isLoading = true)
    )

    private var renderingKey: Int? = null

    private fun renderBitmap(qrCode: QrCode) {
        val key = (qrCode.content + qrCode.qrConfig.hashCode()).hashCode()
        if (renderingKey == key) return
        renderingKey = key
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            try {
                val matrix = QrMatrixGenerator.generateMatrix(qrCode.content, qrCode.qrConfig, 0)
                _bitmap.value = QrCanvasRenderer.renderToBitmap(matrix, qrCode.qrConfig, 600)
                _svgString.value = QrSvgRenderer.renderToSvg(matrix, qrCode.qrConfig, 1024)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleFavorite() {
        val current = uiState.value.qrCode ?: return
        viewModelScope.launch {
            toggleFavoriteUseCase(current.id, !current.isFavorite)
        }
    }

    fun moveToFolder(folderId: Long?) {
        val current = uiState.value.qrCode ?: return
        viewModelScope.launch {
            moveToFolderUseCase(current.id, folderId)
        }
    }

    fun deleteQrCode() {
        val current = uiState.value.qrCode ?: return
        viewModelScope.launch {
            deleteQrCodeUseCase(current.id)
            _events.emit(Unit)
        }
    }
}
