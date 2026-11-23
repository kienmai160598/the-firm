package com.kien.movies.torrent

import kotlinx.coroutines.CoroutineScope

/**
 * One torrent start for the Streams screen: step, peers, speed and buffer while it starts, the
 * local URL when it can play, and the engine stop on exit (tvOS TorrentSession.swift). Not a
 * screen. Stub until slice A10; StreamsViewModel creates it with its viewModelScope.
 */
class TorrentSession(val scope: CoroutineScope)
