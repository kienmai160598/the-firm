package com.kien.movies.domain.model

internal data class RememberedTrackSelection(
    val language: String?,
    val name: String?,
    val trackId: String? = null,
    val indexHint: Int? = null,
    val languageIndexHint: Int? = null,
    val isForcedHint: Boolean? = null
)

internal sealed class RememberedSubtitleSelection {
    data object Disabled : RememberedSubtitleSelection()
    data class Internal(val track: RememberedTrackSelection) : RememberedSubtitleSelection()
    data class Addon(
        val id: String,
        val url: String,
        val language: String,
        val addonName: String
    ) : RememberedSubtitleSelection()
}

internal data class TrackPreference(
    val audio: RememberedTrackSelection? = null,
    val subtitle: RememberedSubtitleSelection? = null
)
