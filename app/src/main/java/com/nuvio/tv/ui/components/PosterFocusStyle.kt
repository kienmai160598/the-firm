@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.tv.material3.Border
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardScale
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.nuvio.tv.ui.theme.ExpressiveMotion
import com.nuvio.tv.ui.theme.focusLiftOnFocus
import com.nuvio.tv.ui.theme.isExpressiveBuild

// Apple TV focus for media cards (posters, backdrops, continue watching): no ring, the card lifts
// on the focus spring with one soft shadow. tv-material's own scale is turned off so the card
// scales once, through [posterFocusLift]. Other builds get the value they passed in.

/** No ring on a media card on the box build. */
@Composable
internal fun posterFocusBorder(upstream: CardBorder): CardBorder =
    if (!isExpressiveBuild) {
        upstream
    } else {
        CardDefaults.border(
            border = Border.None,
            focusedBorder = Border.None,
            pressedBorder = Border.None
        )
    }

/** tv-material's scale off on the box build: [posterFocusLift] lifts the card instead. */
internal fun posterFocusScale(upstream: CardScale): CardScale =
    if (!isExpressiveBuild) upstream else CardDefaults.scale(focusedScale = 1f)

/** Lifts a media card in [shape] while it holds focus; put it before the card's focus modifiers. */
@Composable
internal fun Modifier.posterFocusLift(
    shape: Shape,
    scale: Float = ExpressiveMotion.EXPRESSIVE_CARD_FOCUS_SCALE
): Modifier = if (!isExpressiveBuild) this else focusLiftOnFocus(shape = shape, scale = scale)
