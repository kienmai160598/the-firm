package com.kien.movies.shell

import androidx.annotation.DrawableRes
import com.kien.movies.R
import com.kien.movies.settings.SettingsPage
import com.nuvio.tvos.TvosStreamTarget

/** The four sidebar tabs, in tvOS order (RootTabs.swift). Addons is a Settings page. */
enum class RootTab(val title: String, @param:DrawableRes val icon: Int) {
    Home("Home", R.drawable.ic_tab_home),
    Search("Search", R.drawable.ic_tab_search),
    Library("Library", R.drawable.ic_tab_library),
    Settings("Settings", R.drawable.ic_tab_settings)
}

/**
 * Every place the [Router] can show (tvOS AppRoute.swift plus the tab roots). The player is not a
 * route: as on tvOS it is a full screen cover that Streams presents (`player/PlaybackScreen`), so
 * its failure notice goes straight back to the Streams screen that opened it.
 */
sealed interface AppRoute {
    /** A tab root; always and only the bottom of the back stack. */
    data class Tab(val tab: RootTab) : AppRoute

    data class Details(val type: String, val id: String) : AppRoute

    data class Streams(val target: TvosStreamTarget, val resumesDirectly: Boolean = false) : AppRoute

    /** A page of the Settings tab (tvOS `AppRoute.settings`). */
    data class Settings(val page: SettingsPage) : AppRoute

    /** One addon's page under Settings, Addons (tvOS `AppRoute.addon(manifestUrl:)`). */
    data class Addon(val manifestUrl: String) : AppRoute
}
