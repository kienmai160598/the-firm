package com.kien.movies.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.design.Focus
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvGlass
import com.kien.movies.design.TvShapes
import com.kien.movies.design.TvType

/**
 * A Settings row (tvOS SettingsLinkLabel.swift): the title, its value where Settings shows one, and
 * a trailing glyph, the chevron of a row that opens a page or the check of a chosen one. A
 * tv-material ListItem on its own platter, solid white with dark content when focused. The row's
 * description reads "Title, Value", as on tvOS.
 */
@Composable
fun SettingsLinkLabel(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    @DrawableRes trailingIcon: Int? = R.drawable.ic_settings_chevron
) {
    ListItem(
        selected = false,
        onClick = onClick,
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = listOfNotNull(title, value).joinToString(", ") },
        colors = ListItemDefaults.colors(
            containerColor = TvColors.platter,
            contentColor = TvColors.textPrimary,
            focusedContainerColor = TvColors.focusedFill,
            focusedContentColor = TvColors.focusedContent,
            pressedContainerColor = TvColors.focusedFill,
            pressedContentColor = TvColors.focusedContent
        ),
        shape = ListItemDefaults.shape(shape = TvShapes.panel),
        scale = ListItemDefaults.scale(focusedScale = Focus.wideRowScale),
        border = TvGlass.listItemBorder(TvShapes.panel),
        headlineContent = { Text(text = title, style = TvType.body, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingContent = { Trailing(value, trailingIcon) }
    )
}

@Composable
private fun Trailing(value: String?, @DrawableRes icon: Int?) {
    val secondary = LocalContentColor.current.copy(alpha = SECONDARY_ALPHA)
    Row(horizontalArrangement = Arrangement.spacedBy(VALUE_GAP), verticalAlignment = Alignment.CenterVertically) {
        if (value != null) Text(text = value, style = TvType.body, color = secondary, maxLines = 1)
        if (icon != null) Icon(painter = painterResource(icon), contentDescription = null, tint = secondary, modifier = Modifier.size(GLYPH))
    }
}

private const val SECONDARY_ALPHA = 0.6f
private val VALUE_GAP = 12.dp
private val GLYPH = 18.dp
