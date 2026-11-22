package com.kien.movies.ui.screens.profile

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kien.movies.core.build.BoxBuild
import com.kien.movies.ui.screens.detail.forBuild
import com.kien.movies.ui.theme.ExpressiveColors
import com.kien.movies.ui.theme.ExpressiveFocus
import com.kien.movies.ui.theme.ExpressiveMotion
import com.kien.movies.ui.theme.NuvioFocusRingStyle
import com.kien.movies.ui.theme.NuvioPrimitives
import com.kien.movies.ui.theme.NuvioTheme
import com.kien.movies.ui.theme.ThemeColorPalette
import com.kien.movies.ui.theme.ThemeColors
import com.kien.movies.ui.theme.isExpressiveBuild

private const val HALO_ALPHA = 0.4f
private const val INITIAL_TO_AVATAR = 0.4f

/** The picker shows before any profile's theme applies, so it takes the box build's palette. */
internal val ProfilePickerPalette: ThemeColorPalette =
    ThemeColors.getColorPalette(BoxBuild.appTheme)

/**
 * A profile avatar. On the box build it is a large rounded tile in the Netflix "Who's watching?"
 * manner that answers focus only by lifting (see [profileLift]): no halo, ring or shape change.
 * Elsewhere it squares its corners on focus inside a tonal halo in the profile's own accent.
 */
@Composable
internal fun FocusMorphingProfileAvatar(
    name: String,
    colorHex: String,
    avatarImageUrl: String?,
    focused: Boolean,
    focusProgress: Float,
    avatarSize: Dp,
    haloSize: Dp,
    halo: NuvioFocusRingStyle
) {
    val shape = profileAvatarShape(focused)
    if (isExpressiveBuild) {
        ShapedAvatar(name, colorHex, avatarImageUrl, avatarSize, shape)
        return
    }

    Box(
        modifier = Modifier
            .size(haloSize)
            .background(halo.brush(HALO_ALPHA * focusProgress), shape),
        contentAlignment = Alignment.Center
    ) {
        ShapedAvatar(name, colorHex, avatarImageUrl, avatarSize, shape)
    }
}

@Composable
private fun ShapedAvatar(
    name: String,
    colorHex: String,
    avatarImageUrl: String?,
    size: Dp,
    shape: Shape
) {
    val context = LocalContext.current
    val avatarColor = remember(colorHex) { avatarColorOf(colorHex) }
    val imageRequest = remember(context, avatarImageUrl) {
        avatarImageUrl?.let { url ->
            ImageRequest.Builder(context).data(url).crossfade(true).build()
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(avatarColor),
        contentAlignment = Alignment.Center
    ) {
        if (imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = Color.White,
                fontSize = (size.value * INITIAL_TO_AVATAR).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun avatarColorOf(colorHex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(colorHex)) }
        .getOrDefault(NuvioPrimitives.blue500)

private const val PIN_ERROR_FILL_ALPHA = 0.16f
private const val DISABLED_FILL_ALPHA = 0.5f

/** On the box build a filled, tonal surface carries focus and grouping, so no outline is drawn. */
internal fun Modifier.borderOffOnBox(border: BorderStroke, shape: Shape): Modifier =
    if (isExpressiveBuild) this else this.border(border, shape)

internal fun Modifier.borderOffOnBox(width: Dp, color: Color, shape: Shape): Modifier =
    borderOffOnBox(BorderStroke(width, color), shape)

internal fun profilePanelShape(): Shape =
    RoundedCornerShape(forBuild(PanelCornerUpstream, NuvioTheme.radii.xxl))

internal fun profileTileShape(): Shape =
    RoundedCornerShape(forBuild(TileCornerUpstream, NuvioTheme.radii.xl))

internal fun profileButtonShape(): Shape =
    forBuild(RoundedCornerShape(NuvioTheme.radii.md), CircleShape)

internal fun pinBoxShape(): Shape =
    RoundedCornerShape(forBuild(NuvioTheme.radii.xxs, NuvioTheme.radii.lg))

@Composable
internal fun profileTextColor(): Color = forBuild(Color.White, NuvioTheme.colors.TextPrimary)

@Composable
internal fun profileToastFill(): Color =
    forBuild(Color.Black.copy(alpha = 0.78f), NuvioTheme.colors.BackgroundCard)

/**
 * The name field keeps its focus ring on the box build: its focused fill matches the resting
 * primary button, so the ring is what says which one has focus. Only the resting outline goes.
 */
@Composable
internal fun profileFieldRestingBorder(): Color =
    forBuild(NuvioTheme.colors.Border, Color.Transparent)

@Composable
internal fun profileFieldRestingFill(): Color =
    forBuild(Color.White.copy(alpha = 0.05f), NuvioTheme.colors.BackgroundCard)

/**
 * Upstream fills the primary button with the accent at rest. On the box build only the focused
 * button carries the accent and the primary one rests in the tonal focus tone, so with no ring a
 * focused Cancel never looks like the Create beside it.
 */
@Composable
internal fun overlayButtonFill(enabled: Boolean, primary: Boolean, focused: Boolean): Color {
    val colors = NuvioTheme.colors
    if (!isExpressiveBuild) {
        return when {
            !enabled -> Color.White.copy(alpha = 0.04f)
            primary && focused -> colors.SecondaryVariant
            primary -> colors.Secondary
            focused -> colors.FocusBackground
            else -> Color.White.copy(alpha = 0.06f)
        }
    }
    return when {
        !enabled -> colors.BackgroundCard.copy(alpha = DISABLED_FILL_ALPHA)
        focused -> colors.Secondary
        primary -> colors.FocusBackground
        else -> colors.BackgroundCard
    }
}

@Composable
internal fun overlayButtonContent(enabled: Boolean, primary: Boolean, focused: Boolean): Color {
    val colors = NuvioTheme.colors
    if (!isExpressiveBuild) {
        return when {
            !enabled -> colors.TextSecondary
            primary && focused -> colors.OnSecondaryVariant
            focused -> colors.FocusContent
            primary -> colors.OnSecondary
            else -> colors.TextPrimary
        }
    }
    return when {
        !enabled -> colors.TextSecondary
        focused -> colors.OnSecondary
        else -> colors.TextPrimary
    }
}

@Composable
internal fun pinErrorBorder(): Color = forBuild(Color(0xFFE35D5D), NuvioTheme.colors.Error)

@Composable
internal fun pinErrorFill(): Color = forBuild(
    Color(0xFF311818).copy(alpha = 0.76f),
    NuvioTheme.colors.Error.copy(alpha = PIN_ERROR_FILL_ALPHA)
)

@Composable
internal fun pinErrorText(): Color = forBuild(Color(0xFFFF8E8E), NuvioTheme.colors.Error)

private val PanelCornerUpstream = 20.dp
private val TileCornerUpstream = 18.dp

private const val UPSTREAM_LIFT = 0.04f
private const val PICKER_LIFT = 0.10f
private val PickerAvatarSize = 136.dp
private val CompactPickerAvatarSize = 104.dp
private val PickerAvatarShape = RoundedCornerShape(20.dp)

/** The avatar's corner: a fixed rounded tile on the box build, the morphing pill elsewhere. */
@Composable
internal fun profileAvatarShape(focused: Boolean): Shape =
    if (isExpressiveBuild) PickerAvatarShape else ExpressiveFocus.morphingPill(focused)

internal fun profilePickerAvatarSize(compact: Boolean): Dp =
    if (compact) CompactPickerAvatarSize else PickerAvatarSize

/** The lift on focus: a smooth spring on the box build, upstream's expressive spring elsewhere. */
internal fun profileLiftSpec(): FiniteAnimationSpec<Float> =
    if (isExpressiveBuild) {
        spring(dampingRatio = LIFT_DAMPING, stiffness = Spring.StiffnessMediumLow)
    } else {
        ExpressiveMotion.expressiveFastSpatial()
    }

/** Grows a profile tile with its focus progress, read at draw time so a lift costs no recompose. */
internal fun Modifier.profileLift(focusProgress: State<Float>): Modifier = graphicsLayer {
    val scale = 1f + (if (isExpressiveBuild) PICKER_LIFT else UPSTREAM_LIFT) * focusProgress.value
    scaleX = scale
    scaleY = scale
}

/** The add-profile tile at rest: frosted white on the box build. */
@Composable
internal fun addProfileRestingFill(): Color =
    if (isExpressiveBuild) ExpressiveColors.frosted else NuvioTheme.colors.BackgroundCard

private const val LIFT_DAMPING = 0.8f
