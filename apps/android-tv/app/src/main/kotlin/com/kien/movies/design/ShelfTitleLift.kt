package com.kien.movies.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Moves a shelf title up while the card under it has focus, as the Apple TV app does: the grown
 * card keeps its distance from the title, and the title stays still while a card beside it has
 * focus. The title rises by [rise], what the focus scale adds above the card. Only the title's
 * layer moves, so a focus move recomposes nothing. Create it with [rememberShelfTitleLift].
 */
@Stable
class ShelfTitleLift internal constructor(private val scope: CoroutineScope, private val rise: Dp) {
    private val progress = Animatable(0f)
    private var title: LayoutCoordinates? = null
    private var focusedCard: LayoutCoordinates? = null

    internal fun risePx(density: Density): Float = with(density) { rise.toPx() } * progress.value

    internal fun placeTitle(coordinates: LayoutCoordinates) {
        title = coordinates
    }

    internal fun cardFocusChanged(card: LayoutCoordinates?, isFocused: Boolean) {
        if (isFocused) {
            focusedCard = card
        } else if (focusedCard === card) {
            focusedCard = null
        }
        val target = if (isUnderTitle(focusedCard)) 1f else 0f
        scope.launch { progress.animateTo(target, tween(Focus.animationMillis)) }
    }

    /**
     * A card that starts left of the title's end is under it once the row has scrolled it into
     * view: the row never scrolls a card past the side margin, where the title starts.
     */
    private fun isUnderTitle(card: LayoutCoordinates?): Boolean {
        val titleBounds = title?.takeIf { it.isAttached }?.boundsInWindow() ?: return false
        val cardBounds = card?.takeIf { it.isAttached }?.boundsInWindow() ?: return false
        return cardBounds.left < titleBounds.right
    }
}

/** A [ShelfTitleLift] for cards [height] tall, grown by [scale] on focus. */
@Composable
fun rememberShelfTitleLift(height: Dp, scale: Float = Focus.cardScale): ShelfTitleLift {
    val scope = rememberCoroutineScope()
    return remember(scope, height, scale) { ShelfTitleLift(scope, rise = height * (scale - 1f) / 2f) }
}

/** The title text that [lift] moves; put it after the title's horizontal padding. */
fun Modifier.liftedBy(lift: ShelfTitleLift?): Modifier = if (lift == null) {
    this
} else {
    graphicsLayer { translationY = -lift.risePx(this) }.onPlaced(lift::placeTitle)
}

/** A card of the shelf whose title [lift] moves; put it on the focusable card. */
fun Modifier.liftsShelfTitle(lift: ShelfTitleLift): Modifier = this then LiftsShelfTitleElement(lift)

private data class LiftsShelfTitleElement(val lift: ShelfTitleLift) : ModifierNodeElement<LiftsShelfTitleNode>() {
    override fun create() = LiftsShelfTitleNode(lift)

    override fun update(node: LiftsShelfTitleNode) {
        node.lift = lift
    }
}

private class LiftsShelfTitleNode(var lift: ShelfTitleLift) :
    Modifier.Node(),
    LayoutAwareModifierNode,
    FocusEventModifierNode {
    private var coordinates: LayoutCoordinates? = null
    private var wasFocused = false

    override fun onPlaced(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
    }

    override fun onFocusEvent(focusState: FocusState) {
        if (focusState.hasFocus == wasFocused) return
        wasFocused = focusState.hasFocus
        lift.cardFocusChanged(coordinates, wasFocused)
    }
}
