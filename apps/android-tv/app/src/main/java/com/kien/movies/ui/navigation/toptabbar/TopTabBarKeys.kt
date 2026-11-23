package com.kien.movies.ui.navigation.toptabbar

import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * A long press on Back runs [onLongPress] before any screen sees the key, and the rest of that
 * press, up to its release, is swallowed so it cannot also trigger a short Back.
 */
internal fun Modifier.onLongPressBack(
    enabled: () -> Boolean,
    held: MutableState<Boolean>,
    onLongPress: () -> Unit
): Modifier = onPreviewKeyEvent { event ->
    if (event.key != Key.Back) return@onPreviewKeyEvent false
    val longPress = event.type == KeyEventType.KeyDown &&
        event.nativeKeyEvent.isLongPress && enabled()
    if (longPress && !held.value) {
        held.value = true
        onLongPress()
    }
    if (held.value && event.type == KeyEventType.KeyUp) {
        held.value = false
        return@onPreviewKeyEvent true
    }
    longPress || held.value
}

internal fun KeyEvent.isUpPress(): Boolean = type == KeyEventType.KeyDown && key == Key.DirectionUp
