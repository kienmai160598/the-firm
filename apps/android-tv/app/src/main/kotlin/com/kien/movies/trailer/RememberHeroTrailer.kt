package com.kien.movies.trailer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * A [HeroTrailer] for one screen. It stops when the app goes to the background and lets its player
 * go when the screen leaves, as when another screen opens.
 */
@Composable
fun rememberHeroTrailer(): HeroTrailer {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val trailer = remember(context, scope) { HeroTrailer(context, scope) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { trailer.stop() }
    DisposableEffect(trailer) { onDispose { trailer.release() } }
    return trailer
}

/** Plays the trailer of the title [type] [id] while [isDue], and stops it otherwise. */
@Composable
fun PlayTrailerWhile(trailer: HeroTrailer, type: String?, id: String?, isDue: Boolean) {
    LaunchedEffect(trailer, type, id, isDue) {
        if (isDue && type != null && id != null) trailer.play(type, id) else trailer.stop()
    }
}
