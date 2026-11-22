@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import com.kien.movies.ui.theme.ExpressiveMaterialTheme
import com.kien.movies.ui.theme.LocalMoviesTextStyles
import com.kien.movies.ui.theme.TvSizeFloors
import com.kien.movies.ui.theme.bodyFloor
import com.kien.movies.ui.theme.buildMoviesTextStyles
import com.kien.movies.ui.theme.metaFloor
import com.kien.movies.ui.theme.rowTitleFloor
import com.kien.movies.ui.theme.screenTitleFloor

/**
 * Raises the small steps of the type scale in [content] to the box's 10-foot floors, so the
 * settings, account, addon, plugin and TMDB screens need no per-Text change. A floor only raises
 * a style, so one the theme already raised stays as it is.
 */
@Composable
internal fun TenFootTypography(content: @Composable () -> Unit) {
    val base = MaterialTheme.typography
    val floored = remember(base) { base.withTenFootFloors() }
    val textStyles = remember(floored) { buildMoviesTextStyles(floored) }

    CompositionLocalProvider(LocalMoviesTextStyles provides textStyles) {
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme,
            shapes = MaterialTheme.shapes,
            typography = floored,
            content = { ExpressiveMaterialTheme(content) }
        )
    }
}

/** A settings row's title: the body style raised to the row title floor. */
@Composable
@ReadOnlyComposable
internal fun settingsRowTitleStyle(): TextStyle =
    MaterialTheme.typography.bodyLarge.rowTitleFloor()

private fun Typography.withTenFootFloors(): Typography = copy(
    headlineMedium = headlineMedium.screenTitleFloor(),
    titleMedium = titleMedium.rowTitleFloor(),
    titleSmall = titleSmall.bodyFloor(),
    bodySmall = bodySmall.metaFloor(),
    labelLarge = labelLarge.bodyFloor(),
    labelMedium = labelMedium.bodyFloor(),
    labelSmall = labelSmall.metaFloor()
)

/** A hard-coded secondary text size, raised to the meta floor. */
internal fun metaFontSize(upstream: TextUnit): TextUnit =
    if (upstream.value < TvSizeFloors.metaText.value) {
        TvSizeFloors.metaText
    } else {
        upstream
    }
