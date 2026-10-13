package com.example.offlineqr.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.offlineqr.data.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

data class FolderWithCount(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val itemCount: Int
)

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders ORDER BY createdAt DESC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Query(
        """
        SELECT f.id, f.name, f.createdAt, COUNT(q.id) as itemCount
        FROM folders f
        LEFT JOIN qr_codes q ON q.folderId = f.id
        GROUP BY f.id
        ORDER BY f.createdAt DESC
        """
    )
    fun getAllFoldersWithCount(): Flow<List<FolderWithCount>>

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    fun getFolderById(id: Long): Flow<FolderEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: FolderEntity): Long

    @Update
    suspend fun update(folder: FolderEntity)

    @Query("UPDATE qr_codes SET folderId = NULL WHERE folderId = :folderId")
    suspend fun clearFolderReferences(folderId: Long)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun delete(id: Long)

    @Transaction
    suspend fun deleteFolderSafely(id: Long) {
        clearFolderReferences(id)
        delete(id)
    }

    @Query("DELETE FROM folders")
    suspend fun clearAll()
}
