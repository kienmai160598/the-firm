package com.nuvio.torrent

import cnames.structs.movies_engine
import com.nuvio.torrent.native.movies_engine_destroy
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi

/** Owns one native engine handle; [close] releases it once. */
@OptIn(ExperimentalForeignApi::class)
internal class NativeTorrentEngine(
    private var handle: CPointer<movies_engine>?
) : TorrentEngine {
    override fun close() {
        movies_engine_destroy(handle)
        handle = null
    }
}
