package com.kien.movies.design

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.size.Precision

/**
 * A title logo request: the transparent margin trimmed (so the ink, not the canvas, fits [box]),
 * decoded at twice [box] and at exact precision, so a small logo is scaled up to fit the box as
 * tvOS `resizable().scaledToFit()` does and stays sharp. Null without a URL.
 */
@Composable
fun rememberLogoRequest(url: String?, box: DpSize): ImageRequest? {
    val context = LocalContext.current
    val density = LocalDensity.current
    return remember(url, box, density) {
        url?.let { data ->
            val widthPx = with(density) { (box.width * LOGO_DECODE_FACTOR).roundToPx() }
            val heightPx = with(density) { (box.height * LOGO_DECODE_FACTOR).roundToPx() }
            ImageRequest.Builder(context).data(data).size(widthPx, heightPx).precision(Precision.EXACT).transformations(TrimTransparentEdges).build()
        }
    }
}

/**
 * The logo fitted in [box], or [name] in [textStyle] while the logo loads and when there is none
 * (tvOS TitleArtwork and TitleLogoImage).
 */
@Composable
fun TitleLogo(
    name: String,
    logo: ImageRequest?,
    box: DpSize,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    textMaxLines: Int = 2,
    textMaxWidth: Dp = box.width,
    alignment: Alignment = Alignment.BottomStart
) {
    var showsLogo by remember(logo) { mutableStateOf(false) }
    Box(modifier = modifier.semantics { contentDescription = name }, contentAlignment = alignment) {
        if (!showsLogo) {
            Text(
                text = name,
                style = textStyle,
                color = TvColors.textPrimary,
                maxLines = textMaxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = textMaxWidth)
            )
        }
        if (logo != null) {
            AsyncImage(
                model = logo,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alignment = alignment,
                modifier = Modifier.sizeIn(maxWidth = box.width, maxHeight = box.height),
                onSuccess = { showsLogo = true },
                onError = { showsLogo = false }
            )
        }
    }
}

private const val LOGO_DECODE_FACTOR = 2
