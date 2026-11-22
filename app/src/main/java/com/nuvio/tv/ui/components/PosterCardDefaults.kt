package com.nuvio.tv.ui.components

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import com.nuvio.tv.ui.theme.NuvioComponents
import com.nuvio.tv.ui.theme.NuvioTheme

val LocalLandscapePosterMode = staticCompositionLocalOf { false }
val LocalAlwaysBackdropWithLogo = staticCompositionLocalOf { false }

@Immutable
data class PosterCardStyle(
    val width: Dp = NuvioComponents.tokens.posterCard.width,
    val height: Dp = NuvioComponents.tokens.posterCard.height,
    val cornerRadius: Dp = NuvioComponents.tokens.posterCard.cornerRadius,
    val focusedBorderWidth: Dp = NuvioComponents.tokens.posterCard.focusedBorderWidth,
    val focusedScale: Float = NuvioTheme.components.posterCard.focusedScale
) {
    val aspectRatio: Float
        get() = width.value / height.value
}

object PosterCardDefaults {
    val Style = PosterCardStyle()
}
