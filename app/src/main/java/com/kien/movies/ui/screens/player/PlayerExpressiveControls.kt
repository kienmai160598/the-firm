package com.kien.movies.ui.screens.player

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ButtonScale
import androidx.tv.material3.ButtonShape
import androidx.tv.material3.CardColors
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.IconButtonDefaults
import com.kien.movies.ui.theme.ExpressiveFocus
import com.kien.movies.ui.theme.MoviesTheme

/**
 * Apple TV transport buttons: a frosted circle at rest that the video shows through, solid white
 * with a black icon on focus. tv-material's own focus scale is off; playerFocusLift grows them.
 */
internal object PlayerControlStyle {
    private const val RESTING_CONTAINER_ALPHA = 0.14f
    private const val ACTIVE_CONTAINER_ALPHA = 0.36f
    private const val RESTING_STEP_ALPHA = 0.14f
    private const val DISABLED_STEP_ALPHA = 0.04f
    private const val DISABLED_CONTENT_ALPHA = 0.35f

    val stillScale: ButtonScale = ButtonDefaults.scale(focusedScale = 1f)

    @Composable
    fun iconButtonColors(contentColor: Color, active: Boolean = false): ButtonColors =
        IconButtonDefaults.colors(
            containerColor = Color.White.copy(
                alpha = if (active) ACTIVE_CONTAINER_ALPHA else RESTING_CONTAINER_ALPHA
            ),
            contentColor = contentColor,
            focusedContainerColor = MoviesTheme.colors.Secondary,
            focusedContentColor = MoviesTheme.colors.OnSecondary
        )

    val capsuleShape: ButtonShape = ButtonDefaults.shape(shape = CircleShape)

    /** A filled tonal pill for a primary action, a surface pill for the others. */
    @Composable
    fun pillButtonColors(focused: Boolean, primary: Boolean): ButtonColors {
        val container = ExpressiveFocus.containerColor(
            focused = focused,
            resting = if (primary) {
                MoviesTheme.colors.SecondaryVariant
            } else {
                Color.White.copy(alpha = RESTING_CONTAINER_ALPHA)
            }
        )
        val content = ExpressiveFocus.contentColor(
            focused = focused,
            resting = if (primary) MoviesTheme.colors.OnSecondaryVariant else Color.White
        )

        return ButtonDefaults.colors(
            containerColor = container,
            contentColor = content,
            focusedContainerColor = container,
            focusedContentColor = content,
            pressedContainerColor = container,
            pressedContentColor = content
        )
    }

    /** The minus and plus steppers of the audio panel; tv-material picks the focused pair. */
    @Composable
    fun stepColors(enabled: Boolean): CardColors = CardDefaults.colors(
        containerColor = Color.White.copy(
            alpha = if (enabled) RESTING_STEP_ALPHA else DISABLED_STEP_ALPHA
        ),
        contentColor = Color.White.copy(alpha = if (enabled) 1f else DISABLED_CONTENT_ALPHA),
        focusedContainerColor = MoviesTheme.colors.Secondary,
        focusedContentColor = MoviesTheme.colors.OnSecondary
    )
}
