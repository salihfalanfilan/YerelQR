package com.example.offlineqr.domain.repository

import com.example.offlineqr.domain.model.Folder
import kotlinx.coroutines.flow.Flow

interface FolderRepository {
    fun getAllFolders(): Flow<List<Folder>>
    fun getFolderById(id: Long): Flow<Folder?>
    suspend fun saveFolder(folder: Folder): Long
    suspend fun updateFolder(folder: Folder)
    suspend fun deleteFolder(id: Long)
    suspend fun clearAll()
}
