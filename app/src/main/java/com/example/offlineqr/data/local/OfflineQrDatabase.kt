package com.example.offlineqr.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.offlineqr.data.entity.FolderEntity
import com.example.offlineqr.data.entity.QRCodeEntity

@Database(
    entities = [
        QRCodeEntity::class,
        FolderEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class OfflineQrDatabase : RoomDatabase() {
    abstract fun qrCodeDao(): QrCodeDao
    abstract fun folderDao(): FolderDao
}
