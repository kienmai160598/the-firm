package com.kien.movies.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// The box build's corners, in the Apple TV manner: 12dp on cards and rows, 20dp on panels and
// dialogs, capsules on buttons. Other builds keep upstream's tokens untouched.
internal object ExpressiveRadii {
    private val expressive = MoviesRadii.tokens.copy(
        md = 12.dp,
        lg = 12.dp,
        xl = 20.dp,
        xxl = 20.dp,
        panel = 20.dp
    )

    val forBuild: MoviesRadiusTokens = if (isExpressiveBuild) expressive else MoviesRadii.tokens
}

internal object ExpressiveShapes {
    private val capsule = RoundedCornerShape(percent = 50)
    private val card = RoundedCornerShape(ExpressiveRadii.forBuild.md)
    private val panel = RoundedCornerShape(ExpressiveRadii.forBuild.xl)

    private val expressive = MoviesShapes.tokens.copy(
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

    val forBuild: MoviesShapeTokens = if (isExpressiveBuild) expressive else MoviesShapes.tokens
}
