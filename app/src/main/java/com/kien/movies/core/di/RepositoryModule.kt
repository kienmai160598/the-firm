package com.kien.movies.core.di

import com.kien.movies.data.repository.AddonRepositoryImpl
import com.kien.movies.data.repository.CatalogRepositoryImpl
import com.kien.movies.data.repository.LibraryRepositoryImpl
import com.kien.movies.data.repository.MetaRepositoryImpl
import com.kien.movies.core.streams.DeviceFitStreamRepository
import com.kien.movies.data.repository.SubtitleRepositoryImpl
import com.kien.movies.data.repository.SyncRepositoryImpl
import com.kien.movies.data.repository.WatchProgressRepositoryImpl
import com.kien.movies.domain.repository.AddonRepository
import com.kien.movies.domain.repository.CatalogRepository
import com.kien.movies.domain.repository.LibraryRepository
import com.kien.movies.domain.repository.MetaRepository
import com.kien.movies.domain.repository.StreamRepository
import com.kien.movies.domain.repository.SubtitleRepository
import com.kien.movies.domain.repository.SyncRepository
import com.kien.movies.domain.repository.WatchProgressRepository
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
    abstract fun bindAddonRepository(impl: AddonRepositoryImpl): AddonRepository

    @Binds
    @Singleton
    abstract fun bindCatalogRepository(impl: CatalogRepositoryImpl): CatalogRepository

    @Binds
    @Singleton
    abstract fun bindLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository

    @Binds
    @Singleton
    abstract fun bindMetaRepository(impl: MetaRepositoryImpl): MetaRepository

    @Binds
    @Singleton
    abstract fun bindStreamRepository(impl: DeviceFitStreamRepository): StreamRepository

    @Binds
    @Singleton
    abstract fun bindSubtitleRepository(impl: SubtitleRepositoryImpl): SubtitleRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository

    @Binds
    @Singleton
    abstract fun bindWatchProgressRepository(impl: WatchProgressRepositoryImpl): WatchProgressRepository
}
