package com.kien.movies.ui.screens.home

import com.kien.movies.core.poster.CustomPosterUrlResolver

fun ContinueWatchingItem.withCustomPosterUrl(
    pattern: String
): ContinueWatchingItem {
    if (pattern.isBlank()) return this
    val supportsShape = CustomPosterUrlResolver.supportsShape(pattern)
    return when (this) {
        is ContinueWatchingItem.InProgress -> {
            val ids = CustomPosterUrlResolver.extractIds(progress.contentId)
            val type = if (progress.contentType.equals(
                    "movie",
                    ignoreCase = true
                )
            ) {
                "movie"
            } else {
                "series"
            }
            val resolvedPoster = CustomPosterUrlResolver.resolve(pattern, ids, type)
            val resolvedLandscape = if (supportsShape) {
                CustomPosterUrlResolver.resolve(pattern, ids, type, shape = "landscape")
            } else {
                null
            }
            if (resolvedPoster == null && resolvedLandscape == null) return this
            copy(
                progress = progress.copy(poster = resolvedPoster ?: progress.poster),
                originalPoster = originalPoster ?: progress.poster,
                customLandscapePoster = resolvedLandscape
            )
        }

        is ContinueWatchingItem.NextUp -> {
            val ids = CustomPosterUrlResolver.extractIds(info.contentId)
            val type = if (info.contentType.equals(
                    "movie",
                    ignoreCase = true
                )
            ) {
                "movie"
            } else {
                "series"
            }
            val resolvedPoster = CustomPosterUrlResolver.resolve(pattern, ids, type)
            val resolvedLandscape = if (supportsShape) {
                CustomPosterUrlResolver.resolve(pattern, ids, type, shape = "landscape")
            } else {
                null
            }
            if (resolvedPoster == null && resolvedLandscape == null) return this
            copy(
                info = info.copy(poster = resolvedPoster ?: info.poster),
                originalPoster = originalPoster ?: info.poster,
                customLandscapePoster = resolvedLandscape
            )
        }
    }
}

@JvmName("withCustomPosterUrlsCw")
fun List<ContinueWatchingItem>.withCustomPosterUrls(
    pattern: String
): List<ContinueWatchingItem> {
    if (pattern.isBlank()) return this
    return map { it.withCustomPosterUrl(pattern) }
}
