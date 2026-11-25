package com.kien.movies.addons

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.design.TvType
import com.kien.movies.settings.SettingsRow
import com.kien.movies.settings.SettingsTrailingIcon
import com.kien.movies.settings.settingsSecondary
import com.kien.movies.streams.StreamsSpinner
import com.nuvio.tvos.TvosAddon

/**
 * One row of the installed list (tvOS AddonRow.swift): the name, and its state where Settings shows
 * a value (a spinner while it refreshes, a red Error, else On or Off), then the chevron of a row
 * that opens a page.
 */
@Composable
fun AddonRow(addon: TvosAddon, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val state = stateLabel(addon)
    SettingsRow(
        title = addon.name,
        onClick = onClick,
        modifier = modifier.testTag(ADDON_ROW_TAG),
        description = listOfNotNull(addon.name, state).joinToString(", ")
    ) {
        when {
            addon.isRefreshing -> StreamsSpinner(size = SPINNER)
            addon.errorMessage != null -> Text(text = ERROR, style = TvType.body, color = ERROR_RED, maxLines = 1)
            else -> Text(text = checkNotNull(state), style = TvType.body, color = settingsSecondary(), maxLines = 1)
        }
        SettingsTrailingIcon(R.drawable.ic_settings_chevron)
    }
}

/** What the row reads as after the name; a refreshing addon reads as its name alone, as on tvOS. */
private fun stateLabel(addon: TvosAddon): String? = when {
    addon.isRefreshing -> null
    addon.errorMessage != null -> ERROR
    addon.enabled -> "On"
    else -> "Off"
}

const val ADDON_ROW_TAG = "addon-row"

private const val ERROR = "Error"
private val SPINNER = 16.dp // tvOS ProgressView in a row value
