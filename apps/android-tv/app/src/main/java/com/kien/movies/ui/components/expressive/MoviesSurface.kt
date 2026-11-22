@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.components.expressive

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceBorder
import androidx.tv.material3.ClickableSurfaceColors
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.kien.movies.ui.theme.ExpressiveColors
import com.kien.movies.ui.theme.ExpressiveFocus

private const val DISABLED_FILL_ALPHA = 0.06f

/** No ring in any state: focus reads from the fill and the lift. */
@Composable
internal fun moviesSurfaceBorder(): ClickableSurfaceBorder = ClickableSurfaceDefaults.border(
    border = Border.None,
    focusedBorder = Border.None,
    pressedBorder = Border.None,
    disabledBorder = Border.None,
    focusedDisabledBorder = Border.None
)

/** Frosted (or [resting]) at rest, brighter while [selected], solid white on focus. */
@Composable
internal fun moviesSurfaceColors(
    selected: Boolean = false,
    focus: MoviesCardFocus = MoviesCardFocus.White,
    resting: Color = ExpressiveColors.frosted
): ClickableSurfaceColors {
    val white = focus == MoviesCardFocus.White
    val focusedContainer = if (white) ExpressiveColors.focus else ExpressiveColors.frostedFocused
    val focusedContent = if (white) ExpressiveColors.onFocus else ExpressiveColors.textPrimary
    return ClickableSurfaceDefaults.colors(
        containerColor = if (selected) ExpressiveColors.frostedStrong else resting,
        contentColor = ExpressiveColors.textPrimary,
        focusedContainerColor = focusedContainer,
        focusedContentColor = focusedContent,
        pressedContainerColor = focusedContainer,
        pressedContentColor = focusedContent,
        disabledContainerColor = Color.White.copy(alpha = DISABLED_FILL_ALPHA),
        disabledContentColor = ExpressiveColors.textTertiary
    )
}

/**
 * A clickable tv-material Surface in the kit's look: [shape] in every state (12dp card corner by
 * default, [M3eShapes.capsule] for pills), frosted at rest, white with black content on focus.
 * Content must read LocalContentColor.
 */
@Composable
internal fun MoviesSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    focus: MoviesCardFocus = MoviesCardFocus.White,
    enabled: Boolean = true,
    shape: Shape = M3eShapes.card,
    resting: Color = ExpressiveColors.frosted,
    scale: Float = ExpressiveFocus.CONTROL_SCALE,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        enabled = enabled,
        shape = ClickableSurfaceDefaults.shape(
            shape = shape,
            focusedShape = shape,
            pressedShape = shape,
            disabledShape = shape,
            focusedDisabledShape = shape
        ),
        colors = moviesSurfaceColors(selected = selected, focus = focus, resting = resting),
        scale = ClickableSurfaceDefaults.scale(focusedScale = scale, pressedScale = 1f),
        border = moviesSurfaceBorder(),
        interactionSource = interactionSource,
        content = content
    )
}
