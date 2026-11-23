package com.kien.movies.details

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Text
import coil3.request.ImageRequest
import com.kien.movies.design.ArtworkImage
import com.kien.movies.design.Focus
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvGrid
import com.kien.movies.design.TvType
import com.kien.movies.design.rememberStartAlignedRowSpec
import com.nuvio.tvos.TvosCastMember

/**
 * "Cast & Crew": 260 pt circles on the six column grid, name and role under each (reference
 * `19-details-d6.png`, `20-details-d7.png`). Reached by D-pad (AC D4).
 */
@Composable
internal fun CastSection(cast: List<TvosCastMember>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        ShelfHeader(title = CAST_TITLE)
        CompositionLocalProvider(LocalBringIntoViewSpec provides rememberStartAlignedRowSpec()) {
            LazyRow(
                modifier = Modifier.focusRestorer().focusGroup().testTag(CAST_ROW_TAG),
                contentPadding = PaddingValues(horizontal = TvGrid.sideMargin),
                horizontalArrangement = Arrangement.spacedBy(TvGrid.gutter)
            ) {
                // tvOS keys cast by position too: names repeat (one person, two roles).
                itemsIndexed(cast, key = { index, _ -> index }, contentType = { _, _ -> CAST_CONTENT_TYPE }) { index, member ->
                    CastCard(member = member, modifier = Modifier.testTag(castTag(index)))
                }
            }
        }
    }
}

/**
 * A shelf's title (Callout bold, secondary) at the side margin, its baseline 30 pt above the
 * cards: the title line and a 12 dp gap.
 */
@Composable
internal fun ShelfHeader(title: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = TvGrid.sideMargin)) {
        Text(text = title, style = TvType.rowTitle, color = TvColors.textSecondary, maxLines = 1)
        Spacer(modifier = Modifier.height(SHELF_TITLE_GAP))
    }
}

@Composable
private fun CastCard(member: TvosCastMember, modifier: Modifier = Modifier) {
    val side = TvGrid.itemWidth(CAST_COLUMNS)
    Column(modifier = Modifier.width(side), horizontalAlignment = Alignment.CenterHorizontally) {
        // Select does nothing, as on tvOS: the card is focusable so the row can be browsed.
        Card(
            onClick = {},
            shape = CardDefaults.shape(shape = CircleShape),
            colors = CardDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
            scale = CardDefaults.scale(focusedScale = Focus.cardScale),
            border = CardDefaults.border(border = Border.None, focusedBorder = Border.None, pressedBorder = Border.None),
            glow = CardDefaults.glow(focusedGlow = Glow(elevationColor = TvColors.focusShadow, elevation = Focus.shadowElevation)),
            modifier = modifier
                .size(side)
                .semantics { contentDescription = member.name }
        ) {
            CastPortrait(member)
        }
        Spacer(modifier = Modifier.height(NAME_GAP))
        Text(
            text = member.name,
            style = NAME_STYLE,
            color = TvColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        member.role?.let { role ->
            Text(text = role, style = TvType.caption2, color = TvColors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CastPortrait(member: TvosCastMember) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val side = TvGrid.itemWidth(CAST_COLUMNS)
    val request = remember(member.photoUrl, density) {
        member.photoUrl?.let { url ->
            val px = with(density) { side.roundToPx() }
            ImageRequest.Builder(context).data(url).size(px, px).build()
        }
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ArtworkImage(model = request, modifier = Modifier.fillMaxSize())
        if (member.photoUrl == null) {
            Text(text = castInitials(member.name), style = INITIALS_STYLE, color = TvColors.textSecondary)
        }
    }
}

fun castTag(index: Int): String = "cast-$index"

const val CAST_TITLE = "Cast & Crew"
const val CAST_ROW_TAG = "cast-row"

private const val CAST_CONTENT_TYPE = "cast"
private const val CAST_COLUMNS = 6

// Spec section 3 shelf row: title baseline 30 pt above the cards.
internal val SHELF_TITLE_GAP = 12.dp

// Measured on 20-details-d7.png: name 13 pt under the circle, Caption 1; role Caption 2.
private val NAME_GAP = 6.dp
private val NAME_STYLE = TvType.caption1.copy(fontWeight = FontWeight.Medium)

// Initials when there is no photo: Title 2 (57 pt) at half.
private val INITIALS_STYLE = TvType.body.copy(fontSize = 28.sp, lineHeight = 32.sp)
