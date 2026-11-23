@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.screens.detail

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme

// Shared by the detail, stream, search, library, profile, party, cast and ambient screens. Every
// build has the Material 3 Expressive look; the helpers that still take an upstream value ignore
// it and stay only until their last callers move to the expressive value or the M3E kit.

/**
 * The box renders 960 by 540 dp and is read from across a room, so nothing on it may be
 * smaller than these.
 */
internal object TvSizeFloors {
    val metaText: TextUnit = 14.sp
    val bodyText: TextUnit = 16.sp
    val rowTitle: TextUnit = 20.sp
    val screenTitle: TextUnit = 28.sp
    val control: Dp = 48.dp
    val buttonIcon: Dp = 24.dp
}

internal fun Dp.atLeastOnBox(floor: Dp): Dp = max(this, floor)

internal fun TextUnit.atLeastOnBox(floor: TextUnit): TextUnit = if (this.isSpecified && this.value < floor.value) floor else this

/** Raises the font to [floor]; the line height grows by the same step. */
internal fun TextStyle.atLeastOnBox(floor: TextUnit): TextStyle {
    if (!fontSize.isSpecified || fontSize.value >= floor.value) return this

    val step = floor.value - fontSize.value
    val raisedLineHeight = if (lineHeight.isSpecified) (lineHeight.value + step).sp else lineHeight
    return copy(fontSize = floor, lineHeight = raisedLineHeight)
}

/** A tv-material Button's own label style, raised to the TV floor. */
@Composable
internal fun buttonLabelStyle(): TextStyle = MaterialTheme.typography.labelLarge.atLeastOnBox(TvSizeFloors.bodyText)

/** Focusable controls on the box are at least [TvSizeFloors.control] tall. */
internal fun Modifier.tvControlHeight(): Modifier = heightIn(min = TvSizeFloors.control)

// A tv-material Button's label is 22 sp tall on the box, so 13 dp above and below makes 48 dp.
private val BoxButtonPadding = PaddingValues(horizontal = 16.dp, vertical = 13.dp)

/** A tv-material Button's padding, tall enough for the TV control height. */
internal fun tvButtonPadding(): PaddingValues = BoxButtonPadding
