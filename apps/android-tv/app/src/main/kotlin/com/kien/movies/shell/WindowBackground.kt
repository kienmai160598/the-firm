package com.kien.movies.shell

import android.graphics.drawable.ColorDrawable
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Sets the screen colour as the window background instead of drawing a full screen fill in
 * Compose: the window draws its background anyway, so a Compose fill on top would be a second
 * full screen pass on a fill rate bound GPU (docs/android-tv-rebuild.md, box performance rules).
 */
@Composable
fun WindowBackground(color: Color) {
    val window = LocalActivity.current?.window ?: return
    SideEffect { window.setBackgroundDrawable(ColorDrawable(color.toArgb())) }
}
