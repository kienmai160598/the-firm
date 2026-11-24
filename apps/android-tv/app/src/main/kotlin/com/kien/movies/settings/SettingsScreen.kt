package com.kien.movies.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kien.movies.shell.AppRoute
import com.kien.movies.shell.Router
import com.kien.movies.shell.upToNavPill
import com.nuvio.tvos.TvosLanguageChoice
import com.nuvio.tvos.TvosTraktState

/**
 * The Settings tab root (tvOS SettingsView.swift), laid out like the Settings app: the pages on
 * the right, and on the left the focused page's icon and what it holds. First focus is Account.
 */
@Composable
fun SettingsScreen(router: Router) {
    val languages = viewModel { LanguageSettingsViewModel() }
    val account = viewModel { TraktAccountViewModel() }
    val choice by languages.choice.collectAsStateWithLifecycle()
    val trakt by account.state.collectAsStateWithLifecycle()
    var previewedName by rememberSaveable { mutableStateOf(SettingsPage.Account.name) }
    val previewed = SettingsPage.valueOf(previewedName)
    val firstRow = remember { FocusRequester() }

    SettingsSplit(
        title = null,
        preview = {
            SettingsPreview(caption = previewed.caption) { SettingsSymbolArt(previewed.icon) }
        },
        list = {
            LazyColumn(
                modifier = Modifier.focusRestorer(firstRow).testTag(SETTINGS_LIST_TAG),
                verticalArrangement = Arrangement.spacedBy(SettingsMetrics.rowGap)
            ) {
                items(SettingsPage.entries, key = SettingsPage::name) { page ->
                    val first = if (page == SettingsPage.entries.first()) Modifier.focusRequester(firstRow).upToNavPill() else Modifier
                    SettingsLinkLabel(
                        title = page.title,
                        value = value(page, choice, trakt),
                        onClick = { router.open(AppRoute.Settings(page)) },
                        modifier = first
                            .testTag(settingsRowTag(page))
                            .onFocusChanged { focus -> if (focus.isFocused) previewedName = page.name }
                    )
                }
            }
        }
    )
}

private fun value(page: SettingsPage, choice: TvosLanguageChoice, trakt: TvosTraktState): String? = when (page) {
    SettingsPage.Account -> trakt.summary
    SettingsPage.Playback -> LanguageName.of(choice.audio)
    SettingsPage.Subtitles -> LanguageName.of(choice.subtitles)
    SettingsPage.Addons, SettingsPage.About -> null
}

const val SETTINGS_LIST_TAG = "settings-list"
