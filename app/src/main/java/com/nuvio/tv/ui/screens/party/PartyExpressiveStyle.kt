@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.party

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.nuvio.tv.ui.screens.detail.forBuild
import com.nuvio.tv.ui.theme.NuvioTheme

/** Keypad keys: tonal at rest; focus fills white upstream and with the accent on the box. */
@Composable
internal fun keypadButtonColors(primary: Boolean): ButtonColors {
    val colors = NuvioTheme.colors
    return ButtonDefaults.colors(
        containerColor = if (primary) colors.FocusBackground else colors.BackgroundCard,
        contentColor = colors.TextPrimary,
        focusedContainerColor = forBuild(Color.White, colors.Secondary),
        focusedContentColor = forBuild(Color.Black, colors.OnSecondary)
    )
}

/** The party side sheet takes M3's extra-large corner on the box build. */
internal fun partyPanelShape(): Shape {
    val corner = forBuild(NuvioTheme.spacing.lg, NuvioTheme.radii.xxl)
    return RoundedCornerShape(topStart = corner, bottomStart = corner)
}
