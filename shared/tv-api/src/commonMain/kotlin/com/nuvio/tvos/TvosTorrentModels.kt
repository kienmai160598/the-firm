package com.nuvio.tvos

data class TvosTorrentStart(
    val localUrl: String?,
    val errorMessage: String?,
)

data class TvosTorrentStatus(
    val phase: String,
    val step: String?,
    val localUrl: String?,
    val peers: Int,
    val seeds: Int,
    val downloadSpeedLabel: String,
    val bufferFraction: Float,
    val downloadedFraction: Float,
    val errorMessage: String?,
)
