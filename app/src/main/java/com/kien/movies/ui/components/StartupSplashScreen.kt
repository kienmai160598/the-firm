package com.kien.movies.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun StartupSplashScreen(
    profileColorHex: String?,
    profileBackgroundUrl: String?,
    backgroundCacheKey: String? = null,
    skipGradient: Boolean = false,
    brandWordmarkRes: Int? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (!profileBackgroundUrl.isNullOrBlank()) {
            val imageData: Any = if (profileBackgroundUrl.startsWith("file:")) {
                java.io.File(java.net.URI(profileBackgroundUrl))
            } else {
                profileBackgroundUrl
            }
            val request = ImageRequest.Builder(LocalContext.current)
                .data(imageData)
                .crossfade(false)
            if (backgroundCacheKey != null) {
                request.memoryCacheKey(backgroundCacheKey)
                    .diskCacheKey(backgroundCacheKey)
                    .placeholderMemoryCacheKey(backgroundCacheKey)
            }
            AsyncImage(
                model = request.build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            MoviesSplashBackground()
        }

        MoviesSplashContent()
    }
}
