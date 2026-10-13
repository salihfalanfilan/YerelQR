package com.example.offlineqr.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.offlineqr.R
import com.example.offlineqr.domain.model.Folder
import com.example.offlineqr.domain.model.QrBackupData
import com.example.offlineqr.domain.model.QrBackupItem
import com.example.offlineqr.domain.model.QrCode
import com.example.offlineqr.domain.model.QrType
import com.example.offlineqr.domain.usecase.DeleteQrCodeUseCase
import com.example.offlineqr.domain.usecase.FolderUseCases
import com.example.offlineqr.domain.usecase.GetQrCodesUseCase
import com.example.offlineqr.domain.usecase.MoveToFolderUseCase
import com.example.offlineqr.domain.usecase.QrFilterTab
import com.example.offlineqr.domain.usecase.SaveQrCodeUseCase
import com.example.offlineqr.domain.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.io.File
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val selectedTab: QrFilterTab = QrFilterTab.ALL,
    val searchQuery: String = "",
    val qrCodes: List<QrCode> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val totalActiveCount: Int = 0,
    val favoritesCount: Int = 0,
    val createdCount: Int = 0,
    val scannedCount: Int = 0,
    val isLoading: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getQrCodesUseCase: GetQrCodesUseCase,
    private val saveQrCodeUseCase: SaveQrCodeUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deleteQrCodeUseCase: DeleteQrCodeUseCase,
    private val moveToFolderUseCase: MoveToFolderUseCase,
    private val folderUseCases: FolderUseCases
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(QrFilterTab.ALL)
    val selectedTab: StateFlow<QrFilterTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)

    private val _events = MutableSharedFlow<Long>()
    val events: SharedFlow<Long> = _events.asSharedFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        combine(_selectedTab, _searchQuery, _userMessage) { tab, query, msg -> Triple(tab, query, msg) },
        folderUseCases.getAllFolders(),
        getQrCodesUseCase.getTotalCount(),
        getQrCodesUseCase.getFavoriteCount(),
        _selectedTab.flatMapLatest { tab -> getQrCodesUseCase(tab) }
    ) { (tab, query, msg), folders, totalCount, favCount, qrList ->
        val filtered = if (query.isBlank()) {
            qrList
        } else {
            qrList.filter {
                it.content.contains(query, ignoreCase = true)
            }
        }
        HomeUiState(
            selectedTab = tab,
            searchQuery = query,
            qrCodes = filtered,
            folders = folders,
            totalActiveCount = totalCount,
            favoritesCount = favCount,
            message = msg,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun selectTab(tab: QrFilterTab) {
        _selectedTab.value = tab
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
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

    fun moveToFolder(id: Long, folderId: Long?) {
        viewModelScope.launch {
            moveToFolderUseCase(id, folderId)
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            folderUseCases.createFolder(name)
        }
    }

    fun decodeFromGallery(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
                val scanner = BarcodeScanning.getClient(options)
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        val qr = barcodes.firstOrNull()
                        if (qr != null && !qr.rawValue.isNullOrBlank()) {
                            viewModelScope.launch {
                                val entity = QrCode(
                                    content = qr.rawValue!!,
                                    type = QrType.TARANAN
                                )
                                val id = saveQrCodeUseCase(entity)
                                _events.emit(id)
                            }
                        } else {
                            _userMessage.value = "Seçilen görselde geçerli bir QR kod bulunamadı."
                        }
                    }
                    .addOnFailureListener {
                        _userMessage.value = "Görsel taranamadı: ${it.localizedMessage}"
                    }
            } catch (e: Exception) {
                _userMessage.value = "Görsel yüklenemedi: ${e.localizedMessage}"
            }
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun exportBackup(context: Context) {
        viewModelScope.launch {
            try {
                val allCodes = getQrCodesUseCase(QrFilterTab.ALL).first()
                val allFolders = folderUseCases.getAllFolders().first()
                val folderMap = allFolders.associateBy { it.id }

                val items = allCodes.map { qr ->
                    QrBackupItem(
                        content = qr.content,
                        type = qr.type.name,
                        createdAt = qr.createdAt,
                        isFavorite = qr.isFavorite,
                        folderName = qr.folderId?.let { folderMap[it]?.name },
                        qrConfig = qr.qrConfig
                    )
                }
                val backupData = QrBackupData(items = items)
                val json = Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                }
                val jsonString = json.encodeToString(QrBackupData.serializer(), backupData)

                val fileName = "offline_qr_backup_${System.currentTimeMillis()}.json"
                val file = File(context.cacheDir, fileName)
                file.writeText(jsonString)

                val fileUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, context.getString(R.string.export_btn))
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (e: Exception) {
                _userMessage.value = "Yedek dışa aktarılamadı: ${e.localizedMessage}"
            }
        }
    }

    fun importBackup(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val jsonString = inputStream?.bufferedReader()?.use { it.readText() }
                if (jsonString.isNullOrBlank()) {
                    _userMessage.value = context.getString(R.string.import_backup_error)
                    return@launch
                }
                val json = Json { ignoreUnknownKeys = true }
                val backupData = json.decodeFromString(QrBackupData.serializer(), jsonString)

                if (backupData.items.isEmpty()) {
                    _userMessage.value = "Yedek dosyası boş."
                    return@launch
                }

                val currentFolders = folderUseCases.getAllFolders().first()
                val folderCache = currentFolders.associate { it.name.lowercase() to it.id }.toMutableMap()

                var importedCount = 0
                for (item in backupData.items) {
                    var targetFolderId: Long? = null
                    if (!item.folderName.isNullOrBlank()) {
                        val key = item.folderName.trim().lowercase()
                        targetFolderId = folderCache[key]
                        if (targetFolderId == null) {
                            val newId = folderUseCases.createFolder(item.folderName.trim())
                            folderCache[key] = newId
                            targetFolderId = newId
                        }
                    }

                    val qr = QrCode(
                        content = item.content,
                        type = try { QrType.valueOf(item.type) } catch (e: Exception) { QrType.OLUSTURULAN },
                        createdAt = item.createdAt,
                        isFavorite = item.isFavorite,
                        folderId = targetFolderId,
                        qrConfig = item.qrConfig
                    )
                    saveQrCodeUseCase(qr)
                    importedCount++
                }

                _userMessage.value = context.getString(R.string.import_backup_success, importedCount)
            } catch (e: Exception) {
                _userMessage.value = "${context.getString(R.string.import_backup_error)}: ${e.localizedMessage}"
            }
        }
    }
}
