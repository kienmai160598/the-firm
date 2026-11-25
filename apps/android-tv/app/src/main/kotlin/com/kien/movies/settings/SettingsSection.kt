package com.kien.movies.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvType

/**
 * A section header of a grouped Settings list (tvOS `Section` header): upper case, secondary,
 * starting where the row titles start, with the extra room tvOS leaves above a section.
 */
@Composable
fun SettingsSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = TvType.caption1,
        color = TvColors.textSecondary,
        maxLines = 1,
        modifier = modifier.padding(start = SETTINGS_ROW_TITLE_INSET, top = SECTION_TOP)
    )
}

/** A section footer (tvOS `Section` footer): secondary text under the section's rows. */
@Composable
fun SettingsSectionFooter(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = TvType.caption1,
        color = TvColors.textSecondary,
        modifier = modifier.padding(start = SETTINGS_ROW_TITLE_INSET, end = SETTINGS_ROW_TITLE_INSET, bottom = FOOTER_BOTTOM)
    )
}

// Measured on the Apple TV Addons screenshots, halved: rows 36 dp apart across a header, and the
// next section starts 17 dp under a footer; the list's row gap covers the rest.
private val SECTION_TOP = 7.dp
private val FOOTER_BOTTOM = 10.dp
