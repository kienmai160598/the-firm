package com.kien.movies.ui.util

import android.content.Context
import com.kien.movies.R

private val EPISODE_PATTERN = Regex("^Episode (\\d+)$", RegexOption.IGNORE_CASE)

fun String.localizeEpisodeTitle(appContext: Context): String {
    val match = EPISODE_PATTERN.matchEntire(this.trim()) ?: return this
    val number = match.groupValues[1]
    return "${appContext.getString(R.string.episodes_episode)} $number"
}
