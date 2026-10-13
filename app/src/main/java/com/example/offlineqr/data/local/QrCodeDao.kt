package com.example.offlineqr.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.offlineqr.data.entity.QRCodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QrCodeDao {

    @Query("SELECT * FROM qr_codes ORDER BY createdAt DESC")
    fun getAllQrCodes(): Flow<List<QRCodeEntity>>

    @Query("SELECT * FROM qr_codes WHERE type = 'OLUSTURULAN' ORDER BY createdAt DESC")
    fun getCreatedQrCodes(): Flow<List<QRCodeEntity>>

    @Query("SELECT * FROM qr_codes WHERE type = 'TARANAN' ORDER BY createdAt DESC")
    fun getScannedQrCodes(): Flow<List<QRCodeEntity>>

    @Query("SELECT * FROM qr_codes WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteQrCodes(): Flow<List<QRCodeEntity>>

    @Query("SELECT * FROM qr_codes WHERE folderId = :folderId ORDER BY createdAt DESC")
    fun getQrCodesByFolder(folderId: Long): Flow<List<QRCodeEntity>>

    @Query("SELECT * FROM qr_codes WHERE folderId IS NULL ORDER BY createdAt DESC")
    fun getUncategorizedQrCodes(): Flow<List<QRCodeEntity>>

    @Query("SELECT * FROM qr_codes WHERE id = :id LIMIT 1")
    fun getQrCodeById(id: Long): Flow<QRCodeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(qrCode: QRCodeEntity): Long

    @Update
    suspend fun update(qrCode: QRCodeEntity)

    @Query("DELETE FROM qr_codes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE qr_codes SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE qr_codes SET folderId = :folderId WHERE id = :id")
    suspend fun updateFolder(id: Long, folderId: Long?)

    @Query("UPDATE qr_codes SET folderId = NULL WHERE folderId = :folderId")
    suspend fun clearFolderReferences(folderId: Long)

    @Query("DELETE FROM qr_codes")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM qr_codes")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM qr_codes WHERE isFavorite = 1")
    fun getFavoriteCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM qr_codes WHERE folderId = :folderId")
    fun getCountByFolder(folderId: Long): Flow<Int>
}
