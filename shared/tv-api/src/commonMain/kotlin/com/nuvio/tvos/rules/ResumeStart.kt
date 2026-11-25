package com.nuvio.tvos.rules

/**
 * Where a resumed playback starts when that is known before the player has loaded the stream.
 *
 * Saved progress that holds a position and the length it was measured against resumes at that
 * position whatever length the stream reports (WatchProgressEntry.resolveResumePosition only caps
 * it at the length). Progress with a percent only needs the length first. A player that is handed
 * the position with the stream opens it there, and does not load the start of the file only to
 * throw it away.
 */
object ResumeStart {
    /** The start position in ms, or null when it depends on the stream's length. */
    fun positionBeforeLoadMs(savedPositionMs: Long, savedDurationMs: Long): Long? =
        savedPositionMs.takeIf { savedPositionMs > 0 && savedDurationMs > 0 }
}
