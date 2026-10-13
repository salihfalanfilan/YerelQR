package com.example.offlineqr.data.repository

import com.example.offlineqr.data.local.FolderDao
import com.example.offlineqr.data.mapper.toDomain
import com.example.offlineqr.data.mapper.toEntity
import com.example.offlineqr.domain.model.Folder
import com.example.offlineqr.domain.repository.FolderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderRepositoryImpl @Inject constructor(
    private val folderDao: FolderDao
) : FolderRepository {

    override fun getAllFolders(): Flow<List<Folder>> {
        return folderDao.getAllFoldersWithCount().map { list -> list.map { it.toDomain() } }
    }

    override fun getFolderById(id: Long): Flow<Folder?> {
        return folderDao.getFolderById(id).map { it?.toDomain() }
    }

    override suspend fun saveFolder(folder: Folder): Long {
        return folderDao.insert(folder.toEntity())
    }

    override suspend fun updateFolder(folder: Folder) {
        folderDao.update(folder.toEntity())
    }

    override suspend fun deleteFolder(id: Long) {
        // Atomic deletion that first clears folder references in qr_codes
        folderDao.deleteFolderSafely(id)
    }

    override suspend fun clearAll() {
        folderDao.clearAll()
    }
}
