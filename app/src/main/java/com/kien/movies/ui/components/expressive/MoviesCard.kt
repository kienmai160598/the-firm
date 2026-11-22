@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.components.expressive

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardColors
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardScale
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.kien.movies.ui.theme.ExpressiveColors
import com.kien.movies.ui.theme.ExpressiveFocus

/** How a [MoviesCard] fills when focused. */
internal enum class MoviesCardFocus {
    /** Solid white with black content: its text must read LocalContentColor. */
    White,

    /** A strong frosted fill that keeps fixed white text legible. */
    Frosted
}

internal val moviesCardScale: CardScale = CardDefaults.scale(
    focusedScale = ExpressiveFocus.CONTROL_SCALE,
    pressedScale = 1f
)

/** No ring on a text card: focus reads from its fill and lift. */
@Composable
internal fun moviesCardBorder(): CardBorder = CardDefaults.border(
    border = Border.None,
    focusedBorder = Border.None,
    pressedBorder = Border.None
)

/** Frosted at rest, brighter while [selected]; [focus] picks the focused fill. */
@Composable
internal fun moviesCardColors(
    selected: Boolean = false,
    focus: MoviesCardFocus = MoviesCardFocus.White,
    enabled: Boolean = true
): CardColors {
    val white = focus == MoviesCardFocus.White && enabled
    val focusedContainer = if (white) ExpressiveColors.focus else ExpressiveColors.frostedFocused
    val restingContent = if (enabled) ExpressiveColors.textPrimary else ExpressiveColors.textTertiary
    val focusedContent = if (white) ExpressiveColors.onFocus else restingContent
    return CardDefaults.colors(
        containerColor = if (selected) ExpressiveColors.frostedStrong else ExpressiveColors.frosted,
        contentColor = restingContent,
        focusedContainerColor = focusedContainer,
        focusedContentColor = focusedContent,
        pressedContainerColor = focusedContainer,
        pressedContentColor = focusedContent
    )
}

/**
 * A text card, list row or option tile: 12dp corners, frosted at rest, white on focus. Use
 * [shape] = [M3eShapes.capsule] for a card that acts as a dialog or form action.
 */
@Composable
internal fun MoviesCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    focus: MoviesCardFocus = MoviesCardFocus.White,
    enabled: Boolean = true,
    shape: Shape = M3eShapes.card,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        shape = CardDefaults.shape(shape = shape),
        colors = moviesCardColors(selected = selected, focus = focus, enabled = enabled),
        scale = moviesCardScale,
        border = moviesCardBorder(),
        interactionSource = interactionSource,
        content = content
    )
}
