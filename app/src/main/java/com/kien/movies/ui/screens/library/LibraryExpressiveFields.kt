@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.screens.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.tv.material3.Border
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.CardColors
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceBorder
import androidx.tv.material3.ClickableSurfaceColors
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.kien.movies.ui.components.destructiveButtonColors
import com.kien.movies.ui.screens.detail.tonalButtonColors
import com.kien.movies.ui.theme.ExpressiveFocus
import com.kien.movies.ui.theme.MoviesTheme
import com.kien.movies.ui.theme.expressiveFocusScale
import com.kien.movies.ui.theme.isExpressiveBuild

// Wide fields sit side by side in one row, so they grow less than a small control would.
private const val FIELD_FOCUS_SCALE = 1.03f

/** Corner of the library's fields, their menu and the cloud cards: the M3E large shape. */
internal val LibraryContainerShape: Shape = RoundedCornerShape(MoviesTheme.radii.lg)

/** A filled M3 field: surface container high at rest, the accent on focus, no hairline. */
@Composable
internal fun libraryFieldColors(focused: Boolean): CardColors {
    val container = ExpressiveFocus.containerColor(
        focused = focused,
        resting = MoviesTheme.colors.BackgroundCard
    )
    return CardDefaults.colors(containerColor = container, focusedContainerColor = container)
}

@Composable
internal fun Modifier.libraryFieldFocusScale(focused: Boolean): Modifier =
    expressiveFocusScale(focused, FIELD_FOCUS_SCALE)

private val UpstreamDeleteContainer = Color(0xFF4A2323)
private val UpstreamErrorText = Color(0xFFFFB6B6)

/** The cloud search field: a filled M3 field on the box build, tonal on focus, no hairline. */
@Composable
internal fun librarySearchFieldColors(): ClickableSurfaceColors {
    val resting = MoviesTheme.colors.BackgroundCard
    val focused = if (isExpressiveBuild) MoviesTheme.colors.FocusBackground else resting
    return ClickableSurfaceDefaults.colors(
        containerColor = resting,
        focusedContainerColor = focused
    )
}

@Composable
internal fun librarySearchFieldBorder(): ClickableSurfaceBorder {
    if (isExpressiveBuild) return ClickableSurfaceDefaults.border(focusedBorder = Border.None)

    val shape = RoundedCornerShape(MoviesTheme.radii.md)
    return ClickableSurfaceDefaults.border(
        border = Border(
            border = BorderStroke(MoviesTheme.spacing.hairline, MoviesTheme.colors.Border),
            shape = shape
        ),
        focusedBorder = Border(
            border = MoviesTheme.focusRing.border(MoviesTheme.spacing.xxs),
            shape = shape
        )
    )
}

internal fun librarySearchFieldShape(): Shape =
    if (isExpressiveBuild) LibraryContainerShape else RoundedCornerShape(MoviesTheme.radii.md)

@Composable
internal fun libraryDeleteButtonColors(): ButtonColors =
    if (isExpressiveBuild) destructiveButtonColors() else tonalButtonColors(UpstreamDeleteContainer)

@Composable
internal fun libraryErrorText(): Color =
    if (isExpressiveBuild) MoviesTheme.colors.Error else UpstreamErrorText
