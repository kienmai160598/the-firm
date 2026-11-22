package com.kien.movies.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Text roles of the Apple TV look on the box build. Each takes the style the call site used
// before and returns it unchanged on other builds.

/** A shelf or section title: 20sp SemiBold, as tvOS titles its rows. */
internal fun TextStyle.boxRowTitle(): TextStyle =
    if (isExpressiveBuild) copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold) else this

/** Metadata under a title (year, runtime, genre): 15sp; pair it with TextSecondary. */
internal fun TextStyle.boxMetadata(): TextStyle =
    if (isExpressiveBuild) copy(fontSize = 15.sp) else this
