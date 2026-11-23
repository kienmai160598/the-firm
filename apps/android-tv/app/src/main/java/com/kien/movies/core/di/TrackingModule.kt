package com.kien.movies.core.di

import com.kien.movies.core.profile.ProfileScopedCredentialStore
import com.kien.movies.core.tracking.TrackingHistoryWriter
import com.kien.movies.core.tracking.TrackingLibraryProvider
import com.kien.movies.core.tracking.TrackingProgressProvider
import com.kien.movies.core.tracking.TrackingProvider
import com.kien.movies.core.tracking.TrackingRatingProvider
import com.kien.movies.data.mdblist.MdbListTrackingHistoryWriter
import com.kien.movies.data.mdblist.MdbListTrackingProgressProvider
import com.kien.movies.data.mdblist.MdbListTrackingProvider
import com.kien.movies.data.mdblist.MdbListUserRatings
import com.kien.movies.data.repository.TraktTrackingHistoryWriter
import com.kien.movies.data.repository.TraktTrackingLibraryProvider
import com.kien.movies.data.repository.TraktTrackingProgressProvider
import com.kien.movies.data.repository.TraktTrackingProvider
import com.kien.movies.data.simkl.AndroidSimklAuthStorage
import com.kien.movies.data.simkl.AndroidSimklSyncStorage
import com.kien.movies.data.simkl.SimklApiSyncRemote
import com.kien.movies.data.simkl.SimklAuthStorage
import com.kien.movies.data.simkl.SimklLibraryService
import com.kien.movies.data.simkl.SimklRatings
import com.kien.movies.data.simkl.SimklSyncRemote
import com.kien.movies.data.simkl.SimklSyncStorage
import com.kien.movies.data.simkl.SimklTrackingHistoryWriter
import com.kien.movies.data.simkl.SimklTrackingProgressProvider
import com.kien.movies.data.simkl.SimklTrackingProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TrackingModule {
    @Binds
    @IntoSet
    abstract fun bindMdbListLibrary(
        provider: com.kien.movies.data.mdblist.MdbListTrackingLibraryProvider
    ): TrackingLibraryProvider

    @Binds
    @IntoSet
    abstract fun bindMdbListProvider(provider: MdbListTrackingProvider): TrackingProvider

    @Binds
    @IntoSet
    abstract fun bindMdbListProgress(
        provider: MdbListTrackingProgressProvider
    ): TrackingProgressProvider

    @Binds
    @IntoSet
    abstract fun bindMdbListHistory(writer: MdbListTrackingHistoryWriter): TrackingHistoryWriter

    @Binds
    @IntoSet
    abstract fun bindMdbListRatings(provider: MdbListUserRatings): TrackingRatingProvider

    @Binds
    @IntoSet
    abstract fun bindSimklRatings(provider: SimklRatings): TrackingRatingProvider

    @Binds
    @Singleton
    abstract fun bindSimklAuthStorage(storage: AndroidSimklAuthStorage): SimklAuthStorage

    @Binds
    @IntoSet
    abstract fun bindSimklProfileScopedCredentialStore(
        storage: AndroidSimklAuthStorage
    ): ProfileScopedCredentialStore

    @Binds
    @Singleton
    abstract fun bindSimklSyncStorage(storage: AndroidSimklSyncStorage): SimklSyncStorage

    @Binds
    @Singleton
    abstract fun bindSimklSyncRemote(remote: SimklApiSyncRemote): SimklSyncRemote

    @Binds
    @IntoSet
    abstract fun bindTraktTrackingLibraryProvider(
        provider: TraktTrackingLibraryProvider
    ): TrackingLibraryProvider

    @Binds
    @IntoSet
    abstract fun bindSimklTrackingLibraryProvider(
        provider: SimklLibraryService
    ): TrackingLibraryProvider

    @Binds
    @IntoSet
    abstract fun bindTraktTrackingProgressProvider(
        provider: TraktTrackingProgressProvider
    ): TrackingProgressProvider

    @Binds
    @IntoSet
    abstract fun bindSimklTrackingProgressProvider(
        provider: SimklTrackingProgressProvider
    ): TrackingProgressProvider

    @Binds
    @IntoSet
    abstract fun bindTraktTrackingHistoryWriter(
        writer: TraktTrackingHistoryWriter
    ): TrackingHistoryWriter

    @Binds
    @IntoSet
    abstract fun bindSimklTrackingHistoryWriter(
        writer: SimklTrackingHistoryWriter
    ): TrackingHistoryWriter

    @Binds
    @IntoSet
    abstract fun bindTraktTrackingProvider(provider: TraktTrackingProvider): TrackingProvider

    @Binds
    @IntoSet
    abstract fun bindSimklTrackingProvider(provider: SimklTrackingProvider): TrackingProvider
}
