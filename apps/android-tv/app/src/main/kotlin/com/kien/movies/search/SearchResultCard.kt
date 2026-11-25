package com.kien.movies.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.design.ArtworkImage
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvGrid
import com.kien.movies.design.TvShapes
import com.kien.movies.design.TvType
import com.kien.movies.design.focusLift
import com.kien.movies.design.glassEdgeBorder
import com.nuvio.tvos.TvosItem

/**
 * A "Top Results" card (spec section 3, cards: 560 x 170 pt platter, corner 24): the poster thumb
 * at a 20 pt inset, the title in Body bold and "Movie · Sci-Fi · 2021" in Caption 2.
 */
@Composable
internal fun SearchResultCard(item: TvosItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ResultPlatter(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = item.name },
        thumb = { ArtworkImage(model = item.posterUrl, modifier = Modifier.size(thumbSize).clip(thumbShape)) },
        title = item.name,
        subtitle = resultSubtitle(item)
    )
}

/**
 * A "Recently Searched" card. The bridge keeps recent searches as text, not titles, so the thumb
 * shows a magnifier where tvOS shows the poster of the title that was opened.
 */
@Composable
internal fun RecentSearchCard(query: String, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier) {
    ResultPlatter(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        thumb = {
            Box(modifier = Modifier.size(thumbSize), contentAlignment = Alignment.Center) {
                Icon(painter = painterResource(R.drawable.ic_tab_search), contentDescription = null, tint = TvColors.textSecondary, modifier = Modifier.size(RECENT_ICON))
            }
        },
        title = query,
        subtitle = null
    )
}

/**
 * The platter: flat white 22% with the glass edge; focused, a lighter fill at scale 1.05 (measured
 * on 40-search-results-down.png: 588 x 180 pt from 560 x 170, fill #7C8081). It never turns
 * solid white, so the edge stays.
 */
@Composable
private fun ResultPlatter(
    onClick: () -> Unit,
    modifier: Modifier,
    thumb: @Composable () -> Unit,
    title: String,
    subtitle: String?,
    onLongClick: (() -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.size(resultCardSize()).focusLift(RESULT_FOCUS_SCALE),
        shape = ClickableSurfaceDefaults.shape(shape = TvShapes.panel),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = TvColors.platter,
            contentColor = TvColors.textPrimary,
            focusedContainerColor = FOCUSED_PLATTER,
            focusedContentColor = TvColors.textPrimary,
            pressedContainerColor = FOCUSED_PLATTER,
            pressedContentColor = TvColors.textPrimary
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(border = glassEdgeBorder(TvShapes.panel), focusedBorder = glassEdgeBorder(TvShapes.panel)),
        glow = ClickableSurfaceDefaults.glow(focusedGlow = focusShadow)
    ) {
        Row(
            modifier = Modifier.fillMaxHeight().padding(horizontal = INSET),
            horizontalArrangement = Arrangement.spacedBy(THUMB_TO_TEXT),
            verticalAlignment = Alignment.CenterVertically
        ) {
            thumb()
            Column(verticalArrangement = Arrangement.spacedBy(TITLE_TO_SUBTITLE)) {
                Text(text = title, style = TvType.bodyEmphasized, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!subtitle.isNullOrEmpty()) {
                    Text(text = subtitle, style = TvType.caption2, color = TvColors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** "Movie · Sci-Fi · 2021": kind, first genre, year or years; missing parts are left out. */
internal fun resultSubtitle(item: TvosItem): String = listOfNotNull(
    kindLabel(item.type),
    item.genres.firstOrNull()?.takeIf(String::isNotBlank),
    item.releaseInfo?.takeIf(String::isNotBlank)
).joinToString(SEPARATOR)

/** The focused poster's caption: "TV Show · Sci-Fi". */
internal fun posterCaption(item: TvosItem): String = listOfNotNull(
    kindLabel(item.type),
    item.genres.firstOrNull()?.takeIf(String::isNotBlank)
).joinToString(SEPARATOR)

private fun kindLabel(type: String): String? = when (type) {
    "movie" -> "Movie"
    "series" -> "TV Show"
    "" -> null
    else -> type.replaceFirstChar(Char::uppercaseChar)
}

/** Three columns of the grid, 170 pt tall. */
internal fun resultCardSize(): DpSize = DpSize(TvGrid.itemWidth(RESULT_COLUMNS), RESULT_CARD_HEIGHT)

internal const val RESULT_COLUMNS = 3
internal val RESULT_CARD_HEIGHT: Dp = 85.dp

/** Spec section 2: one soft shadow, 12 dp, black 40%. */
internal val focusShadow = Glow(elevationColor = Color.Black.copy(alpha = 0.4f), elevation = 12.dp)

private const val SEPARATOR = " · "
private const val RESULT_FOCUS_SCALE = 1.05f
private val FOCUSED_PLATTER = Color.White.copy(alpha = 0.37f)
private val INSET = 10.dp
private val thumbSize = DpSize(43.dp, 65.dp)
private val thumbShape = RoundedCornerShape(4.dp)
private val THUMB_TO_TEXT = 11.dp
private val TITLE_TO_SUBTITLE = 1.dp
private val RECENT_ICON = 24.dp
