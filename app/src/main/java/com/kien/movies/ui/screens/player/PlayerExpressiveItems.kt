package com.kien.movies.ui.screens.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.tv.material3.Border
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardColors
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardScale
import androidx.tv.material3.CardShape
import androidx.tv.material3.LocalContentColor
import com.kien.movies.ui.theme.ExpressiveFocus
import com.kien.movies.ui.theme.NuvioTheme

/** The Apple TV list row: a focused row fills solid white with black content, others stay clear. */
internal object PlayerListItemStyle {
    const val FOCUS_SCALE = 1.02f
    private const val SUPPORTING_ALPHA = 0.64f
    private const val SELECTED_RESTING_ALPHA = 0.14f

    val shape: CardShape = CardDefaults.shape(RoundedCornerShape(NuvioTheme.radii.lg))
    val stillScale: CardScale = CardDefaults.scale(focusedScale = 1f, pressedScale = 1f)

    @Composable
    fun colors(selected: Boolean = false, resting: Color = Color.Transparent): CardColors =
        CardDefaults.colors(
            containerColor = if (selected) {
                Color.White.copy(alpha = SELECTED_RESTING_ALPHA)
            } else {
                resting
            },
            contentColor = Color.White,
            focusedContainerColor = NuvioTheme.colors.Secondary,
            focusedContentColor = NuvioTheme.colors.OnSecondary,
            pressedContainerColor = NuvioTheme.colors.Secondary,
            pressedContentColor = NuvioTheme.colors.OnSecondary
        )

    @Composable
    fun noBorder(): CardBorder = CardDefaults.border(
        border = Border.None,
        focusedBorder = Border.None,
        pressedBorder = Border.None
    )

    /** The row's own content colour: white at rest, black on the focused fill. Read inside it. */
    @Composable
    fun labelColor(@Suppress("UNUSED_PARAMETER") selected: Boolean = false): Color =
        LocalContentColor.current

    /** Secondary lines of a row, dimmed from the row's content colour. */
    @Composable
    fun supportingColor(): Color = LocalContentColor.current.copy(alpha = SUPPORTING_ALPHA)
}

/** Capsules that answer focus like the rows: white fill and black content while focused. */
internal object PlayerPillStyle {
    @Composable
    fun colors(
        focused: Boolean,
        selected: Boolean = false,
        resting: Color = Color.Transparent
    ): CardColors {
        val container = ExpressiveFocus.containerColor(focused, selected, resting)
        return CardDefaults.colors(
            containerColor = container,
            focusedContainerColor = container,
            pressedContainerColor = container
        )
    }

    @Suppress("UNUSED_PARAMETER")
    fun shape(focused: Boolean): CardShape = CardDefaults.shape(CircleShape)
}

/** The Apple TV lift: a focused part grows a little on a smooth spring, read in graphicsLayer. */
@Composable
internal fun Modifier.playerFocusLift(
    focused: Boolean,
    scale: Float = PlayerFocusLift.CONTROL_SCALE
): Modifier {
    val current = animateFloatAsState(
        targetValue = if (focused) scale else 1f,
        animationSpec = spring(
            dampingRatio = PlayerFocusLift.DAMPING,
            stiffness = PlayerFocusLift.STIFFNESS
        ),
        label = "playerFocusLift"
    )
    return graphicsLayer {
        scaleX = current.value
        scaleY = current.value
    }
}

internal object PlayerFocusLift {
    const val CONTROL_SCALE = 1.04f
    const val DAMPING = 0.8f
    const val STIFFNESS = 600f
}

/** Grows a focused item on the lift spring, for parts that keep no focus state. */
@Composable
internal fun Modifier.focusSpringScale(
    scale: Float = PlayerListItemStyle.FOCUS_SCALE
): Modifier {
    var focused by remember { mutableStateOf(false) }
    return onFocusChanged { focused = it.isFocused }.playerFocusLift(focused, scale)
}
