package com.kien.movies.ui.screens.search

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.tv.material3.MaterialTheme
import com.kien.movies.ui.theme.ExpressiveColors
import androidx.tv.material3.Border
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardColors
import androidx.tv.material3.CardDefaults
import com.kien.movies.ui.screens.detail.forBuild
import com.kien.movies.ui.screens.detail.tonalButtonColors
import androidx.compose.ui.util.lerp
import com.kien.movies.ui.theme.ExpressiveFocus
import com.kien.movies.ui.theme.NuvioTheme
import com.kien.movies.ui.theme.expressiveFocusScale
import com.kien.movies.ui.theme.isExpressiveBuild

private const val PULSE_MILLIS = 900
private const val PULSE_MAX_SCALE = 1.35f
private const val PULSE_START_ALPHA = 0.2f

// Material 3 emphasized decelerate: the ring leaves fast and settles, like a spring would.
private val PulseEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/** A tonal icon button: accent on focus, tonal while [selected], with the M3E shape change. */
@Composable
internal fun Modifier.expressiveIconButtonSurface(
    focused: Boolean,
    selected: Boolean = false
): Modifier {
    val container = ExpressiveFocus.containerColor(
        focused = focused,
        selected = selected,
        resting = searchRestingFill()
    )
    val shape = if (isExpressiveBuild) CircleShape else ExpressiveFocus.morphingPill(focused)
    return expressiveFocusScale(focused).background(container, shape)
}

/** A resting control on the search screens: frosted white on the box build. */
@Composable
internal fun searchRestingFill(): Color =
    if (isExpressiveBuild) ExpressiveColors.frosted else NuvioTheme.colors.BackgroundCard

private val SearchFieldMinHeight = 64.dp
private const val PLACEHOLDER_ALPHA = 0.5f

/**
 * The search field on the box build, as tvOS draws it: a large frosted capsule with white text at
 * rest that turns solid white with black text and cursor while it has focus. No outline.
 */
@Composable
internal fun searchFieldColors(): TextFieldColors {
    if (!isExpressiveBuild) {
        return TextFieldDefaults.colors(
            focusedContainerColor = NuvioTheme.colors.BackgroundCard,
            unfocusedContainerColor = NuvioTheme.colors.BackgroundCard,
            focusedIndicatorColor = NuvioTheme.colors.FocusRing,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = NuvioTheme.colors.TextPrimary,
            unfocusedTextColor = NuvioTheme.colors.TextPrimary,
            cursorColor = NuvioTheme.colors.FocusRing
        )
    }
    return TextFieldDefaults.colors(
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
}

/** The placeholder dims with the field's text and turns black with it on focus. */
@Composable
internal fun searchPlaceholderColor(fieldFocused: Boolean): Color = when {
    !isExpressiveBuild -> NuvioTheme.colors.TextTertiary
    fieldFocused -> ExpressiveColors.onFocus.copy(alpha = PLACEHOLDER_ALPHA)
    else -> ExpressiveColors.textTertiary
}

internal fun Modifier.searchFieldSize(): Modifier =
    if (isExpressiveBuild) heightIn(min = SearchFieldMinHeight) else this

/** The query in the field: the row title size on the box build, so it reads from the sofa. */
@Composable
internal fun searchFieldTextStyle(): TextStyle {
    val base = TextStyle(textDirection = TextDirection.Content)
    return if (isExpressiveBuild) MaterialTheme.typography.titleMedium.merge(base) else base
}

/** A magnifier leading the field on the box build; none upstream. */
internal fun searchFieldLeadingIcon(): (@Composable () -> Unit)? =
    if (isExpressiveBuild) {
        { Icon(imageVector = Icons.Default.Search, contentDescription = null) }
    } else {
        null
    }

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

/** Recent searches: tonal rows that fill with the accent on focus on the box build. */
@Composable
internal fun recentSearchButtonColors(): ButtonColors {
    if (isExpressiveBuild) return tonalButtonColors(ExpressiveColors.frosted)

    return ButtonDefaults.colors(
        containerColor = NuvioTheme.colors.BackgroundCard,
        contentColor = NuvioTheme.colors.TextPrimary,
        focusedContainerColor = NuvioTheme.colors.FocusBackground,
        focusedContentColor = NuvioTheme.colors.Primary
    )
}

internal fun recentRowShape(): Shape =
    RoundedCornerShape(forBuild(NuvioTheme.radii.md, NuvioTheme.radii.lg))

internal fun recentClearShape(): Shape =
    forBuild(RoundedCornerShape(NuvioTheme.radii.md), CircleShape)

@Composable
internal fun recentRemoveFocusedContainer(): Color =
    if (isExpressiveBuild) NuvioTheme.colors.Secondary else NuvioTheme.colors.FocusBackground

@Composable
internal fun recentRemoveTint(focused: Boolean): Color {
    val colors = NuvioTheme.colors
    return if (isExpressiveBuild && focused) colors.OnSecondary else colors.TextPrimary
}

private val UpstreamFieldCorner = 14.dp

/** Discover's filter fields and their menus: the M3E large corner on the box build. */
internal val DiscoverFieldShape: Shape =
    RoundedCornerShape(forBuild(UpstreamFieldCorner, NuvioTheme.radii.lg))

/** A filled M3 field on the box build: no hairline, the accent on focus. */
@Composable
internal fun discoverFieldColors(focused: Boolean): CardColors {
    if (!isExpressiveBuild) {
        return CardDefaults.colors(
            containerColor = NuvioTheme.colors.BackgroundCard,
            focusedContainerColor = NuvioTheme.colors.FocusBackground
        )
    }
    val container = ExpressiveFocus.containerColor(focused, resting = searchRestingFill())
    return CardDefaults.colors(containerColor = container, focusedContainerColor = container)
}

@Composable
internal fun discoverFieldBorder(): CardBorder {
    if (isExpressiveBuild) return CardDefaults.border(focusedBorder = Border.None)

    val shape = RoundedCornerShape(UpstreamFieldCorner)
    return CardDefaults.border(
        border = Border(
            border = BorderStroke(NuvioTheme.spacing.hairline, NuvioTheme.colors.Border),
            shape = shape
        ),
        focusedBorder = Border(
            border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
            shape = shape
        )
    )
}

@Composable
internal fun discoverFieldContent(focused: Boolean, resting: Color): Color =
    if (isExpressiveBuild) ExpressiveFocus.contentColor(focused, resting = resting) else resting

@Composable
internal fun discoverFieldIconTint(focused: Boolean): Color {
    if (isExpressiveBuild) return ExpressiveFocus.contentColor(focused)

    return if (focused) NuvioTheme.colors.FocusRing else NuvioTheme.colors.TextSecondary
}

@Composable
internal fun discoverMenuBorder(): BorderStroke? = if (isExpressiveBuild) {
    null
} else {
    BorderStroke(NuvioTheme.spacing.hairline, NuvioTheme.colors.Border)
}
