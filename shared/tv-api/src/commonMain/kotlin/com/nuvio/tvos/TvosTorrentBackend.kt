package com.nuvio.tvos

import kotlinx.coroutines.flow.Flow

// The torrent engine behind TvosTorrent: both apps run the new engine in engine/ through
// EngineTorrentBackend (docs/engine-rebuild.md, E7a for tvOS, E7 for Android). Each platform's
// TvosTorrentBackend (apple-tv-bridge tvosMain, android-tv-bridge androidMain) opens it with its
// own state directory and memory budget.

internal expect suspend fun startTorrent(stream: TvosStream): TvosTorrentStart

internal expect fun torrentStatuses(): Flow<TvosTorrentStatus>

internal expect fun stopTorrent()
