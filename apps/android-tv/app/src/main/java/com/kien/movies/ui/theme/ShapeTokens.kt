package com.kien.movies.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class MoviesRadiusTokens(
    val none: Dp,
    val xxs: Dp,
    val xs: Dp,
    val sm: Dp,
    val md: Dp,
    val lg: Dp,
    val xl: Dp,
    val xxl: Dp,
    val panel: Dp,
    val full: Dp
)

@Immutable
data class MoviesShapeTokens(
    val posterCard: Shape,
    val backdropCard: Shape,
    val collectionCard: Shape,
    val button: Shape,
    val iconButton: Shape,
    val chip: Shape,
    val badge: Shape,
    val dialog: Shape,
    val sidePanel: Shape,
    val sidebar: Shape,
    val navItem: Shape,
    val progress: Shape,
    val slider: Shape,
    val field: Shape,
    val menu: Shape,
    val circle: Shape
)

object MoviesRadii {
    val tokens = MoviesRadiusTokens(
        none = 0.dp,
        xxs = 2.dp,
        xs = 4.dp,
        sm = 8.dp,
        md = 12.dp,
        lg = 14.dp,
        xl = 16.dp,
        xxl = 20.dp,
        panel = 28.dp,
        full = 999.dp
    )
}

object MoviesShapes {
    val tokens = MoviesShapeTokens(
        posterCard = RoundedCornerShape(MoviesRadii.tokens.md),
        backdropCard = RoundedCornerShape(MoviesRadii.tokens.xl),
        collectionCard = RoundedCornerShape(MoviesRadii.tokens.xl),
        button = RoundedCornerShape(MoviesRadii.tokens.md),
        iconButton = RoundedCornerShape(MoviesRadii.tokens.md),
        chip = RoundedCornerShape(MoviesRadii.tokens.full),
        badge = RoundedCornerShape(MoviesRadii.tokens.xs),
        dialog = RoundedCornerShape(MoviesRadii.tokens.xl),
        sidePanel = RoundedCornerShape(MoviesRadii.tokens.xxl),
        sidebar = RoundedCornerShape(30.dp),
        navItem = RoundedCornerShape(MoviesRadii.tokens.full),
        progress = RoundedCornerShape(MoviesRadii.tokens.xxs),
        slider = RoundedCornerShape(MoviesRadii.tokens.full),
        field = RoundedCornerShape(MoviesRadii.tokens.md),
        menu = RoundedCornerShape(MoviesRadii.tokens.lg),
        circle = CircleShape
    )
}
