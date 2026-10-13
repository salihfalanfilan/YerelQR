package com.example.offlineqr.di

import com.example.offlineqr.data.repository.FolderRepositoryImpl
import com.example.offlineqr.data.repository.QrRepositoryImpl
import com.example.offlineqr.data.repository.SettingsRepositoryImpl
import com.example.offlineqr.domain.repository.FolderRepository
import com.example.offlineqr.domain.repository.QrRepository
import com.example.offlineqr.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindQrRepository(
        impl: QrRepositoryImpl
    ): QrRepository

    @Binds
    @Singleton
    abstract fun bindFolderRepository(
        impl: FolderRepositoryImpl
    ): FolderRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository
}
