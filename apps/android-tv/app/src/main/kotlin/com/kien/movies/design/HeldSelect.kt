package com.kien.movies.design

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.onPreviewKeyEvent

/**
 * For a menu opened by a long press of select: swallows that press's remaining key events until a
 * new press starts. A real remote keeps sending repeated key downs while select is held, then the
 * key up; without this, the first repeat after the menu opens presses its focused button and the
 * key up clicks it, so holding select picks the first item. Put it on the menu's root, an
 * ancestor of every button.
 */
fun Modifier.ignoringTheHeldSelect(): Modifier = composed {
    var isArmed by remember { mutableStateOf(false) }
    onPreviewKeyEvent { event ->
        val native = event.nativeKeyEvent
        when {
            isArmed || native.keyCode !in SELECT_KEYS -> false

            native.action == AndroidKeyEvent.ACTION_DOWN && native.repeatCount == 0 -> {
                isArmed = true
                false
            }

            else -> true
        }
    }
}

private val SELECT_KEYS = setOf(
    AndroidKeyEvent.KEYCODE_DPAD_CENTER,
    AndroidKeyEvent.KEYCODE_ENTER,
    AndroidKeyEvent.KEYCODE_NUMPAD_ENTER
)
