package com.nuvio.tv.ui.theme

import androidx.compose.ui.unit.dp
import com.nuvio.tv.domain.model.AppTheme
import com.nuvio.tv.core.build.BoxBuild

// The Material 3 Expressive corner scale (androidx material3 ShapeTokens): small parts keep
// their radius, cards and containers move one step rounder, like the box's launcher.
internal object ExpressiveRadii {
    private val expressive = NuvioRadii.tokens.copy(
        lg = 16.dp,
        xl = 20.dp,
        xxl = 28.dp,
        panel = 32.dp
    )

    // Fixed per build from its default theme, not read from the composition, so
    // NuvioTheme.radii stays readable from code that runs outside one.
    val forBuild: NuvioRadiusTokens =
        if (BoxBuild.appTheme == AppTheme.EXPRESSIVE) expressive else NuvioRadii.tokens
}
