@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import com.nuvio.tv.ui.theme.ExpressiveMaterialTheme
import com.nuvio.tv.ui.theme.LocalNuvioTextStyles
import com.nuvio.tv.ui.theme.TvSizeFloors
import com.nuvio.tv.ui.theme.bodyFloor
import com.nuvio.tv.ui.theme.buildNuvioTextStyles
import com.nuvio.tv.ui.theme.isExpressiveBuild
import com.nuvio.tv.ui.theme.metaFloor
import com.nuvio.tv.ui.theme.rowTitleFloor
import com.nuvio.tv.ui.theme.screenTitleFloor

/**
 * Raises the small steps of the type scale in [content] to the box's 10-foot floors, so the
 * settings, account, addon, plugin and TMDB screens need no per-Text change. A floor only raises
 * a style, so one the theme already raised stays as it is. Other builds compose [content] as is.
 */
@Composable
internal fun TenFootTypography(content: @Composable () -> Unit) {
    if (!isExpressiveBuild) {
        content()
        return
    }
    val base = MaterialTheme.typography
    val floored = remember(base) { base.withTenFootFloors() }
    val textStyles = remember(floored) { buildNuvioTextStyles(floored) }

    CompositionLocalProvider(LocalNuvioTextStyles provides textStyles) {
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme,
            shapes = MaterialTheme.shapes,
            typography = floored,
            content = { ExpressiveMaterialTheme(content) }
        )
    }
}

/** A settings row's title: a row title on the box, upstream's body style elsewhere. */
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

/** A hard-coded secondary text size, raised to the meta floor on the box. */
internal fun metaFontSize(upstream: TextUnit): TextUnit =
    if (isExpressiveBuild && upstream.value < TvSizeFloors.metaText.value) {
        TvSizeFloors.metaText
    } else {
        upstream
    }
