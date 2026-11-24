package com.kien.movies.streams

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.kien.movies.design.ArtworkImage
import com.kien.movies.design.BakedScrim
import com.kien.movies.design.TvColors
import com.kien.movies.design.rememberBakedImage

/**
 * The receded backdrop behind Streams (tvOS StreamsHeader.swift, StreamsBackground): the film's
 * art blurred and darkened, so the glass rows take its colour. tvOS blurs it at runtime; the box
 * cannot, so the art is decoded tiny and scaled up, which reads as the same soft blur, and the
 * darkening is baked into that bitmap. One opaque image, no blur, no extra layer.
 */
@Composable
fun StreamsBackground(backdropUrl: String?, modifier: Modifier = Modifier) {
    val request = rememberBakedImage(backdropUrl ?: return, RECEDED_DECODE_SIZE, RECEDED_SCRIM)
    ArtworkImage(model = request, modifier = modifier.fillMaxSize())
}

/** About 1/20 of the screen: scaled up with bilinear filtering it softens like tvOS's 40 pt blur. */
private val RECEDED_DECODE_SIZE = DpSize(48.dp, 27.dp)

/** BackdropView.swift receded: black 45% over the art, with the leading and bottom shades. */
private val RECEDED_SCRIM = BakedScrim.backdrop(TvColors.details, dimming = 0.45f)
