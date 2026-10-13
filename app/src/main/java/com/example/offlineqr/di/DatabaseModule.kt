package com.example.offlineqr.di

import android.content.Context
import androidx.room.Room
import com.example.offlineqr.data.local.FolderDao
import com.example.offlineqr.data.local.OfflineQrDatabase
import com.example.offlineqr.data.local.QrCodeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideOfflineQrDatabase(
        @ApplicationContext context: Context
    ): OfflineQrDatabase {
        return Room.databaseBuilder(
            context,
            OfflineQrDatabase::class.java,
            "offline_qr.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideQrCodeDao(database: OfflineQrDatabase): QrCodeDao {
        return database.qrCodeDao()
    }

    @Provides
    fun provideFolderDao(database: OfflineQrDatabase): FolderDao {
        return database.folderDao()
    }
}
