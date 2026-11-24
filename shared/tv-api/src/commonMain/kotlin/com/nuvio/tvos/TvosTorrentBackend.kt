package com.nuvio.tvos

import kotlinx.coroutines.flow.Flow

// The torrent engine behind TvosTorrent, one per platform: tvOS runs the new engine in engine/
// (apple-tv-bridge tvosMain, docs/engine-rebuild.md E7a), Android the old one until E6
// (android-tv-bridge androidMain).

internal expect suspend fun startTorrent(stream: TvosStream): TvosTorrentStart

internal expect fun torrentStatuses(): Flow<TvosTorrentStatus>

internal expect fun stopTorrent()
