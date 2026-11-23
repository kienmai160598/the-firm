package com.kien.movies.core.debrid

import com.kien.movies.data.remote.dto.RealDebridTorrentFileDto
import com.kien.movies.domain.model.StreamClientResolve
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealDebridFileSelector @Inject constructor() {
    fun selectFile(
        files: List<RealDebridTorrentFileDto>,
        resolve: StreamClientResolve,
        season: Int?,
        episode: Int?
    ): RealDebridTorrentFileDto? = selectDebridFile(
        files = files,
        resolve = resolve,
        season = season,
        episode = episode,
        path = { it.path.orEmpty() },
        isPlayable = { it.displayName().hasDebridVideoExtension() },
        size = { it.bytes ?: 0L }
    )
}
