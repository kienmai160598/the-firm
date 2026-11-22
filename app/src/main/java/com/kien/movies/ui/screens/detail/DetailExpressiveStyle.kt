@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.screens.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.tv.material3.LocalContentColor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ButtonScale
import androidx.tv.material3.ButtonShape
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardColors
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardScale
import androidx.tv.material3.CardShape
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.kien.movies.ui.theme.ExpressiveColors
import com.kien.movies.ui.theme.ExpressiveFocus
import com.kien.movies.ui.theme.NuvioPrimitives
import com.kien.movies.ui.theme.NuvioTheme
import com.kien.movies.ui.theme.expressiveFocusScale
import com.kien.movies.ui.theme.isExpressiveBuild

/** What a hero button needs to answer focus the Material 3 Expressive way, without a ring. */
internal class HeroButtonLook(
    val interactionSource: MutableInteractionSource,
    val focusScale: Modifier,
    val shape: ButtonShape,
    val colors: ButtonColors,
    val scale: ButtonScale = ButtonScale.None
)

/** Play and the icon actions rest frosted and turn solid white with black content on focus. */
@Composable
internal fun heroPlayLook(): HeroButtonLook = heroButtonLook(
    upstreamResting = NuvioTheme.colors.SecondaryVariant,
    upstreamRestingContent = NuvioTheme.colors.OnSecondaryVariant
)

@Composable
internal fun heroIconLook(selected: Boolean = false): HeroButtonLook = heroButtonLook(
    upstreamResting = NuvioTheme.colors.BackgroundCard,
    upstreamRestingContent = NuvioTheme.colors.TextPrimary,
    selected = selected
)

// The hero sits on top of the whole details page, so its buttons switch colour per focus state
// inside tv-material rather than animating them in composition: an animated colour or shape
// read here recomposes the button on every spring frame. Only the scale springs, in a layer.
private val HeroCapsuleShape = ButtonDefaults.shape(
    shape = CircleShape,
    focusedShape = CircleShape,
    pressedShape = CircleShape
)

@Composable
private fun heroButtonLook(
    upstreamResting: Color,
    upstreamRestingContent: Color,
    selected: Boolean = false
): HeroButtonLook {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val colors = NuvioTheme.colors
    val resting = forBuild(upstreamResting, ExpressiveColors.frosted)
    val restingContent = forBuild(upstreamRestingContent, colors.TextPrimary)
    val selectedContainer = forBuild(colors.FocusBackground, ExpressiveColors.frostedStrong)

    return HeroButtonLook(
        interactionSource = interactionSource,
        focusScale = Modifier.expressiveFocusScale(focused),
        shape = HeroCapsuleShape,
        colors = ButtonDefaults.colors(
            containerColor = if (selected) selectedContainer else resting,
            contentColor = if (selected) colors.TextPrimary else restingContent,
            focusedContainerColor = colors.Secondary,
            focusedContentColor = colors.OnSecondary,
            pressedContainerColor = colors.Secondary,
            pressedContentColor = colors.OnSecondary
        )
    )
}

/**
 * Text tabs in the tvOS manner: bare text at rest, a frosted pill while selected, a solid white
 * pill with black text on focus. Fixed per state, never animated in composition, so moving
 * focus along the season or people tabs recomposes nothing per spring frame.
 */
@Composable
internal fun seasonTabColors(selected: Boolean): CardColors {
    val colors = NuvioTheme.colors
    val resting = when {
        !selected -> forBuild(colors.BackgroundCard, Color.Transparent)
        else -> colors.FocusBackground
    }
    return CardDefaults.colors(
        containerColor = resting,
        contentColor = colors.TextPrimary,
        focusedContainerColor = colors.Secondary,
        focusedContentColor = colors.OnSecondary,
        pressedContainerColor = colors.Secondary,
        pressedContentColor = colors.OnSecondary
    )
}

/** The text colour of a season or people tab, fixed per state. */
@Composable
internal fun textTabContentColor(focused: Boolean, selected: Boolean): Color = when {
    focused -> NuvioTheme.colors.OnSecondary
    selected -> NuvioTheme.colors.TextPrimary
    else -> NuvioTheme.extendedColors.textSecondary
}

// A tv Card draws a focus border by default, so dropping the ring needs an explicit None.
@Composable
internal fun noFocusRingCardBorder(): CardBorder = CardDefaults.border(focusedBorder = Border.None)

private const val PEOPLE_TAB_UPSTREAM_FOCUS_SCALE = 1.03f
private const val PEOPLE_TAB_SELECTED_ALPHA = 0.92f
private const val PEOPLE_TAB_RESTING_ALPHA = 0.55f

// Upstream's people tabs are bare text; on the box they become pills like the top tab bar.
@Composable
internal fun peopleTabColors(focused: Boolean, selected: Boolean): CardColors {
    if (!isExpressiveBuild) {
        return CardDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent
        )
    }
    return seasonTabColors(selected)
}

@Composable
internal fun peopleTabContentColor(focused: Boolean, selected: Boolean): Color {
    if (isExpressiveBuild) return textTabContentColor(focused, selected)

    val primary = NuvioTheme.colors.TextPrimary
    return when {
        focused -> primary
        selected -> primary.copy(alpha = PEOPLE_TAB_SELECTED_ALPHA)
        else -> primary.copy(alpha = PEOPLE_TAB_RESTING_ALPHA)
    }
}

@Composable
internal fun peopleTabBorder(): CardBorder {
    if (isExpressiveBuild) return noFocusRingCardBorder()

    return CardDefaults.border(
        focusedBorder = Border(
            border = BorderStroke(NuvioTheme.spacing.none, Color.Transparent),
            shape = RoundedCornerShape(NuvioTheme.radii.xl)
        )
    )
}

@Composable
internal fun peopleTabShape(): CardShape =
    if (isExpressiveBuild) CardDefaults.shape(CircleShape) else CardDefaults.shape()

@Composable
internal fun peopleTabScale(): CardScale = CardDefaults.scale(
    focusedScale = forBuild(PEOPLE_TAB_UPSTREAM_FOCUS_SCALE, ExpressiveFocus.CONTROL_SCALE)
)

// Title large is 28 sp tall, so 12 dp above and below clears the 48 dp TV control height.
@Composable
internal fun peopleTabPadding(): PaddingValues = if (isExpressiveBuild) {
    PaddingValues(horizontal = NuvioTheme.spacing.lg, vertical = NuvioTheme.spacing.md)
} else {
    PaddingValues(horizontal = NuvioTheme.spacing.xxs, vertical = NuvioTheme.spacing.xxs)
}

private const val ERROR_MESSAGE_TINT = 0.24f
private val UpstreamMessageError = Color(0xFF5A1C1C)
private val UpstreamMessageCorner = 10.dp

/** The detail page's toast: a raised M3 surface, tinted with the theme's error tone if failed. */
@Composable
internal fun detailMessageContainer(isError: Boolean): Color {
    val raised = NuvioTheme.colors.BackgroundElevated
    if (!isError) return raised
    if (!isExpressiveBuild) return UpstreamMessageError

    return NuvioTheme.colors.Error.copy(alpha = ERROR_MESSAGE_TINT)
        .compositeOver(NuvioTheme.colors.BackgroundCard)
}

internal fun detailMessageShape(): Shape =
    RoundedCornerShape(forBuild(UpstreamMessageCorner, NuvioTheme.radii.lg))

private val UpstreamHeroBadgeShape = RoundedCornerShape(6.dp)

/** The hero's outlined age and status tags: pills on the box build, like M3 assist chips. */
internal fun heroBadgeShape(): Shape = forBuild(UpstreamHeroBadgeShape, CircleShape)

private const val UPSTREAM_STARS_FOCUS_ALPHA = 0.32f

/** The star row fills with the focus tone on the box build instead of tinting and ringing. */
@Composable
internal fun ratingStarsContainer(focused: Boolean): Color = when {
    !focused -> Color.Transparent
    isExpressiveBuild -> NuvioTheme.colors.FocusBackground
    else -> NuvioTheme.colors.FocusBackground.copy(alpha = UPSTREAM_STARS_FOCUS_ALPHA)
}

@Composable
internal fun ratingStarsRing(focused: Boolean): Color =
    if (focused && !isExpressiveBuild) NuvioTheme.colors.FocusRing else Color.Transparent

private val UpstreamSeasonChipShape = RoundedCornerShape(14.dp)

// The ratings' season chips sit in a lazy row, so their colours are fixed per state rather
// than animated: tonal while selected, the accent while focused, a pill without a ring.
@Composable
internal fun ratingSeasonChipColors(selected: Boolean): CardColors {
    val colors = NuvioTheme.colors
    val resting = if (selected) colors.FocusBackground else colors.BackgroundCard
    if (!isExpressiveBuild) {
        return CardDefaults.colors(
            containerColor = resting,
            focusedContainerColor = NuvioTheme.colors.FocusBackground
        )
    }
    return CardDefaults.colors(
        containerColor = resting,
        contentColor = NuvioTheme.colors.TextPrimary,
        focusedContainerColor = NuvioTheme.colors.Secondary,
        focusedContentColor = NuvioTheme.colors.OnSecondary
    )
}

@Composable
internal fun ratingSeasonChipContent(): Color =
    if (isExpressiveBuild) LocalContentColor.current else NuvioTheme.colors.TextPrimary

@Composable
internal fun ratingSeasonChipShape(): CardShape =
    CardDefaults.shape(forBuild(UpstreamSeasonChipShape, CircleShape))

@Composable
internal fun ratingSeasonChipBorder(): CardBorder {
    if (isExpressiveBuild) return noFocusRingCardBorder()

    return CardDefaults.border(
        focusedBorder = Border(
            border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
            shape = UpstreamSeasonChipShape
        )
    )
}

// A 16 sp label is 20 sp tall, so 14 dp above and below makes the 48 dp TV control height.
internal fun ratingSeasonChipPadding(): PaddingValues = forBuild(
    upstream = PaddingValues(horizontal = 11.dp, vertical = 6.dp),
    expressive = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
)

private val UpstreamOverlaySurface = Color(0xFF050505)
private val UpstreamOverlayGradientMid = Color(0xFF090909)

/** The episode overlay sits on the theme's background on the box build, not a near-black. */
@Composable
internal fun episodeOverlaySurface(): Color =
    if (isExpressiveBuild) NuvioTheme.colors.Background else UpstreamOverlaySurface

@Composable
internal fun episodeOverlayNoneGradient(): List<Color> {
    if (!isExpressiveBuild) {
        val bottom = NuvioPrimitives.neutral925
        return listOf(UpstreamOverlaySurface, UpstreamOverlayGradientMid, bottom)
    }
    val colors = NuvioTheme.colors
    return listOf(colors.Background, colors.Background, colors.BackgroundElevated)
}

private val UpstreamShuffleGradient =
    listOf(Color(0xFF070707), Color(0xFF101010), Color(0xFF151515))

@Composable
internal fun shufflePreviewGradient(): List<Color> {
    if (!isExpressiveBuild) return UpstreamShuffleGradient

    val colors = NuvioTheme.colors
    return listOf(colors.Background, colors.BackgroundElevated, colors.BackgroundCard)
}

/** For buttons upstream leaves on tv-material defaults: the tonal look on the box build. */
@Composable
internal fun tonalOnBoxButtonColors(): ButtonColors =
    if (isExpressiveBuild) tonalButtonColors() else ButtonDefaults.colors()
