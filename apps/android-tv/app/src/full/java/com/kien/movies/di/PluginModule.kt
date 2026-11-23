package com.kien.movies.di

import com.kien.movies.core.auth.AuthManager
import com.kien.movies.core.plugin.PluginManager
import com.kien.movies.core.plugin.PluginRuntime
import com.kien.movies.core.plugin.cloudstream.ExternalExtensionLoader
import com.kien.movies.core.plugin.cloudstream.ExternalExtensionRunner
import com.kien.movies.core.plugin.cloudstream.ExternalRepoParser
import com.kien.movies.core.sync.PluginSyncService
import com.kien.movies.data.local.PluginDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PluginModule {

    @Provides
    @Singleton
    fun providePluginRuntime(): PluginRuntime {
        return PluginRuntime()
    }

    @Provides
    @Singleton
    fun providePluginManager(
        dataStore: PluginDataStore,
        runtime: PluginRuntime,
        pluginSyncService: PluginSyncService,
        authManager: AuthManager,
        externalRepoParser: ExternalRepoParser,
        externalExtensionLoader: ExternalExtensionLoader,
        externalExtensionRunner: ExternalExtensionRunner
    ): PluginManager {
        return PluginManager(
            dataStore,
            runtime,
            pluginSyncService,
            authManager,
            externalRepoParser,
            externalExtensionLoader,
            externalExtensionRunner
        )
    }
}
