package com.nuvio.tv.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Apple TV focus on the box build: a focused control fills solid white with black content and
 * lifts a little on the focus spring; a resting one is frosted (white at 14%) with white
 * content; a selected one keeps the frosted fill with white content. No outline rings. The top
 * tab bar is the reference; other controls reuse these so the whole app answers focus alike.
 */
internal object ExpressiveFocus {
    /** A focused button, pill or row lifts this much; cards use the card focus scale. */
    const val CONTROL_SCALE = 1.04f
    const val PILL_PERCENT = 50

    /** Capsules keep their shape on focus, as on tvOS. */
    const val FOCUSED_PILL_PERCENT = 50

    /** Elevation of the one soft shadow under a focused card. */
    val CARD_LIFT_ELEVATION: Dp = 18.dp

    /** Focused fill: solid white. */
    val focusedContainer: Color get() = ExpressiveColors.focus

    /** Content on [focusedContainer]: black. */
    val focusedContent: Color get() = ExpressiveColors.onFocus

    /** Resting fill of a button or pill: white at 14%. */
    val frostedContainer: Color get() = ExpressiveColors.frosted

    @Composable
    fun containerColor(
        focused: Boolean,
        selected: Boolean = false,
        resting: Color = Color.Transparent
    ): Color {
        val color by animateColorAsState(
            targetValue = when {
                focused -> NuvioTheme.colors.Secondary
                selected -> NuvioTheme.colors.FocusBackground
                else -> resting
            },
            animationSpec = ExpressiveMotion.defaultEffects(),
            label = "expressiveContainer"
        )
        return color
    }

    @Composable
    fun contentColor(
        focused: Boolean,
        selected: Boolean = false,
        resting: Color = NuvioTheme.colors.TextSecondary
    ): Color {
        val color by animateColorAsState(
            targetValue = when {
                focused -> NuvioTheme.colors.OnSecondary
                selected -> NuvioTheme.colors.TextPrimary
                else -> resting
            },
            animationSpec = ExpressiveMotion.defaultEffects(),
            label = "expressiveContent"
        )
        return color
    }

    /** A frosted capsule at rest that turns white on focus: the tvOS button fill. */
    @Composable
    fun frostedContainerColor(focused: Boolean, selected: Boolean = false): Color =
        containerColor(
            focused = focused,
            selected = selected,
            resting = ExpressiveColors.frosted
        )

    /** White content at rest, black on a focused white fill. */
    @Composable
    fun frostedContentColor(focused: Boolean): Color =
        contentColor(focused = focused, resting = NuvioTheme.colors.TextPrimary)

    /** A capsule; kept as a function so callers written for the M3E corner morph still work. */
    @Composable
    fun morphingPill(focused: Boolean): Shape {
        val percent by animateFloatAsState(
            targetValue = (if (focused) FOCUSED_PILL_PERCENT else PILL_PERCENT).toFloat(),
            animationSpec = ExpressiveMotion.focusLift(),
            label = "expressivePillCorner"
        )
        return RoundedCornerShape(FractionalCornerSize(percent.coerceIn(0f, 50f)))
    }
}

/** [CornerSize] taking a Float as percent; the Float overload of CornerSize means pixels. */
private data class FractionalCornerSize(private val percent: Float) : CornerSize {
    override fun toPx(shapeSize: Size, density: Density): Float =
        shapeSize.minDimension * percent / 100f
}

/** Grows a focused control on the focus lift spring; [scale] 1 leaves it still. */
@Composable
internal fun Modifier.expressiveFocusScale(
    focused: Boolean,
    scale: Float = ExpressiveFocus.CONTROL_SCALE
): Modifier {
    val current = animateFloatAsState(
        targetValue = if (focused) scale else 1f,
        animationSpec = ExpressiveMotion.focusLift(),
        label = "expressiveFocusScale"
    )
    return graphicsLayer {
        scaleX = current.value
        scaleY = current.value
    }
}

/**
 * The Apple TV lift for a card: scales to [scale] and casts one soft dark shadow in [shape],
 * both following the focus spring. The shadow exists only while lifted, so a row of cards pays
 * for one shadow at most. Values are read in the layer block, so a lift recomposes nothing.
 */
@Composable
internal fun Modifier.focusLift(
    focused: Boolean,
    shape: Shape = RectangleShape,
    scale: Float = ExpressiveMotion.EXPRESSIVE_CARD_FOCUS_SCALE,
    elevation: Dp = ExpressiveFocus.CARD_LIFT_ELEVATION
): Modifier {
    val progress = animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = ExpressiveMotion.focusLift(),
        label = "focusLift"
    )
    return graphicsLayer { applyLift(progress.value, shape, scale, elevation) }
}

/**
 * [focusLift] that follows the focus of the focusable after it in the chain (a tv-material Card
 * or Surface) by itself, so the caller keeps no focus state and a focus move recomposes nothing.
 * Put it before the card's own modifiers that request focus.
 */
@Composable
internal fun Modifier.focusLiftOnFocus(
    shape: Shape = RectangleShape,
    scale: Float = ExpressiveMotion.EXPRESSIVE_CARD_FOCUS_SCALE,
    elevation: Dp = ExpressiveFocus.CARD_LIFT_ELEVATION
): Modifier {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    return this
        .graphicsLayer { applyLift(progress.value, shape, scale, elevation) }
        .onFocusChanged { state ->
            val target = if (state.hasFocus) 1f else 0f
            if (progress.targetValue != target) {
                scope.launch { progress.animateTo(target, ExpressiveMotion.focusLift()) }
            }
        }
}

private fun GraphicsLayerScope.applyLift(
    lift: Float,
    shape: Shape,
    scale: Float,
    elevation: Dp
) {
    val current = 1f + (scale - 1f) * lift
    scaleX = current
    scaleY = current
    if (lift <= 0.01f) return

    shadowElevation = elevation.toPx() * lift.coerceIn(0f, 1f)
    this.shape = shape
    clip = false
    ambientShadowColor = ExpressiveColors.liftShadow
    spotShadowColor = ExpressiveColors.liftShadow
}
