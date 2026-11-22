package com.nuvio.tvos

data class TvosStream(
    val key: String,
    val name: String,
    val title: String?,
    val detail: String?,
    val badges: List<String>,
    val sizeLabel: String?,
    val kind: String,
    val url: String?,
    val externalUrl: String?,
    val infoHash: String?,
    val fileIndex: String?,
    val filename: String?,
    val addonId: String,
    val addonName: String,
    val playableUrl: String?,
    val requestHeaders: Map<String, String>,
)

data class TvosStreamGroup(
    val addonId: String,
    val addonName: String,
    val streams: List<TvosStream>,
    val isLoading: Boolean,
    val errorMessage: String?,
)

data class TvosStreamsEmptyState(
    val title: String,
    val message: String,
    val isMissingStreamAddon: Boolean,
)

data class TvosStreamsState(
    val groups: List<TvosStreamGroup>,
    val isLoading: Boolean,
    val emptyState: TvosStreamsEmptyState?,
)
