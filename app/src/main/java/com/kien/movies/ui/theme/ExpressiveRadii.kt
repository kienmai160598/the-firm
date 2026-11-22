package com.kien.movies.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// Corners in the Apple TV manner: 12dp on cards and rows, 20dp on panels and dialogs, capsules
// on buttons.
internal object ExpressiveRadii {
    val tokens: MoviesRadiusTokens = MoviesRadii.tokens.copy(
        md = 12.dp,
        lg = 12.dp,
        xl = 20.dp,
        xxl = 20.dp,
        panel = 20.dp
    )
}

internal object ExpressiveShapes {
    private val capsule = RoundedCornerShape(percent = 50)
    private val card = RoundedCornerShape(ExpressiveRadii.tokens.md)
    private val panel = RoundedCornerShape(ExpressiveRadii.tokens.xl)

    val tokens: MoviesShapeTokens = MoviesShapes.tokens.copy(
        posterCard = card,
        backdropCard = card,
        collectionCard = card,
        button = capsule,
        iconButton = capsule,
        dialog = panel,
        sidePanel = panel,
        menu = card,
        field = card
    )
}
