package com.kien.movies.ui.components

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import com.kien.movies.ui.theme.MoviesComponents
import com.kien.movies.ui.theme.MoviesTheme

val LocalLandscapePosterMode = staticCompositionLocalOf { false }
val LocalAlwaysBackdropWithLogo = staticCompositionLocalOf { false }

@Immutable
data class PosterCardStyle(
    val width: Dp = MoviesComponents.tokens.posterCard.width,
    val height: Dp = MoviesComponents.tokens.posterCard.height,
    val cornerRadius: Dp = MoviesComponents.tokens.posterCard.cornerRadius,
    val focusedBorderWidth: Dp = MoviesComponents.tokens.posterCard.focusedBorderWidth,
    val focusedScale: Float = MoviesTheme.components.posterCard.focusedScale
) {
    val aspectRatio: Float
        get() = width.value / height.value
}

object PosterCardDefaults {
    val Style = PosterCardStyle()
}
