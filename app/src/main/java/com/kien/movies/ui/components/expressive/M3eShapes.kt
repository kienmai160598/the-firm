package com.kien.movies.ui.components.expressive

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import com.kien.movies.ui.theme.ExpressiveFocus
import com.kien.movies.ui.theme.ExpressiveRadii

/**
 * The three corners of the M3E kit: capsules for buttons and chips, 12dp for cards, rows and
 * fields, 20dp for panels and dialogs. Shapes switch per state instead of animating, so a list
 * of controls costs no frame per item on the box.
 */
internal object M3eShapes {
    val capsule: Shape = RoundedCornerShape(percent = ExpressiveFocus.PILL_PERCENT)
    val card: Shape = RoundedCornerShape(ExpressiveRadii.forBuild.md)
    val panel: Shape = RoundedCornerShape(ExpressiveRadii.forBuild.xl)
}
