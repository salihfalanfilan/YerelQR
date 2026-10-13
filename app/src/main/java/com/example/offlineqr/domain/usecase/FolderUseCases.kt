package com.example.offlineqr.domain.usecase

import com.example.offlineqr.domain.model.Folder
import com.example.offlineqr.domain.repository.FolderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FolderUseCases @Inject constructor(
    private val folderRepository: FolderRepository
) {
    fun getAllFolders(): Flow<List<Folder>> = folderRepository.getAllFolders()

    fun getFolderById(id: Long): Flow<Folder?> = folderRepository.getFolderById(id)

    suspend fun createFolder(name: String): Long {
        require(name.isNotBlank()) { "Folder name cannot be empty" }
        return folderRepository.saveFolder(Folder(name = name.trim()))
    }

    suspend fun updateFolder(folder: Folder) {
        require(folder.name.isNotBlank()) { "Folder name cannot be empty" }
        folderRepository.updateFolder(folder.copy(name = folder.name.trim()))
    }

    suspend fun deleteFolder(id: Long) {
        // Safe deletion: removes folder, updates all its items to folderId = null
        folderRepository.deleteFolder(id)
    }
}
