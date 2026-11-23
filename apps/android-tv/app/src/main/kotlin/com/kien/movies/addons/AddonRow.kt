package com.kien.movies.addons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.kien.movies.R
import com.kien.movies.streams.TvColors
import com.kien.movies.streams.TvControls
import com.kien.movies.streams.TvGlass
import com.kien.movies.streams.TvShapes
import com.kien.movies.streams.TvType
import com.nuvio.tvos.TvosAddon

/**
 * One installed addon (tvOS AddonRow.swift): logo, name and version, summary or error, capability
 * badges, and On, Off or Refreshing. A tv-material ListItem as a platter (white 22% plus the glass
 * edge, panel corner), solid white with dark content when focused, so every secondary text takes
 * its colour from the content colour.
 */
@Composable
fun AddonRow(addon: TvosAddon, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(
        selected = false,
        onClick = onClick,
        modifier = modifier.testTag(ADDON_ROW_TAG),
        colors = ListItemDefaults.colors(
            containerColor = TvColors.platter,
            contentColor = TvColors.textPrimary,
            focusedContainerColor = TvColors.focusedFill,
            focusedContentColor = TvColors.focusedContent,
            pressedContainerColor = TvColors.focusedFill,
            pressedContentColor = TvColors.focusedContent
        ),
        shape = ListItemDefaults.shape(shape = TvShapes.panel),
        scale = ListItemDefaults.scale(focusedScale = TvControls.wideRowFocusScale),
        border = TvGlass.listItemBorder(TvShapes.panel),
        leadingContent = { AddonLogo(url = addon.logoUrl, enabled = addon.enabled) },
        headlineContent = { TitleLine(addon) },
        supportingContent = { Details(addon) },
        trailingContent = { StateLabel(addon) }
    )
}

const val ADDON_ROW_TAG = "addon-row"

@Composable
private fun TitleLine(addon: TvosAddon) {
    Row(horizontalArrangement = Arrangement.spacedBy(TITLE_GAP), verticalAlignment = Alignment.Bottom) {
        Text(text = addon.name, style = TvType.bodyEmphasized, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        if (addon.version.isNotEmpty()) {
            Text(text = "v${addon.version}", style = TvType.caption2, color = secondary())
        }
    }
}

@Composable
private fun Details(addon: TvosAddon) {
    Column(verticalArrangement = Arrangement.spacedBy(DETAIL_GAP)) {
        val error = addon.errorMessage
        when {
            error != null -> Row(horizontalArrangement = Arrangement.spacedBy(ICON_GAP), verticalAlignment = Alignment.CenterVertically) {
                Icon(painter = painterResource(R.drawable.ic_addons_warning), contentDescription = null, tint = ERROR_RED, modifier = Modifier.size(SMALL_ICON))
                Text(text = error, style = TvType.caption2, color = ERROR_RED, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }

            addon.summary.isNotEmpty() -> Text(text = addon.summary, style = TvType.caption2, color = secondary(), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (addon.capabilities.isNotEmpty()) CapabilityBadges(addon.capabilities)
    }
}

@Composable
private fun CapabilityBadges(capabilities: List<String>) {
    val color = secondary()
    Row(horizontalArrangement = Arrangement.spacedBy(BADGE_GAP)) {
        capabilities.forEach { capability ->
            Text(
                text = capability,
                style = TvType.overline,
                color = color,
                modifier = Modifier
                    .border(BADGE_BORDER, color, TvShapes.pill)
                    .padding(horizontal = BADGE_PADDING_H, vertical = BADGE_PADDING_V)
            )
        }
    }
}

@Composable
private fun StateLabel(addon: TvosAddon) {
    val label = when {
        addon.isRefreshing -> "Refreshing"
        addon.enabled -> "On"
        else -> "Off"
    }
    Text(text = label, style = TvType.caption1, color = secondary())
}

/** tvOS AddonLogo: 96 pt with the card corner, the puzzle symbol until the logo loads. */
@Composable
private fun AddonLogo(url: String?, enabled: Boolean) {
    var loaded by remember(url) { mutableStateOf(false) }
    val alpha = if (enabled) 1f else DISABLED_ALPHA
    Box(
        modifier = Modifier
            .size(LOGO_SIZE)
            .clip(TvShapes.card)
            .background(TvColors.platter),
        contentAlignment = Alignment.Center
    ) {
        if (!loaded) {
            Icon(
                painter = painterResource(R.drawable.ic_tab_addons),
                contentDescription = null,
                tint = secondary().copy(alpha = SECONDARY_ALPHA * alpha),
                modifier = Modifier.size(LOGO_SYMBOL)
            )
        }
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alpha = alpha,
                onSuccess = { loaded = true },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** Secondary text that stays readable on the focused white row. */
@Composable
private fun secondary(): Color = LocalContentColor.current.copy(alpha = SECONDARY_ALPHA)

/** tvOS `.red` on a dark background. */
internal val ERROR_RED = Color(0xFFFF453A)

private val SECONDARY_ALPHA = TvColors.textSecondary.alpha
private const val DISABLED_ALPHA = 0.4f
private val LOGO_SIZE = 48.dp
private val LOGO_SYMBOL = 20.dp
private val SMALL_ICON = 14.dp
private val DETAIL_GAP = 5.dp
private val TITLE_GAP = 8.dp
private val ICON_GAP = 4.dp
private val BADGE_GAP = 6.dp
private val BADGE_BORDER = 1.dp
private val BADGE_PADDING_H = 7.dp
private val BADGE_PADDING_V = 2.dp
