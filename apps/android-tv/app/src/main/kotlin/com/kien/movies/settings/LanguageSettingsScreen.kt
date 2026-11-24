package com.kien.movies.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvType

/**
 * A language list like the one in Settings (tvOS LanguageSettingsView.swift): one row per choice,
 * a check on the chosen one. It opens scrolled to the chosen row, with focus on it.
 */
@Composable
fun LanguageSettingsScreen(kind: LanguageKind) {
    val model = viewModel { LanguageSettingsViewModel() }
    val choice by model.choice.collectAsStateWithLifecycle()
    val choices = remember(kind) { kind.choices() }
    val selected = choice.selected(kind)
    val openedOn = remember(kind) { choices.indexOf(selected).coerceAtLeast(0) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (openedOn + HEADER_ITEMS - ROWS_ABOVE_CHOSEN).coerceAtLeast(0))
    val chosenRow = remember { FocusRequester() }
    val chosenAtOpen = remember(kind) { choices[openedOn] }

    SettingsSplit(
        title = kind.page.title,
        preview = { SettingsPreview(caption = kind.page.caption) { SettingsSymbolArt(kind.page.icon) } },
        list = {
            LazyColumn(
                state = listState,
                modifier = Modifier.focusRestorer(chosenRow).testTag(LANGUAGE_LIST_TAG),
                contentPadding = PaddingValues(bottom = BOTTOM_ROOM),
                verticalArrangement = Arrangement.spacedBy(SettingsMetrics.rowGap)
            ) {
                item(key = "header") {
                    Text(text = kind.listTitle, style = TvType.caption1, color = TvColors.textSecondary, modifier = Modifier.padding(start = HEADER_INSET))
                }
                items(choices, key = { code -> code }) { code ->
                    val opener = if (code == chosenAtOpen) Modifier.focusRequester(chosenRow) else Modifier
                    SettingsLinkLabel(
                        title = LanguageName.of(code),
                        onClick = { model.select(code, kind) },
                        trailingIcon = if (code == selected) R.drawable.ic_player_check else null,
                        modifier = opener.testTag(languageRowTag(code))
                    )
                }
            }
        }
    )
}

fun languageRowTag(code: String): String = "language-$code"

const val LANGUAGE_LIST_TAG = "language-list"

private const val ROWS_ABOVE_CHOSEN = 3
private const val HEADER_ITEMS = 1
private val HEADER_INSET = 16.dp
private val BOTTOM_ROOM = 27.dp
