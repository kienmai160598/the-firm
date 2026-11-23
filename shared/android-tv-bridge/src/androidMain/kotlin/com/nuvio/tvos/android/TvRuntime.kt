package com.nuvio.tvos.android

import android.content.Context
import com.nuvio.app.core.auth.AuthStorage
import com.nuvio.app.core.network.ServerConfigurationStorage
import com.nuvio.app.core.poster.CustomPosterUrlStorage
import com.nuvio.app.core.storage.PlatformLocalAccountDataCleaner
import com.nuvio.app.core.sync.SyncClientIdentityStorage
import com.nuvio.app.core.ui.CardDepthStyleStorage
import com.nuvio.app.core.ui.PosterCardStyleStorage
import com.nuvio.app.features.addons.AddonHttpClientProvider
import com.nuvio.app.features.addons.AddonStorage
import com.nuvio.app.features.autosync.AutoSyncPreferencesAndroid
import com.nuvio.app.features.collection.CollectionMobileSettingsStorage
import com.nuvio.app.features.collection.CollectionStorage
import com.nuvio.app.features.debrid.DebridSettingsStorage
import com.nuvio.app.features.details.MetaScreenSettingsStorage
import com.nuvio.app.features.details.SeasonViewModeStorage
import com.nuvio.app.features.downloads.DownloadsStorage
import com.nuvio.app.features.home.HomeCatalogSettingsStorage
import com.nuvio.app.features.library.LibraryDisplaySettingsStorage
import com.nuvio.app.features.library.LibraryStorage
import com.nuvio.app.features.mdblist.MdbListSettingsStorage
import com.nuvio.app.features.mdblist.PlatformMdbListAuthPersistence
import com.nuvio.app.features.mdblist.PlatformMdbListSyncStorage
import com.nuvio.app.features.membership.MemberAssetStorage
import com.nuvio.app.features.notifications.EpisodeReleaseNotificationsStorage
import com.nuvio.app.features.p2p.P2pSettingsStorage
import com.nuvio.app.features.p2p.P2pStreamingEngine
import com.nuvio.app.features.player.ExternalPlayerPlatform
import com.nuvio.app.features.player.PlayerSettingsStorage
import com.nuvio.app.features.player.PlayerTrackPreferenceStorage
import com.nuvio.app.features.player.SubtitleFileCache
import com.nuvio.app.features.plugins.PluginRepository
import com.nuvio.app.features.plugins.PluginStorage
import com.nuvio.app.features.profiles.AvatarStorage
import com.nuvio.app.features.profiles.ProfilePinCacheStorage
import com.nuvio.app.features.profiles.ProfileStorage
import com.nuvio.app.features.search.DiscoverSelectionStorage
import com.nuvio.app.features.search.SearchHistoryStorage
import com.nuvio.app.features.servers.ServerStorage
import com.nuvio.app.features.settings.SentrySettingsStorage
import com.nuvio.app.features.settings.ThemeSettingsStorage
import com.nuvio.app.features.shuffle.EpisodeShuffleStorage
import com.nuvio.app.features.simkl.SimklAuthStorage
import com.nuvio.app.features.simkl.SimklSyncStorage
import com.nuvio.app.features.streams.BingeGroupCacheStorage
import com.nuvio.app.features.streams.StreamBadgeSettingsStorage
import com.nuvio.app.features.streams.StreamLinkCacheStorage
import com.nuvio.app.features.tmdb.TmdbSettingsStorage
import com.nuvio.app.features.trakt.TraktAuthStorage
import com.nuvio.app.features.trakt.TraktCommentsStorage
import com.nuvio.app.features.trakt.TraktLibraryStorage
import com.nuvio.app.features.trakt.TraktSettingsStorage
import com.nuvio.app.features.updater.AndroidAppUpdaterPlatform
import com.nuvio.app.features.watched.WatchedStorage
import com.nuvio.app.features.watchprogress.ContinueWatchingEnrichmentStorage
import com.nuvio.app.features.watchprogress.ContinueWatchingPreferencesStorage
import com.nuvio.app.features.watchprogress.ResumePromptStorage
import com.nuvio.app.features.watchprogress.WatchProgressStorage

/**
 * Gives every core storage and platform object compiled into the bridge its Context, as
 * core's phone MainActivity does (shared/core/src/androidMain/.../MainActivity.kt), without
 * any UI. A storage that is never initialised reads and writes nothing, silently, so
 * scripts/check-tv-runtime-init fails when an object with `fun initialize(` is missing here.
 *
 * Call it once from Application.onCreate, off the main thread, before any Tvos* call.
 * Idempotent: a second call returns at once.
 */
object TvRuntime {
    private val lock = Any()

    @Volatile
    private var initialized = false

    val isInitialized: Boolean get() = initialized

    fun init(context: Context) {
        if (initialized) return
        synchronized(lock) {
            if (initialized) return
            initializeAll(context.applicationContext)
            initialized = true
        }
    }

    // Same order as core's MainActivity, so an object that reads another at init finds it ready.
    private fun initializeAll(context: Context) {
        ThemeSettingsStorage.initialize(context)
        SentrySettingsStorage.initialize(context)
        AutoSyncPreferencesAndroid.initialize(context)
        SyncClientIdentityStorage.initialize(context)
        AddonHttpClientProvider.initialize(context)
        AddonStorage.initialize(context)
        AuthStorage.initialize(context)
        ServerConfigurationStorage.initialize(context)
        LibraryStorage.initialize(context)
        WatchedStorage.initialize(context)
        MetaScreenSettingsStorage.initialize(context)
        EpisodeShuffleStorage.initialize(context)
        HomeCatalogSettingsStorage.initialize(context)
        PlayerSettingsStorage.initialize(context)
        PlayerTrackPreferenceStorage.initialize(context)
        P2pSettingsStorage.initialize(context)
        P2pStreamingEngine.initialize(context)
        ExternalPlayerPlatform.initialize(context)
        SubtitleFileCache.initialize(context)
        ProfileStorage.initialize(context)
        AvatarStorage.initialize(context)
        ProfilePinCacheStorage.initialize(context)
        MemberAssetStorage.initialize(context)
        DiscoverSelectionStorage.initialize(context)
        SearchHistoryStorage.initialize(context)
        SeasonViewModeStorage.initialize(context)
        PosterCardStyleStorage.initialize(context)
        CustomPosterUrlStorage.initialize(context)
        CardDepthStyleStorage.initialize(context)
        DebridSettingsStorage.initialize(context)
        TmdbSettingsStorage.initialize(context)
        MdbListSettingsStorage.initialize(context)
        TraktAuthStorage.initialize(context)
        TraktCommentsStorage.initialize(context)
        TraktLibraryStorage.initialize(context)
        TraktSettingsStorage.initialize(context)
        PlatformMdbListAuthPersistence.initialize(context)
        ServerStorage.initialize(context)
        PlatformMdbListSyncStorage.initialize(context)
        SimklAuthStorage.initialize(context)
        SimklSyncStorage.initialize(context)
        LibraryDisplaySettingsStorage.initialize(context)
        ContinueWatchingPreferencesStorage.initialize(context)
        ResumePromptStorage.initialize(context)
        ContinueWatchingEnrichmentStorage.initialize(context)
        EpisodeReleaseNotificationsStorage.initialize(context)
        WatchProgressStorage.initialize(context)
        StreamLinkCacheStorage.initialize(context)
        StreamBadgeSettingsStorage.initialize(context)
        BingeGroupCacheStorage.initialize(context)
        PluginStorage.initialize(context)
        PluginRepository.initialize()
        CollectionMobileSettingsStorage.initialize(context)
        CollectionStorage.initialize(context)
        DownloadsStorage.initialize(context)
        AndroidAppUpdaterPlatform.initialize(context)
        PlatformLocalAccountDataCleaner.initialize(context)
    }
}
