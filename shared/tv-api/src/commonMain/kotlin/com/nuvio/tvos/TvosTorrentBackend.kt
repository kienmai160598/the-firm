package com.nuvio.tvos

import kotlinx.coroutines.flow.Flow

// The torrent engine behind TvosTorrent, one per platform: tvOS picks it at build time
// (apple-tv-bridge src/tvosLegacyEngine or src/tvosNextEngine, docs/engine-rebuild.md D2),
// Android uses the old engine (android-tv-bridge androidMain).

internal expect suspend fun startTorrent(stream: TvosStream): TvosTorrentStart

internal expect fun torrentStatuses(): Flow<TvosTorrentStatus>

internal expect fun stopTorrent()
