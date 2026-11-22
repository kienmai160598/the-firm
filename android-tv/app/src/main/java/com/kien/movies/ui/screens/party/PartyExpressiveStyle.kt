package com.kien.movies.ui.screens.party

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import com.kien.movies.ui.theme.MoviesTheme

/** The party side sheet takes M3's extra-large corner. */
internal fun partyPanelShape(): Shape {
    val corner = MoviesTheme.radii.xxl
    return RoundedCornerShape(topStart = corner, bottomStart = corner)
}
