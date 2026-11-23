package com.kien.movies.ui.screens.player

import android.os.SystemClock

private const val LOADING_EVENT_LIMIT = 80
private const val LOADING_RAW_EVENT_LIMIT = 120

internal data class PlayerLoadingDiagnosticEvent(
    val timeMs: Long,
    val elapsedMs: Long,
    val phase: String,
    val message: String?,
    val progress: Float?,
    val detail: String?
)

internal class PlayerLoadingDiagnosticsLog(
    private val elapsedRealtimeMs: () -> Long = SystemClock::elapsedRealtime,
    private val wallClockMs: () -> Long = System::currentTimeMillis
) {
    var phase: String = "idle"
        private set
    var lastMessage: String? = null
        private set
    var lastProgress: Float? = null
        private set
    private var startedAtMs: Long = 0L
    private var phaseStartedAtMs: Long = 0L
    private var lastSignature: String = ""
    private var sequence: Int = 0
    private val events: ArrayDeque<PlayerLoadingDiagnosticEvent> = ArrayDeque()
    private val rawLines: ArrayDeque<String> = ArrayDeque()

    val recordedEvents: List<PlayerLoadingDiagnosticEvent>
        get() = events.toList()
    val recordedRawLines: List<String>
        get() = rawLines.toList()

    fun reset(phase: String, message: String?, progress: Float?) {
        val now = elapsedRealtimeMs()
        startedAtMs = now
        this.phase = phase
        phaseStartedAtMs = now
        lastMessage = message
        lastProgress = progress
        lastSignature = ""
        sequence = 0
        events.clear()
        rawLines.clear()
    }

    fun record(
        phase: String,
        message: String?,
        progress: Float?,
        detail: String?,
        launchStartedAtMs: Long?,
        describePlayback: () -> String
    ) {
        val now = elapsedRealtimeMs()
        val timeMs = wallClockMs()
        if (startedAtMs == 0L) {
            startedAtMs = now
            phaseStartedAtMs = now
        }
        if (phase != this.phase) {
            this.phase = phase
            phaseStartedAtMs = now
        }
        lastMessage = message
        lastProgress = progress
        val normalizedProgress = progress?.let { "%.3f".format(it.coerceIn(0f, 1f)) }.orEmpty()
        val signature = "$phase|${message.orEmpty()}|$normalizedProgress|${detail.orEmpty()}"
        if (signature == lastSignature) return
        lastSignature = signature
        sequence += 1
        val elapsedMs = (now - startedAtMs).coerceAtLeast(0L)
        val phaseElapsedMs = (now - phaseStartedAtMs).coerceAtLeast(0L)
        val clickElapsedMs = launchStartedAtMs?.let { (now - it).coerceAtLeast(0L) }
        addRawLine(
            "STARTUP_STAGE: seq=$sequence phase=$phase elapsedMs=$elapsedMs " +
                "phaseElapsedMs=$phaseElapsedMs clickElapsedMs=${clickElapsedMs ?: -1L} " +
                "${describePlayback()} " +
                "message=${message?.compactTraceValue() ?: "n/a"} progress=${progress ?: -1f} " +
                "detail=${detail?.compactTraceValue() ?: "n/a"}"
        )
        events.addLast(
            PlayerLoadingDiagnosticEvent(
                timeMs = timeMs,
                elapsedMs = elapsedMs,
                phase = phase,
                message = message,
                progress = progress,
                detail = detail
            )
        )
        while (events.size > LOADING_EVENT_LIMIT) {
            events.removeFirst()
        }
    }

    fun elapsedSinceStartMs(): Long = (elapsedRealtimeMs() - startedAtMs).coerceAtLeast(0L)

    fun elapsedForReportMs(): Long = elapsedSinceMarkMs(startedAtMs)

    fun phaseElapsedForReportMs(): Long = elapsedSinceMarkMs(phaseStartedAtMs)

    private fun elapsedSinceMarkMs(markMs: Long): Long {
        val elapsedMs = if (markMs > 0L) elapsedRealtimeMs() - markMs else 0L
        return elapsedMs.coerceAtLeast(0L)
    }

    private fun addRawLine(line: String) {
        rawLines.addLast(line.rawLoadingEventLine())
        while (rawLines.size > LOADING_RAW_EVENT_LIMIT) {
            rawLines.removeFirst()
        }
    }
}

private fun String.compactTraceValue(): String = replace('\n', ' ')
    .replace('\r', ' ')
    .take(160)

private fun String.rawLoadingEventLine(): String = replace('\n', ' ')
    .replace('\r', ' ')
    .take(2000)
