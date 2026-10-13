package com.example.offlineqr.ui.folders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.offlineqr.domain.model.Folder
import com.example.offlineqr.domain.model.QrCode
import com.example.offlineqr.domain.usecase.DeleteQrCodeUseCase
import com.example.offlineqr.domain.usecase.FolderUseCases
import com.example.offlineqr.domain.usecase.GetQrCodesUseCase
import com.example.offlineqr.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FolderDetailUiState(
    val folder: Folder? = null,
    val qrCodes: List<QrCode> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class FolderViewModel @Inject constructor(
    private val folderUseCases: FolderUseCases,
    private val getQrCodesUseCase: GetQrCodesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deleteQrCodeUseCase: DeleteQrCodeUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val folderId: Long = savedStateHandle["folderId"] ?: 0L

    val uiState: StateFlow<FolderDetailUiState> = if (folderId > 0) {
        combine(
            folderUseCases.getFolderById(folderId),
            getQrCodesUseCase.byFolder(folderId)
        ) { folder, qrCodes ->
            FolderDetailUiState(
                folder = folder,
                qrCodes = qrCodes,
                isLoading = false
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FolderDetailUiState(isLoading = true)
        )
    } else {
        MutableStateFlow(FolderDetailUiState()).asStateFlow()
    }

    private val _events = MutableSharedFlow<Unit>()
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    fun renameFolder(newName: String) {
        val currentFolder = uiState.value.folder ?: return
        viewModelScope.launch {
            if (newName.isNotBlank()) {
                folderUseCases.updateFolder(currentFolder.copy(name = newName.trim()))
            }
        }
    }

    fun deleteFolder() {
        if (folderId > 0) {
            viewModelScope.launch {
                folderUseCases.deleteFolder(folderId)
                _events.emit(Unit)
            }
        }
    }

    fun toggleFavorite(id: Long, current: Boolean) {
        viewModelScope.launch {
            toggleFavoriteUseCase(id, !current)
        }
    }

    fun deleteQrCode(id: Long) {
        viewModelScope.launch {
            deleteQrCodeUseCase(id)
        }
    }
}
