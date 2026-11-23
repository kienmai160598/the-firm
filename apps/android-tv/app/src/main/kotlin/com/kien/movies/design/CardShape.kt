package com.kien.movies.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

/**
 * The one card shape of posters, episodes, Continue Watching and cast (tvOS CardShape.swift from
 * commit 6ad37b6a9f: corner 24 pt, content inset 20 pt). Older tvOS radii of 22 and 18 pt are
 * stale; do not copy them.
 */
object CardShape {
    val shape = RoundedCornerShape(CardSize.cornerRadius)
}

/** Clips artwork to [CardShape]; mirrors `cardArtworkShape()`. */
fun Modifier.cardArtworkShape(): Modifier = clip(CardShape.shape)
