package com.kien.movies.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.Text
import coil3.compose.AsyncImage

/**
 * Remote artwork with a flat placeholder (tvOS ArtworkImage.swift). Coil decodes at the laid out
 * size, so give the modifier a fixed size: a poster is never decoded at full resolution. The
 * placeholder fill is drawn only until the image arrives, so a loaded card costs one fill.
 *
 * @param model a URL string, or a Coil `ImageRequest` when the caller needs its own size.
 * @param fallbackTitle shown on the placeholder when there is no URL or the load failed.
 */
@Composable
fun ArtworkImage(
    model: Any?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackTitle: String? = null,
    colorFilter: ColorFilter? = null
) {
    var phase by remember(model) { mutableStateOf(if (model == null) ArtworkPhase.Failed else ArtworkPhase.Loading) }
    val placeholder = if (phase == ArtworkPhase.Loaded) Modifier else Modifier.background(Palette.artworkPlaceholder)
    Box(modifier = modifier.then(placeholder), contentAlignment = Alignment.Center) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                colorFilter = colorFilter,
                onSuccess = { phase = ArtworkPhase.Loaded },
                onError = { phase = ArtworkPhase.Failed }
            )
        }
        if (phase == ArtworkPhase.Failed && fallbackTitle != null) {
            Text(
                text = fallbackTitle,
                style = TextStyles.cardTitle,
                color = Palette.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(CardSize.contentInset)
            )
        }
    }
}

private enum class ArtworkPhase { Loading, Loaded, Failed }
