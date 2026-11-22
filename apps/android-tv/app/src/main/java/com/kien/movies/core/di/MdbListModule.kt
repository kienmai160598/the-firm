package com.kien.movies.core.di

import com.kien.movies.BuildConfig
import com.kien.movies.core.network.NoDelaySocketFactory
import com.kien.movies.core.profile.ProfileScopedCredentialStore
import com.kien.movies.core.profile.ProfileManager
import com.kien.movies.core.tracking.TrackingProviderId
import com.kien.movies.data.local.TraktSettingsDataStore
import com.kien.movies.data.local.ProfileDataStore
import com.kien.movies.data.mdblist.AndroidMdbListAuthPersistence
import com.kien.movies.data.mdblist.AndroidMdbListSyncStorage
import com.kien.movies.data.mdblist.MdbListSyncRepository
import com.kien.movies.data.mdblist.MdbListApiClient
import com.kien.movies.data.mdblist.MdbListAuthRepository
import com.kien.movies.data.mdblist.MdbListAuthStore
import com.kien.movies.data.mdblist.MdbListConfiguration
import com.kien.movies.data.mdblist.MdbListHttpClient
import com.kien.movies.data.mdblist.OkHttpMdbListEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object MdbListModule {
    @Provides
    @Singleton
    fun library(
        api: MdbListApiClient,
        sync: MdbListSyncRepository,
        auth: MdbListAuthStore,
        profiles: ProfileManager
    ) = com.kien.movies.data.mdblist.MdbListLibraryService(
        api, sync, auth, profiles.activeProfileId, CoroutineScope(SupervisorJob() + Dispatchers.IO)
    )

    @Provides
    @Singleton
    fun configuration(): MdbListConfiguration = MdbListConfiguration(
        clientId = BuildConfig.MDBLIST_CLIENT_ID,
        appVersion = BuildConfig.VERSION_NAME
    )

    @Provides
    @Singleton
    fun http(configuration: MdbListConfiguration): MdbListHttpClient = MdbListHttpClient(
        OkHttpMdbListEngine(
            OkHttpClient.Builder()
                .socketFactory(NoDelaySocketFactory)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .callTimeout(45, TimeUnit.SECONDS)
                .build(),
            configuration
        )
    )

    @Provides
    @Singleton
    fun authStore(persistence: AndroidMdbListAuthPersistence, profileDataStore: ProfileDataStore): MdbListAuthStore {
        val store = MdbListAuthStore(persistence)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            profileDataStore.activeProfileId.collect(store::selectProfile)
        }
        return store
    }

    @Provides
    @IntoSet
    fun credentialStore(store: MdbListAuthStore): ProfileScopedCredentialStore = store

    @Provides
    @Singleton
    fun auth(http: MdbListHttpClient, configuration: MdbListConfiguration, store: MdbListAuthStore) =
        MdbListAuthRepository(http, configuration, store)

    @Provides
    @Singleton
    fun api(http: MdbListHttpClient, auth: MdbListAuthRepository, store: MdbListAuthStore) =
        MdbListApiClient(http, auth, store)

    @Provides
    @Singleton
    fun syncRepository(
        storage: AndroidMdbListSyncStorage,
        auth: MdbListAuthStore,
        api: MdbListApiClient,
        profiles: ProfileManager,
        settings: TraktSettingsDataStore
    ) = MdbListSyncRepository(
        storage, auth, api, profiles.activeProfileId,
        CoroutineScope(SupervisorJob() + Dispatchers.IO),
        ratingsEnabled = { settings.ratingSource.value == TrackingProviderId.MDBLIST }
    )
}
