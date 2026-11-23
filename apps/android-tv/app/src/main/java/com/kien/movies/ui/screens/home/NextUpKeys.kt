package com.kien.movies.ui.screens.home

import com.kien.movies.domain.model.WatchProgress

internal fun buildNextUpSeedCacheKey(progress: WatchProgress, showUnairedNextUp: Boolean): String {
    return buildString {
        append(progress.contentId.trim())
        append("|")
        append(progress.season ?: -1)
        append("|")
        append(progress.episode ?: -1)
        append("|unaired=")
        append(showUnairedNextUp)
        if (progress.excludedNextUpSeasons.isNotEmpty()) {
            append("|excluded=")
            append(progress.excludedNextUpSeasons.sorted().joinToString(","))
        }
    }
}

internal fun nextUpDismissKey(contentId: String, season: Int?, episode: Int?): String {
    return contentId.trim()
}
