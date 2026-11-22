package com.nuvio.tv.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.nuvio.tv.R

// The bundled Google Sans Flex is cut to wght 400-700, opsz 14-57 and ROND 0-100. A variable
// font renders its default instance (wght 400, opsz 18) unless every Font sets its axes, so each
// weight below names its own wght, and each role its optical size and roundness, as the
// km-launcher does in its font XMLs.
private val GoogleSansFlexWeights = listOf(
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
    FontWeight.Bold
)

@OptIn(ExperimentalTextApi::class)
private fun googleSansFlex(opticalSize: Float, roundness: Float): FontFamily = FontFamily(
    GoogleSansFlexWeights.map { weight ->
        Font(
            resId = R.font.google_sans_flex,
            weight = weight,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(weight.weight),
                FontVariation.Setting("opsz", opticalSize),
                FontVariation.Setting("ROND", roundness)
            )
        )
    }
)

/** Display and large hero text: half rounded, as the launcher's focused app name. */
internal val GoogleSansFlexDisplay = googleSansFlex(opticalSize = 45f, roundness = 60f)

/** Headlines and screen titles. */
internal val GoogleSansFlexHeadline = googleSansFlex(opticalSize = 28f, roundness = 0f)

/** Titles and body text, and any Text that sets no style. */
internal val GoogleSansFlexFamily = googleSansFlex(opticalSize = 16f, roundness = 0f)

/** Labels, chips, badges and buttons. */
internal val GoogleSansFlexLabel = googleSansFlex(opticalSize = 14f, roundness = 0f)
