package com.kien.movies.core.di

import android.os.Build
import com.kien.movies.BuildConfig
import com.kien.movies.core.network.NoDelaySocketFactory
import com.kien.movies.core.profile.ProfileScopedCredentialStore
import com.kien.movies.core.sync.SyncClientIdentity
import com.kien.movies.data.local.ProfileDataStore
import com.kien.movies.data.mediaserver.AndroidServerPersistence
import com.kien.movies.data.mediaserver.ServerRepository
import com.kien.movies.data.mediaserver.emby.EmbyProvider
import com.kien.movies.data.mediaserver.jellyfin.JellyfinProvider
import com.kien.movies.data.mediaserver.mediabrowser.ServerClientIdentity
import com.kien.movies.data.mediaserver.plex.PlexProvider
import com.kien.movies.data.mediaserver.silo.SiloProvider
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
object MediaServerModule {
    @Provides
    @Singleton
    fun repository(
        persistence: AndroidServerPersistence,
        identity: SyncClientIdentity,
        profileDataStore: ProfileDataStore
    ): ServerRepository {
        val http = OkHttpClient.Builder()
            .socketFactory(NoDelaySocketFactory)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .build()
        val clientIdentity = ServerClientIdentity(
            device = Build.MODEL.orEmpty().ifBlank { "Android TV" },
            version = BuildConfig.VERSION_NAME,
            deviceId = identity::currentClientId
        )
        lateinit var repository: ServerRepository
        val profileClientIdentity = ServerClientIdentity(
            device = clientIdentity.device,
            version = clientIdentity.version,
            deviceId = { repository.profileDeviceId(identity.currentClientId()) }
        )
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        repository = ServerRepository(
            persistence = persistence,
            providers = listOf(
                JellyfinProvider(http, profileClientIdentity),
                EmbyProvider(http, profileClientIdentity),
                PlexProvider(http, clientIdentity),
                SiloProvider(http, clientIdentity)
            ),
            scope = scope
        )
        scope.launch { profileDataStore.activeProfileId.collect(repository::selectProfile) }
        return repository
    }

    @Provides
    @IntoSet
    fun credentialStore(repository: ServerRepository): ProfileScopedCredentialStore = repository
}
