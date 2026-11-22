package com.kien.movies.ui.screens.player

import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.kien.movies.ui.theme.isExpressiveBuild
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import androidx.tv.material3.Typography as TvTypography

private const val MIN_LINE_HEIGHT_RATIO = 1.25f

/**
 * The smallest sizes the player may draw at ten feet on the box's 960 x 540 dp screen.
 * Sizes only ever grow to these floors; nothing larger shrinks.
 */
internal object PlayerTenFoot {
    val metaText: TextUnit = 14.sp
    val bodyText: TextUnit = 16.sp
    val timeText: TextUnit = 16.sp
    val titleText: TextUnit = 24.sp
    val controlIcon: Dp = 28.dp
    val sideMargin: Dp = 48.dp
    val edgeMargin: Dp = 28.dp

    /** [size] raised to [floor] on the box build, untouched elsewhere. */
    fun atLeast(size: TextUnit, floor: TextUnit): TextUnit =
        if (isExpressiveBuild && size.value < floor.value) floor else size

    fun atLeast(size: Dp, floor: Dp): Dp =
        if (isExpressiveBuild) maxOf(size, floor) else size
}

internal fun TextStyle.atLeast(floor: TextUnit): TextStyle {
    if (!isExpressiveBuild || fontSize.value >= floor.value) return this

    val roomyLineHeight = (floor.value * MIN_LINE_HEIGHT_RATIO).sp
    val raisedLineHeight = if (lineHeight.isSp && lineHeight.value < roomyLineHeight.value) {
        roomyLineHeight
    } else {
        lineHeight
    }
    return copy(fontSize = floor, lineHeight = raisedLineHeight)
}

/**
 * Raises the small type steps under the player: captions and labels to the meta floor, body and
 * button labels to the body floor. Both the tv and the androidx material3 themes are covered,
 * since the side panels read the latter.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun PlayerTenFootType(content: @Composable () -> Unit) {
    if (!isExpressiveBuild) return content()

    val tvTypography = TvMaterialTheme.typography
    val materialTypography = MaterialTheme.typography
    val flooredTv = remember(tvTypography) { tvTypography.withTenFootFloors() }
    val flooredMaterial = remember(materialTypography) { materialTypography.withTenFootFloors() }
    val appIndication = LocalIndication.current

    TvMaterialTheme(
        colorScheme = TvMaterialTheme.colorScheme,
        shapes = TvMaterialTheme.shapes,
        typography = flooredTv
    ) {
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme,
            shapes = MaterialTheme.shapes,
            typography = flooredMaterial
        ) {
            CompositionLocalProvider(LocalIndication provides appIndication, content = content)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
private fun TvTypography.withTenFootFloors(): TvTypography = copy(
    titleSmall = titleSmall.atLeast(PlayerTenFoot.metaText),
    bodyMedium = bodyMedium.atLeast(PlayerTenFoot.bodyText),
    bodySmall = bodySmall.atLeast(PlayerTenFoot.metaText),
    labelLarge = labelLarge.atLeast(PlayerTenFoot.bodyText),
    labelMedium = labelMedium.atLeast(PlayerTenFoot.metaText),
    labelSmall = labelSmall.atLeast(PlayerTenFoot.metaText)
)

private fun Typography.withTenFootFloors(): Typography = copy(
    titleSmall = titleSmall.atLeast(PlayerTenFoot.metaText),
    bodyMedium = bodyMedium.atLeast(PlayerTenFoot.bodyText),
    bodySmall = bodySmall.atLeast(PlayerTenFoot.metaText),
    labelLarge = labelLarge.atLeast(PlayerTenFoot.bodyText),
    labelMedium = labelMedium.atLeast(PlayerTenFoot.metaText),
    labelSmall = labelSmall.atLeast(PlayerTenFoot.metaText)
)
