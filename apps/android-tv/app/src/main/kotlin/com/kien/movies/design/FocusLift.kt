package com.kien.movies.design

import androidx.compose.animation.core.Animatable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.launch

/**
 * The focus lift of a tv-material control on the tvOS curve: it scales to [scale] with
 * [Motion.focusSpring] when it takes focus and back when focus leaves. Put it on the control's own
 * `modifier` and give the control no scale of its own ([TvGlass.noButtonScale] and the like), so the
 * two never stack.
 *
 * tv-material times its scale with a fixed tween that cannot be changed. Here the scale is set in
 * a graphics layer during placement: an animation frame redraws the layer, never recomposes or
 * measures, and the control keeps its layout size.
 */
fun Modifier.focusLift(scale: Float): Modifier = this then FocusLiftElement(scale)

private data class FocusLiftElement(val scale: Float) : ModifierNodeElement<FocusLiftNode>() {
    override fun create(): FocusLiftNode = FocusLiftNode(scale)

    override fun update(node: FocusLiftNode) {
        node.scale = scale
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "focusLift"
        properties["scale"] = scale
    }
}

private class FocusLiftNode(var scale: Float) :
    Modifier.Node(),
    FocusEventModifierNode,
    LayoutModifierNode {
    private val current = Animatable(1f)

    override fun onFocusEvent(focusState: FocusState) {
        val target = if (focusState.hasFocus) scale else 1f
        if (!isAttached || current.targetValue == target) return
        coroutineScope.launch { current.animateTo(target, Motion.focusSpring) }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                scaleX = current.value
                scaleY = current.value
            }
        }
    }
}
