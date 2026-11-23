package com.kien.movies.ui.screens.search

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.tv.material3.MaterialTheme
import com.kien.movies.ui.components.expressive.M3eShapes
import com.kien.movies.ui.theme.ExpressiveColors

private const val PULSE_MILLIS = 900
private const val PULSE_MAX_SCALE = 1.35f
private const val PULSE_START_ALPHA = 0.2f

// Material 3 emphasized decelerate: the ring leaves fast and settles, like a spring would.
private val PulseEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

private val SearchFieldMinHeight = 64.dp
private const val PLACEHOLDER_ALPHA = 0.5f

/**
 * The search field as tvOS draws it: a large frosted capsule with white text at rest that turns
 * solid white with black text and cursor while it has focus. No outline.
 */
@Composable
internal fun searchFieldColors(): TextFieldColors = TextFieldDefaults.colors(
    focusedContainerColor = ExpressiveColors.focus,
    unfocusedContainerColor = ExpressiveColors.frosted,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    focusedTextColor = ExpressiveColors.onFocus,
    unfocusedTextColor = ExpressiveColors.textPrimary,
    cursorColor = ExpressiveColors.onFocus,
    focusedLeadingIconColor = ExpressiveColors.onFocus,
    unfocusedLeadingIconColor = ExpressiveColors.textSecondary
)

/** The placeholder dims with the field's text and turns black with it on focus. */
internal fun searchPlaceholderColor(fieldFocused: Boolean): Color = if (fieldFocused) {
    ExpressiveColors.onFocus.copy(alpha = PLACEHOLDER_ALPHA)
} else {
    ExpressiveColors.textTertiary
}

internal fun Modifier.searchFieldSize(): Modifier = heightIn(min = SearchFieldMinHeight)

/** The query in the field: the row title size, so it reads from the sofa. */
@Composable
internal fun searchFieldTextStyle(): TextStyle = MaterialTheme.typography.titleMedium.merge(TextStyle(textDirection = TextDirection.Content))

/** A magnifier leading the field. */
internal fun searchFieldLeadingIcon(): (@Composable () -> Unit) = { Icon(imageVector = Icons.Default.Search, contentDescription = null) }

/** The listening pulse; composed only while listening, so an idle search screen runs no frames. */
@Composable
internal fun VoicePulseRing(color: Color, modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "voicePulse")
    val progress by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(PULSE_MILLIS, easing = PulseEasing)),
        label = "voicePulseProgress"
    )

    Canvas(modifier = modifier) {
        drawCircle(
            color = color.copy(alpha = PULSE_START_ALPHA * (1f - progress)),
            radius = size.minDimension / 2f * lerp(1f, PULSE_MAX_SCALE, progress)
        )
    }
}

/** Discover's filter fields and their menus: the kit's 12dp field corner. */
internal val DiscoverFieldShape: Shape = M3eShapes.card
