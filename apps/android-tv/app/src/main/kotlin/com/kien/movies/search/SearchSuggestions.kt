package com.kien.movies.search

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvGrid
import com.kien.movies.design.TvShapes
import com.kien.movies.design.glassEdgeBorder

/**
 * The suggestion capsules between the keyboard and the divider (39-search-results.png): a
 * magnifier and the suggested query, the typed part bright and the rest dim. Picking one makes it
 * the query. One row that scrolls if the capsules outgrow the content width.
 */
@Composable
internal fun SearchSuggestions(suggestions: List<SearchSuggestion>, onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier.focusRestorer().testTag(SEARCH_SUGGESTIONS_TAG),
        // The first capsule hangs out by its padding, so its glyph starts on the margin.
        contentPadding = PaddingValues(start = TvGrid.sideMargin - CAPSULE_START, end = TvGrid.sideMargin),
        horizontalArrangement = Arrangement.spacedBy(CAPSULE_GAP)
    ) {
        items(suggestions, key = SearchSuggestion::text, contentType = { "suggestion" }) { suggestion ->
            SuggestionCapsule(suggestion = suggestion, onClick = { onPick(suggestion.text) })
        }
    }
}

/** Unfocused: flat fill with the glass edge. Focused: the solid light control, no edge. */
@Composable
private fun SuggestionCapsule(suggestion: SearchSuggestion, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Surface(
        onClick = onClick,
        modifier = Modifier.height(CAPSULE_HEIGHT),
        shape = ClickableSurfaceDefaults.shape(shape = TvShapes.pill),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = CAPSULE_FILL,
            contentColor = TvColors.textPrimary,
            focusedContainerColor = TvColors.focusedFill,
            focusedContentColor = TvColors.focusedContent,
            pressedContainerColor = TvColors.focusedFill,
            pressedContentColor = TvColors.focusedContent
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = CONTROL_FOCUS_SCALE),
        border = ClickableSurfaceDefaults.border(border = glassEdgeBorder(TvShapes.pill), focusedBorder = Border.None),
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart).padding(start = CAPSULE_START, end = CAPSULE_END),
            horizontalArrangement = Arrangement.spacedBy(ICON_TO_TEXT),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painter = painterResource(R.drawable.ic_tab_search), contentDescription = null, modifier = Modifier.size(CAPSULE_ICON))
            Text(text = suggestionText(suggestion, dim = if (isFocused) FOCUSED_DIM else TvColors.textSecondary), style = suggestionStyle, maxLines = 1)
        }
    }
}

/** The typed part in the content colour, the rest in [dim]. */
private fun suggestionText(suggestion: SearchSuggestion, dim: Color): AnnotatedString = buildAnnotatedString {
    val text = suggestion.text
    withStyle(SpanStyle(color = dim)) { append(text.substring(0, suggestion.matchStart)) }
    append(text.substring(suggestion.matchStart, suggestion.matchEnd))
    withStyle(SpanStyle(color = dim)) { append(text.substring(suggestion.matchEnd)) }
}

const val SEARCH_SUGGESTIONS_TAG = "search-suggestions"

/** Spec section 2: a focused control scales 1.05. */
internal const val CONTROL_FOCUS_SCALE = 1.05f

// Measured on 39-search-results.png (not in the spec): capsule 66 pt tall (spec 64), fill white
// 16% (#50585B, lighter than the 22% platter), glyph 35 pt, text 38 pt (Headline size).
internal val CAPSULE_HEIGHT = 33.dp
private val CAPSULE_FILL = Color.White.copy(alpha = 0.16f)
private val CAPSULE_GAP = 10.dp
private val CAPSULE_START = 10.5.dp
private val CAPSULE_END = 13.dp
private val CAPSULE_ICON = 24.dp
private val ICON_TO_TEXT = 7.5.dp
private val FOCUSED_DIM = Color(0xFF141414).copy(alpha = 0.6f)
private val suggestionStyle = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Medium, lineHeight = 23.sp)
