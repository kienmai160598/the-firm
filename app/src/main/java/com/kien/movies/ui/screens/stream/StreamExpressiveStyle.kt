@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.screens.stream

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.tv.material3.Border
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api

private const val PANEL_SCRIM_ALPHA = 0.45f

/** The same [outline] in every state, so tv-material's default focus ring never shows. */
@Composable
internal fun streamCardBorder(outline: Border): CardBorder =
    CardDefaults.border(border = outline, focusedBorder = outline, pressedBorder = outline)

/** The panel that holds the stream list: a translucent dark scrim, never a blur. */
internal object StreamPanelStyle {
    val usesBlur: Boolean = false

    @Suppress("UNUSED_PARAMETER")
    fun background(blurred: Boolean): Color = Color.Black.copy(alpha = PANEL_SCRIM_ALPHA)
}

/** The title column: left aligned like Apple TV. */
internal object StreamTitleColumnStyle {
    val horizontalAlignment: Alignment.Horizontal = Alignment.Start
    val logoAlignment: Alignment = Alignment.CenterStart
    val textAlign: TextAlign = TextAlign.Start
}

/** Horizontal stops of the backdrop scrim, dark behind the title column and over the list. */
internal fun streamBackdropScrimStops(): List<Pair<Float, Float>> =
    listOf(0.0f to 0.92f, 0.30f to 0.70f, 0.50f to 0.45f, 0.75f to 0.55f, 1.0f to 0.75f)
