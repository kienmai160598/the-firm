package com.kien.movies.core.di

import android.content.Context
import com.kien.movies.core.torrent.TorrentService
import com.kien.movies.core.torrent.TorrentSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TorrentModule {

    @Provides
    @Singleton
    fun provideTorrentSettings(@ApplicationContext context: Context): TorrentSettings = TorrentSettings(context)

    @Provides
    @Singleton
    fun provideTorrentService(
        @ApplicationContext context: Context,
        torrentSettings: TorrentSettings
    ): TorrentService = TorrentService(context, torrentSettings)
}
