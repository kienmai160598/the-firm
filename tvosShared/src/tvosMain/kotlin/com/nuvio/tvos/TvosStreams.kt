package com.nuvio.tvos

import co.touchlab.kermit.Logger
import com.nuvio.app.features.streams.StreamsRepository
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

object TvosStreams {
    private val log = Logger.withTag("TvosStreams")
    private val mainScope = MainScope()

    fun observeStreams(
        target: TvosStreamTarget,
        onChange: (TvosStreamsState) -> Unit,
    ): TvosCancellable {
        val job = mainScope.launch {
            load(target, forceRefresh = false)
            StreamsRepository.uiState
                .filter { state -> state.requestToken == target.requestToken() }
                .onEach { state -> log.i { "Streams ${target.videoId}: ${state.summary()}" } }
                .map { state -> tvosStreamsState(state) }
                .distinctUntilChanged()
                .collect(onChange)
        }
        return TvosCancellable(job)
    }

    fun reload(target: TvosStreamTarget) {
        load(target, forceRefresh = true)
    }

    private fun load(target: TvosStreamTarget, forceRefresh: Boolean) {
        if (forceRefresh) {
            StreamsRepository.reload(
                type = target.type,
                videoId = target.videoId,
                parentMetaId = target.parentMetaId,
                season = target.seasonNumber,
                episode = target.episodeNumber,
                manualSelection = true,
            )
        } else {
            StreamsRepository.load(
                type = target.type,
                videoId = target.videoId,
                parentMetaId = target.parentMetaId,
                season = target.seasonNumber,
                episode = target.episodeNumber,
                manualSelection = true,
            )
        }
    }

    private fun TvosStreamTarget.requestToken(): String =
        StreamsRepository.requestToken(
            type = type,
            videoId = videoId,
            season = seasonNumber,
            episode = episodeNumber,
            manualSelection = true,
        )
}
