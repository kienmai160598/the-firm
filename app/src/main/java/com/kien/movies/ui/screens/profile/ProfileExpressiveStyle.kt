package com.kien.movies.ui.screens.profile

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import com.kien.movies.ui.theme.ExpressiveColors
import com.kien.movies.ui.theme.MoviesPrimitives
import com.kien.movies.ui.theme.MoviesTheme
import com.kien.movies.ui.theme.ThemeColorPalette
import com.kien.movies.ui.theme.ThemeColors

private const val INITIAL_TO_AVATAR = 0.4f

/** The picker shows before any profile's theme applies, so it takes the box build's palette. */
internal val ProfilePickerPalette: ThemeColorPalette =
    ThemeColors.getColorPalette(BoxBuild.appTheme)

/** The avatar's corner: a fixed rounded tile in every focus state. */
internal val ProfileAvatarShape: Shape = RoundedCornerShape(20.dp)

/**
 * A profile avatar: a large rounded tile in the Netflix "Who's watching?" manner that answers
 * focus only by lifting (see [profileLift]): no halo, ring or shape change.
 */
@Composable
internal fun ProfilePickerAvatar(
    name: String,
    colorHex: String,
    avatarImageUrl: String?,
    avatarSize: Dp
) {
    ShapedAvatar(name, colorHex, avatarImageUrl, avatarSize, ProfileAvatarShape)
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
        .getOrDefault(MoviesPrimitives.blue500)

private const val PIN_ERROR_FILL_ALPHA = 0.16f
private const val DISABLED_FILL_ALPHA = 0.06f

internal fun profilePanelShape(): Shape =
    RoundedCornerShape(MoviesTheme.radii.xxl)

internal fun profileTileShape(): Shape =
    RoundedCornerShape(MoviesTheme.radii.xl)

internal fun pinBoxShape(): Shape =
    RoundedCornerShape(MoviesTheme.radii.lg)

@Composable
internal fun profileTextColor(): Color = MoviesTheme.colors.TextPrimary

@Composable
internal fun profileToastFill(): Color =
    MoviesTheme.colors.BackgroundCard

/**
 * The overlay button in the kit's palette: frosted at rest (brighter for the primary action),
 * solid white on focus, a faint fill while disabled.
 */
internal fun overlayButtonFill(enabled: Boolean, primary: Boolean, focused: Boolean): Color =
    when {
        !enabled -> Color.White.copy(alpha = DISABLED_FILL_ALPHA)
        focused -> ExpressiveColors.focus
        primary -> ExpressiveColors.frostedStrong
        else -> ExpressiveColors.frosted
    }

/** Black on the white focused fill, white at rest, tertiary while disabled. */
internal fun overlayButtonContent(enabled: Boolean, focused: Boolean): Color =
    when {
        !enabled -> ExpressiveColors.textTertiary
        focused -> ExpressiveColors.onFocus
        else -> ExpressiveColors.textPrimary
    }

@Composable
internal fun pinErrorBorder(): Color = MoviesTheme.colors.Error

@Composable
internal fun pinErrorFill(): Color = MoviesTheme.colors.Error.copy(alpha = PIN_ERROR_FILL_ALPHA)

@Composable
internal fun pinErrorText(): Color = MoviesTheme.colors.Error

private const val PICKER_LIFT = 0.10f
private val PickerAvatarSize = 136.dp
private val CompactPickerAvatarSize = 104.dp

internal fun profilePickerAvatarSize(compact: Boolean): Dp =
    if (compact) CompactPickerAvatarSize else PickerAvatarSize

/** The lift on focus: a smooth spring. */
internal fun profileLiftSpec(): FiniteAnimationSpec<Float> =
    spring(dampingRatio = LIFT_DAMPING, stiffness = Spring.StiffnessMediumLow)

/** Grows a profile tile with its focus progress, read at draw time so a lift costs no recompose. */
internal fun Modifier.profileLift(focusProgress: State<Float>): Modifier = graphicsLayer {
    val scale = 1f + PICKER_LIFT * focusProgress.value
    scaleX = scale
    scaleY = scale
}

/** The add-profile tile at rest: frosted white. */
internal val addProfileRestingFill: Color = ExpressiveColors.frosted

private const val LIFT_DAMPING = 0.8f
