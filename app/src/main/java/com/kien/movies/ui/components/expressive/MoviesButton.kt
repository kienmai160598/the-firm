@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.components.expressive

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonBorder
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ButtonScale
import androidx.tv.material3.ButtonShape
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.Text
import com.kien.movies.ui.theme.ExpressiveColors
import com.kien.movies.ui.theme.ExpressiveFocus
import com.kien.movies.ui.theme.MoviesTheme
import com.kien.movies.ui.theme.bodyFloor
import com.kien.movies.ui.theme.tvControlHeight

/** How a [MoviesButton] rests and answers focus. */
internal enum class MoviesButtonStyle {
    /** Frosted capsule at rest, solid white with black content on focus. */
    Secondary,

    /** The screen's main action: a brighter frosted capsule at rest, white on focus. */
    Primary,

    /** A chosen option in a group: the brighter frosted fill, white on focus. */
    Selected,

    /** Deletes or signs out: frosted at rest, Netflix red with white content on focus. */
    Destructive
}

private val ButtonIconSize: Dp = 20.dp
private val ButtonIconGap: Dp = 8.dp
private const val DISABLED_FILL_ALPHA = 0.06f

/** A capsule in every state, as tvOS and M3E draw their buttons. */
internal val moviesButtonShape: ButtonShape = ButtonDefaults.shape(
    shape = M3eShapes.capsule,
    focusedShape = M3eShapes.capsule,
    pressedShape = M3eShapes.capsule,
    disabledShape = M3eShapes.capsule,
    focusedDisabledShape = M3eShapes.capsule
)

/** Grows a focused button on the shared control scale; pressing settles it back. */
internal val moviesButtonScale: ButtonScale = ButtonDefaults.scale(
    focusedScale = ExpressiveFocus.CONTROL_SCALE,
    pressedScale = 1f
)

/** No outline in any state: focus reads from the white fill. */
@Composable
internal fun moviesButtonBorder(): ButtonBorder = ButtonDefaults.border(
    border = Border.None,
    focusedBorder = Border.None,
    pressedBorder = Border.None,
    disabledBorder = Border.None,
    focusedDisabledBorder = Border.None
)

@Composable
internal fun moviesButtonColors(style: MoviesButtonStyle = MoviesButtonStyle.Secondary): ButtonColors {
    val restingFill = when (style) {
        MoviesButtonStyle.Primary, MoviesButtonStyle.Selected -> ExpressiveColors.frostedStrong
        MoviesButtonStyle.Secondary, MoviesButtonStyle.Destructive -> ExpressiveColors.frosted
    }
    val destructive = style == MoviesButtonStyle.Destructive
    return ButtonDefaults.colors(
        containerColor = restingFill,
        contentColor = ExpressiveColors.textPrimary,
        focusedContainerColor = if (destructive) ExpressiveColors.accentRed else ExpressiveColors.focus,
        focusedContentColor = if (destructive) Color.White else ExpressiveColors.onFocus,
        pressedContainerColor = if (destructive) ExpressiveColors.accentRed else ExpressiveColors.secondaryPressed,
        pressedContentColor = if (destructive) Color.White else ExpressiveColors.onFocus,
        disabledContainerColor = Color.White.copy(alpha = DISABLED_FILL_ALPHA),
        disabledContentColor = ExpressiveColors.textTertiary
    )
}

/**
 * The app's button: a tv-material Button in the M3E capsule with the Apple TV fill. Its content
 * must read LocalContentColor (Text and Icon do by default) so it turns black on focus.
 */
@Composable
internal fun MoviesButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: MoviesButtonStyle = MoviesButtonStyle.Secondary,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.tvControlHeight(),
        enabled = enabled,
        scale = moviesButtonScale,
        shape = moviesButtonShape,
        colors = moviesButtonColors(style),
        border = moviesButtonBorder(),
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

/** A [MoviesButton] holding a label and an optional leading icon. */
@Composable
internal fun MoviesTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: MoviesButtonStyle = MoviesButtonStyle.Secondary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null
) {
    MoviesButton(
        onClick = onClick,
        modifier = modifier,
        style = style,
        enabled = enabled,
        onLongClick = onLongClick
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(ButtonIconSize))
            Spacer(modifier = Modifier.width(ButtonIconGap))
        }
        Text(text = text, style = MoviesTheme.textStyles.button.bodyFloor())
    }
}

/** A round icon-only button with the same fills as [MoviesButton]. */
@Composable
internal fun MoviesIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: MoviesButtonStyle = MoviesButtonStyle.Secondary,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable BoxScope.() -> Unit
) {
    IconButton(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        enabled = enabled,
        scale = moviesButtonScale,
        shape = moviesButtonShape,
        colors = moviesButtonColors(style),
        border = moviesButtonBorder(),
        interactionSource = interactionSource,
        content = content
    )
}
