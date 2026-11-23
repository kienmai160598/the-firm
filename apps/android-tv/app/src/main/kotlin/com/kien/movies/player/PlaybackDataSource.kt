package com.kien.movies.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.TransferListener

/**
 * Where the player's bytes come from. Replaces core's PlatformPlaybackDataSourceFactory and
 * PlayerSubtitleRouting, which the Android bridge excludes (they need the phone player engine):
 * - the stream is fetched with its addon's request headers and a browser user agent;
 * - online subtitles are marked by [SubtitleUriMarker] and fetched without those headers, so a
 *   stream's `Authorization` never reaches a subtitle host;
 * - a loopback URL (a started torrent) gets a long read timeout: the engine answers once the
 *   pieces arrive.
 * [transferListener] counts the bytes for [StartupDeadline] and [StallWatchdog].
 */
@OptIn(UnstableApi::class)
internal object PlaybackDataSource {
    /** Same user agent as core's PlayerPlaybackNetworking: some hosts refuse ExoPlayer's own. */
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 15_000
    private const val LOOPBACK_READ_TIMEOUT_MS = 65_000
    private val loopbackHosts = setOf("127.0.0.1", "localhost", "::1", "[::1]")

    fun factory(context: Context, url: String, headers: Map<String, String>, transferListener: TransferListener): DataSource.Factory {
        val readTimeout = if (Uri.parse(url).host in loopbackHosts) LOOPBACK_READ_TIMEOUT_MS else READ_TIMEOUT_MS
        val media = http(readTimeout).setDefaultRequestProperties(headers)
        val subtitles = http(READ_TIMEOUT_MS)
        return DefaultDataSource.Factory(context, SubtitleRoutingFactory(media, subtitles)).setTransferListener(transferListener)
    }

    private fun http(readTimeoutMs: Int): DefaultHttpDataSource.Factory = DefaultHttpDataSource.Factory()
        .setUserAgent(USER_AGENT)
        .setConnectTimeoutMs(CONNECT_TIMEOUT_MS)
        .setReadTimeoutMs(readTimeoutMs)
        .setAllowCrossProtocolRedirects(true)
}

/** Marks an online subtitle URL so [SubtitleRoutingFactory] fetches it without the stream's headers. */
internal object SubtitleUriMarker {
    private const val MARKER = "nuvio_type=subtitle"

    /** [url] with the marker added to its query; non HTTP URLs stay as they are. */
    fun mark(url: String): String {
        val scheme = url.substringBefore("://", missingDelimiterValue = "").lowercase()
        if (scheme != "http" && scheme != "https") return url
        val fragmentStart = url.indexOf('#').takeIf { it >= 0 } ?: url.length
        val base = url.substring(0, fragmentStart)
        val separator = if ('?' in base) "&" else "?"
        return base + separator + MARKER + url.substring(fragmentStart)
    }

    /** The original URL of a marked [url], or null when it is not marked. */
    fun unmark(url: String): String? {
        val fragmentStart = url.indexOf('#').takeIf { it >= 0 } ?: url.length
        val base = url.substring(0, fragmentStart)
        val queryStart = base.indexOf('?').takeIf { it >= 0 } ?: return null
        val parts = base.substring(queryStart + 1).split('&')
        if (MARKER !in parts) return null
        val query = parts.filter { it != MARKER }.joinToString("&")
        val path = base.substring(0, queryStart)
        return (if (query.isEmpty()) path else "$path?$query") + url.substring(fragmentStart)
    }
}

@OptIn(UnstableApi::class)
private class SubtitleRoutingFactory(private val media: DataSource.Factory, private val subtitles: DataSource.Factory) : DataSource.Factory {
    override fun createDataSource(): DataSource = SubtitleRoutingDataSource(media, subtitles)
}

@OptIn(UnstableApi::class)
private class SubtitleRoutingDataSource(private val media: DataSource.Factory, private val subtitles: DataSource.Factory) : DataSource {
    private val transferListeners = mutableListOf<TransferListener>()
    private var active: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) {
        transferListeners += transferListener
        active?.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val subtitleUrl = SubtitleUriMarker.unmark(dataSpec.uri.toString())
        val source = (if (subtitleUrl != null) subtitles else media).createDataSource()
        transferListeners.forEach(source::addTransferListener)
        active = source
        return source.open(if (subtitleUrl != null) dataSpec.withUri(Uri.parse(subtitleUrl)) else dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = checkNotNull(active) { "read before open" }.read(buffer, offset, length)

    override fun getUri(): Uri? = active?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = active?.responseHeaders ?: emptyMap()

    override fun close() {
        try {
            active?.close()
        } finally {
            active = null
        }
    }
}
