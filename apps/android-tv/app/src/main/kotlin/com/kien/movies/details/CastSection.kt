package com.kien.movies.details

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
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
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import coil3.request.ImageRequest
import com.kien.movies.design.ArtworkImage
import com.kien.movies.design.CardFocus
import com.kien.movies.design.CardSize
import com.kien.movies.design.Palette
import com.kien.movies.design.Spacing
import com.kien.movies.design.TextStyles
import com.nuvio.tvos.TvosCastMember

/** "Cast & Crew": round portraits with name and role (tvOS CastSection.swift). Reached by D-pad (AC D4). */
@Composable
internal fun CastSection(cast: List<TvosCastMember>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HEADER_GAP_4PT)) {
        ShelfHeader(title = CAST_TITLE, modifier = Modifier.padding(horizontal = Spacing.screenHorizontal))
        LazyRow(
            modifier = Modifier.focusRestorer().focusGroup().testTag(CAST_ROW_TAG),
            contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = ROW_PADDING_30PT),
            horizontalArrangement = Arrangement.spacedBy(Spacing.shelfItem)
        ) {
            // tvOS keys cast by position too: names repeat (one person, two roles).
            itemsIndexed(cast, key = { index, _ -> index }, contentType = { _, _ -> CAST_CONTENT_TYPE }) { index, member ->
                CastCard(member = member, modifier = Modifier.testTag(castTag(index)))
            }
        }
    }
}

/** Section title (tvOS ShelfHeader in CatalogRowView.swift). */
@Composable
internal fun ShelfHeader(title: String, modifier: Modifier = Modifier) {
    Text(text = title, style = TextStyles.shelfHeader, color = Palette.textSecondary, modifier = modifier)
}

@Composable
private fun CastCard(member: TvosCastMember, modifier: Modifier = Modifier) {
    Column(
        modifier = Modifier.width(CAPTION_WIDTH_220PT),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.textGapSmall)
    ) {
        // Select does nothing, as on tvOS: the card is focusable so the row can be browsed.
        Card(
            onClick = {},
            shape = CardDefaults.shape(shape = CircleShape),
            colors = CardFocus.colors(),
            scale = CardFocus.scale(),
            border = CardFocus.border(),
            glow = CardFocus.glow(),
            modifier = modifier
                .padding(bottom = Spacing.textGapSmall)
                .size(CardSize.castPortrait)
                .semantics { contentDescription = member.name }
        ) {
            CastPortrait(member)
        }
        Text(
            text = member.name,
            style = NAME_STYLE,
            color = Palette.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        member.role?.let { role ->
            Text(text = role, style = TextStyles.small, color = ROLE_COLOR, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CastPortrait(member: TvosCastMember) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val request = remember(member.photoUrl, density) {
        member.photoUrl?.let { url ->
            val side = with(density) { CardSize.castPortrait.roundToPx() }
            ImageRequest.Builder(context).data(url).size(side, side).build()
        }
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ArtworkImage(model = request, modifier = Modifier.fillMaxSize())
        if (member.photoUrl == null) {
            Text(text = castInitials(member.name), style = INITIALS_STYLE, color = Palette.textSecondary)
        }
    }
}

fun castTag(index: Int): String = "cast-$index"

const val CAST_TITLE = "Cast & Crew"
const val CAST_ROW_TAG = "cast-row"

private const val CAST_CONTENT_TYPE = "cast"

// tvOS literals, pt / 2.
private val HEADER_GAP_4PT = 2.dp
private val ROW_PADDING_30PT = 15.dp
private val CAPTION_WIDTH_220PT = 110.dp
private val NAME_STYLE = TextStyles.small.copy(fontWeight = FontWeight.Medium)
private val ROLE_COLOR = Color.White.copy(alpha = 0.5f)

// tvOS .title2 is 48 pt.
private val INITIALS_STYLE = TextStyles.shelfHeader.copy(fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.Normal)
