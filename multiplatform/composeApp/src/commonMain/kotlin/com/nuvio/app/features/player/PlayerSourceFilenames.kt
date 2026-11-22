package com.nuvio.app.features.player

import com.nuvio.app.features.streams.StreamItem

internal fun streamPlaybackFilename(stream: StreamItem, url: String): String? =
    stream.behaviorHints.filename?.takeIf { it.isNotBlank() }
        ?: sourceFilenameFromUrl(url)

internal fun sourceFilenameFromUrl(url: String?): String? =
    url?.substringBefore('?')?.substringAfterLast('/')
        ?.takeIf { it.isNotBlank() && it.contains('.') }
