package com.kien.movies.settings

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import com.kien.movies.R
import com.kien.movies.addons.AddonsScreen

/** The pages of the Settings tab, in the order the list shows them (tvOS SettingsPage.swift). */
enum class SettingsPage(val title: String, @param:DrawableRes val icon: Int, val caption: String) {
    Account("Account", R.drawable.ic_settings_account, "Sign in to Trakt to sync what you watch, your watchlist and your progress."),
    Playback("Playback", R.drawable.ic_settings_playback, "The audio language a film starts in."),
    Subtitles("Subtitles", R.drawable.ic_settings_subtitles, "The subtitles a film starts with, or none."),
    Addons("Addons", R.drawable.ic_settings_addons, "Install, order and turn off the addons that bring catalogs, streams and subtitles."),
    About("About", R.drawable.ic_settings_about, "Version, build and licence.")
}

/** The test tag of a page's row in the Settings list. */
fun settingsRowTag(page: SettingsPage): String = "settings-${page.name.lowercase()}"

/** The screen a Settings page opens (tvOS `SettingsPage.screen`). */
@Composable
fun SettingsPageScreen(page: SettingsPage) {
    when (page) {
        SettingsPage.Account -> AccountScreen()
        SettingsPage.Playback -> LanguageSettingsScreen(LanguageKind.Audio)
        SettingsPage.Subtitles -> LanguageSettingsScreen(LanguageKind.Subtitles)
        SettingsPage.Addons -> AddonsScreen()
        SettingsPage.About -> AboutScreen()
    }
}
