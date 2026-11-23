package com.kien.movies.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.nuvio.tvos.TvosMetaDetails

/**
 * "About": a platter one third of the content wide with the name, genres and overview
 * (reference `20-details-d7.png`). Focusable so the D-pad can scroll to it; Select does nothing.
 */
@Composable
internal fun AboutSection(details: TvosMetaDetails, focusRequester: FocusRequester, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        ShelfHeader(title = ABOUT_TITLE)
        Platter(
            restingFill = ABOUT_FILL,
            modifier = Modifier.padding(start = TvGrid.margin).focusRequester(focusRequester).testTag(DETAILS_ABOUT_TAG),
            width = TvGrid.itemWidth(INFO_COLUMNS),
            contentPadding = ABOUT_PADDING
        ) {
            Text(text = details.name, style = ABOUT_NAME_STYLE, color = TvColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            aboutGenres(details)?.let { genres ->
                Text(text = genres, style = TvType.caption2, color = TvColors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            details.overview?.let { overview ->
                Spacer(modifier = Modifier.height(ABOUT_OVERVIEW_GAP))
                Text(text = overview, style = TvType.caption1, color = TvColors.textPrimary, maxLines = ABOUT_LINES, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/**
 * "Information": label and value pairs in the first of three columns (reference
 * `21-details-d8.png`). One focusable platter, so the D-pad can scroll down to it (AC D4): flat at
 * rest, a lifted platter with the glass edge when focused. Select does nothing.
 */
@Composable
internal fun DetailsInfoSection(entries: List<InfoEntry>, modifier: Modifier = Modifier) {
    if (entries.isEmpty()) return
    Column(modifier = modifier) {
        Text(
            text = INFO_TITLE,
            style = TvType.headline,
            color = TvColors.textPrimary,
            modifier = Modifier.padding(horizontal = TvGrid.margin)
        )
        Spacer(modifier = Modifier.height(INFO_TITLE_GAP))
        // The platter reaches out by its padding so the text lines up with the title.
        Platter(
            restingFill = Color.Transparent,
            modifier = Modifier.padding(start = TvGrid.margin - INFO_PADDING).testTag(DETAILS_INFO_TAG),
            width = TvGrid.itemWidth(INFO_COLUMNS) + INFO_PADDING * 2,
            contentPadding = INFO_PADDING
        ) {
            entries.forEachIndexed { index, entry ->
                if (index > 0) Spacer(modifier = Modifier.height(INFO_ENTRY_GAP))
                Text(text = entry.title, style = TvType.caption1, color = TvColors.textSecondary, maxLines = 1)
                Spacer(modifier = Modifier.height(INFO_VALUE_GAP))
                Text(text = entry.value, style = TvType.caption1, color = TvColors.textPrimary, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** A focusable panel: [restingFill] at rest, the platter fill, edge and 1.05 lift when focused. */
@Composable
private fun Platter(
    restingFill: Color,
    width: Dp,
    contentPadding: Dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    // The edge goes through the surface's border so it lifts with the focused platter.
    val edge = Border(border = BorderStroke(GLASS_EDGE_WIDTH, TvColors.glassEdge), shape = TvShapes.panel)
    Surface(
        onClick = {},
        shape = ClickableSurfaceDefaults.shape(shape = TvShapes.panel),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = restingFill,
            contentColor = TvColors.textPrimary,
            focusedContainerColor = TvColors.platter,
            focusedContentColor = TvColors.textPrimary,
            pressedContainerColor = TvColors.platter,
            pressedContentColor = TvColors.textPrimary
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = TvFocus.controlScale),
        border = ClickableSurfaceDefaults.border(
            border = if (restingFill == Color.Transparent) Border.None else edge,
            focusedBorder = edge,
            pressedBorder = edge
        ),
        modifier = modifier.width(width)
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

const val ABOUT_TITLE = "About"
const val INFO_TITLE = "Information"
const val DETAILS_ABOUT_TAG = "details-about"
const val DETAILS_INFO_TAG = "details-info"

private const val INFO_COLUMNS = 3
private const val ABOUT_LINES = 5

// Measured on 20-details-d7.png and 21-details-d8.png (pt / 2): About platter 560 pt wide,
// text inset 22 pt, fill white about 10%; Information labels 37 pt above their values, pairs
// 98 pt apart, the focused platter 16 pt outside the text.
private val ABOUT_FILL = Color.White.copy(alpha = 0.1f)
private val ABOUT_PADDING = 11.dp
private val ABOUT_OVERVIEW_GAP = 12.dp
private val ABOUT_NAME_STYLE = TvType.body.copy(fontWeight = FontWeight.Bold)
private val INFO_TITLE_GAP = 8.dp
private val INFO_PADDING = 14.dp
private val INFO_VALUE_GAP = 2.dp
private val INFO_ENTRY_GAP = 14.dp
private val GLASS_EDGE_WIDTH = 1.dp
