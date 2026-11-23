package com.kien.movies.design

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

/**
 * Lets the repeats of a held D-pad key through at most once per gate, so a held arrow moves focus
 * at a rate the box can draw instead of queueing a focus move, a scroll and a recomposition per
 * repeat (ported from the old app's `ui/util/DpadThrottleModifier.kt`). The first press always
 * passes; only repeats are gated. A repeat that passes is handled by the normal focus search.
 */
fun Modifier.dpadRepeatThrottle(
    horizontalGateMs: Long = DpadRepeatGate.HORIZONTAL_GATE_MS,
    verticalGateMs: Long = DpadRepeatGate.VERTICAL_GATE_MS
): Modifier = this then DpadThrottleElement(horizontalGateMs, verticalGateMs)

/** The throttle rule, free of Android types so it is tested on the JVM. */
class DpadRepeatGate(private val horizontalGateMs: Long, private val verticalGateMs: Long) {
    private var lastPassedAtMs: Long? = null

    /** True when a repeat at [eventTimeMs] comes too soon after the last one that passed. */
    fun blocks(isVertical: Boolean, eventTimeMs: Long): Boolean {
        val gateMs = if (isVertical) verticalGateMs else horizontalGateMs
        val last = lastPassedAtMs
        if (last != null && eventTimeMs - last < gateMs) return true
        lastPassedAtMs = eventTimeMs
        return false
    }

    companion object {
        const val HORIZONTAL_GATE_MS = 80L
        const val VERTICAL_GATE_MS = 112L
    }
}

private data class DpadThrottleElement(val horizontalGateMs: Long, val verticalGateMs: Long) : ModifierNodeElement<DpadThrottleNode>() {
    override fun create() = DpadThrottleNode(DpadRepeatGate(horizontalGateMs, verticalGateMs))

    override fun update(node: DpadThrottleNode) {
        node.gate = DpadRepeatGate(horizontalGateMs, verticalGateMs)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "dpadRepeatThrottle"
        properties["horizontalGateMs"] = horizontalGateMs
        properties["verticalGateMs"] = verticalGateMs
    }
}

private class DpadThrottleNode(var gate: DpadRepeatGate) :
    Modifier.Node(),
    KeyInputModifierNode {
    override fun onPreKeyEvent(event: KeyEvent): Boolean {
        val native = event.nativeKeyEvent
        if (native.action != AndroidKeyEvent.ACTION_DOWN || native.repeatCount == 0) return false
        val isVertical = when (native.keyCode) {
            AndroidKeyEvent.KEYCODE_DPAD_UP, AndroidKeyEvent.KEYCODE_DPAD_DOWN -> true
            AndroidKeyEvent.KEYCODE_DPAD_LEFT, AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> false
            else -> return false
        }
        return gate.blocks(isVertical, native.eventTime)
    }

    override fun onKeyEvent(event: KeyEvent): Boolean = false
}
