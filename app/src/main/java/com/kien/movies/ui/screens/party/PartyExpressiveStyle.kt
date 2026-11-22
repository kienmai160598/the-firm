@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.screens.party

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.kien.movies.ui.screens.detail.forBuild
import com.kien.movies.ui.theme.MoviesTheme

/** Keypad keys: tonal at rest; focus fills white upstream and with the accent on the box. */
@Composable
internal fun keypadButtonColors(primary: Boolean): ButtonColors {
    val colors = MoviesTheme.colors
    return ButtonDefaults.colors(
        containerColor = if (primary) colors.FocusBackground else colors.BackgroundCard,
        contentColor = colors.TextPrimary,
        focusedContainerColor = forBuild(Color.White, colors.Secondary),
        focusedContentColor = forBuild(Color.Black, colors.OnSecondary)
    )
}

/** The party side sheet takes M3's extra-large corner on the box build. */
internal fun partyPanelShape(): Shape {
    val corner = forBuild(MoviesTheme.spacing.lg, MoviesTheme.radii.xxl)
    return RoundedCornerShape(topStart = corner, bottomStart = corner)
}
