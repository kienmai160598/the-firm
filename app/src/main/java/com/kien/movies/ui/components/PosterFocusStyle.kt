@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.tv.material3.Border
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardScale
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.kien.movies.ui.theme.ExpressiveMotion
import com.kien.movies.ui.theme.focusLiftOnFocus

// Apple TV focus for media cards (posters, backdrops, continue watching): no ring, the card lifts
// on the focus spring with one soft shadow. tv-material's own scale is turned off so the card
// scales once, through [posterFocusLift].

/** No ring on a media card. */
@Composable
internal fun posterFocusBorder(): CardBorder = CardDefaults.border(
    border = Border.None,
    focusedBorder = Border.None,
    pressedBorder = Border.None
)

/** tv-material's scale off: [posterFocusLift] lifts the card instead. */
internal val posterFocusScale: CardScale = CardDefaults.scale(focusedScale = 1f)

/** Lifts a media card in [shape] while it holds focus; put it before the card's focus modifiers. */
@Composable
internal fun Modifier.posterFocusLift(
    shape: Shape,
    scale: Float = ExpressiveMotion.EXPRESSIVE_CARD_FOCUS_SCALE
): Modifier = focusLiftOnFocus(shape = shape, scale = scale)
