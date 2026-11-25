package com.kien.movies.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.design.Focus
import com.kien.movies.design.TvGlass
import com.kien.movies.design.TvShapes
import com.kien.movies.design.TvType
import com.kien.movies.design.focusLift

/**
 * A Settings row (tvOS SettingsLinkLabel.swift): the title, its value where Settings shows one, and
 * a trailing glyph, the chevron of a row that opens a page or the check of a chosen one. A capsule
 * in [SettingsRowColors], as tall as the tvOS grouped row. The row's description reads
 * "Title, Value", as on tvOS.
 */
@Composable
fun SettingsLinkLabel(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    @DrawableRes trailingIcon: Int? = R.drawable.ic_settings_chevron
) {
    SettingsRow(title = title, onClick = onClick, modifier = modifier, description = listOfNotNull(title, value).joinToString(", ")) {
        if (value != null) Text(text = value, style = TvType.body, color = settingsSecondary(), maxLines = 1)
        SettingsTrailingIcon(trailingIcon)
    }
}

/**
 * The row [SettingsLinkLabel] draws, with any [trailing] content where Settings shows a value (a
 * spinner, a red "Error"). [description] is what the row reads as.
 */
@Composable
fun SettingsRow(title: String, onClick: () -> Unit, modifier: Modifier = Modifier, description: String = title, trailing: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(SettingsMetrics.rowHeight)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .focusLift(Focus.wideRowScale),
        colors = SettingsRowColors.surface(),
        shape = TvGlass.surfaceShape(TvShapes.pill),
        scale = TvGlass.noSurfaceScale()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(SettingsMetrics.rowHeight).padding(start = TITLE_INSET, end = GLYPH_INSET),
            horizontalArrangement = Arrangement.spacedBy(VALUE_GAP),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = TvType.body, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            trailing()
        }
    }
}

/** The chevron or check at a row's end, in the row's secondary colour. */
@Composable
fun SettingsTrailingIcon(@DrawableRes icon: Int?) {
    if (icon != null) Icon(painter = painterResource(icon), contentDescription = null, tint = settingsSecondary(), modifier = Modifier.size(GLYPH))
}

/** A value's colour: secondary on the dark row, still readable on the focused white one. */
@Composable
fun settingsSecondary(): Color = LocalContentColor.current.copy(alpha = SECONDARY_ALPHA)

// Measured on an Apple TV 4K screenshot of this list, halved (dp = pt / 2).
private const val SECONDARY_ALPHA = 0.6f

/** Where a row's title starts inside its platter; plain text above or below the rows starts here too. */
internal val SETTINGS_ROW_TITLE_INSET = 10.dp
private val TITLE_INSET = SETTINGS_ROW_TITLE_INSET
private val GLYPH_INSET = 4.dp
private val VALUE_GAP = 8.dp
private val GLYPH = 22.dp
