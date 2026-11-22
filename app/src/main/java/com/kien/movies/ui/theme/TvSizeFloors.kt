package com.kien.movies.ui.theme

import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The smallest sizes that still read from the sofa on a TV (960x540 dp). */
internal object TvSizeFloors {
    val metaText: TextUnit = 14.sp
    val bodyText: TextUnit = 16.sp
    val rowTitle: TextUnit = 20.sp
    val screenTitle: TextUnit = 28.sp
    val control: Dp = 48.dp
    val controlIcon: Dp = 24.dp

    /** [size] raised to [floor]. */
    fun atLeast(size: TextUnit, floor: TextUnit): TextUnit =
        if (size.value < floor.value) floor else size

    /** [size] raised to [floor]. */
    fun atLeast(size: Dp, floor: Dp): Dp = if (size < floor) floor else size
}

/** A focusable button, chip or row at least [TvSizeFloors.control] tall. */
internal fun Modifier.tvControlHeight(): Modifier = heightIn(min = TvSizeFloors.control)

/** This style with its font size raised to [floor]. */
internal fun TextStyle.atLeast(floor: TextUnit): TextStyle {
    if (!fontSize.isSp || fontSize.value >= floor.value) return this
    val ratio = floor.value / fontSize.value
    val scaledLineHeight =
        if (lineHeight.isSp) (lineHeight.value * ratio).sp else lineHeight
    return copy(fontSize = floor, lineHeight = scaledLineHeight)
}

/** Secondary and metadata text: at least [TvSizeFloors.metaText] on the box. */
internal fun TextStyle.metaFloor(): TextStyle = atLeast(TvSizeFloors.metaText)

/** Body text and button or chip labels: at least [TvSizeFloors.bodyText] on the box. */
internal fun TextStyle.bodyFloor(): TextStyle = atLeast(TvSizeFloors.bodyText)

/** Row and section titles: at least [TvSizeFloors.rowTitle] on the box. */
internal fun TextStyle.rowTitleFloor(): TextStyle = atLeast(TvSizeFloors.rowTitle)

/** Screen titles: at least [TvSizeFloors.screenTitle] on the box. */
internal fun TextStyle.screenTitleFloor(): TextStyle = atLeast(TvSizeFloors.screenTitle)

/** [metaFloor], kept across recompositions: for text inside lazy-row items. */
@Composable
internal fun TextStyle.rememberMetaFloor(): TextStyle = remember(this) { metaFloor() }

/** [bodyFloor], kept across recompositions: for text inside lazy-row items. */
@Composable
internal fun TextStyle.rememberBodyFloor(): TextStyle = remember(this) { bodyFloor() }
