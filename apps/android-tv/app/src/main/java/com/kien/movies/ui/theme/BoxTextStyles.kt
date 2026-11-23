package com.kien.movies.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Text roles of the Apple TV look.

/** A shelf or section title: 20sp SemiBold, as tvOS titles its rows. */
internal fun TextStyle.boxRowTitle(): TextStyle = copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
