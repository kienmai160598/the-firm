package com.nuvio.tvos.rules

/**
 * Decides the online subtitle to load as a film starts ([OnlineSubtitleChoice.startingChoice])
 * once both the file's own subtitle languages and the online list are known, whichever comes
 * last. It answers once per playback, and not at all once the viewer has picked a subtitle. One
 * per playback, on the player's thread (tvOS MpvPlaybackController, Android PlayerController).
 */
class StartingSubtitle(private val preferred: List<String>) {
    private var online: List<OnlineSubtitle>? = null
    private var embeddedLanguages: List<String>? = null
    private var isSettled = false

    /** The subtitle to load now, if this list was the last thing missing. */
    fun online(subtitles: List<OnlineSubtitle>): OnlineSubtitle? {
        online = subtitles
        return decide()
    }

    /** The subtitle to load now, if the file's subtitle languages were the last thing missing. */
    fun tracks(embeddedLanguages: List<String>): OnlineSubtitle? {
        this.embeddedLanguages = embeddedLanguages
        return decide()
    }

    fun viewerChose() {
        isSettled = true
    }

    private fun decide(): OnlineSubtitle? {
        val subtitles = online ?: return null
        val languages = embeddedLanguages ?: return null
        if (isSettled) return null
        isSettled = true
        return OnlineSubtitleChoice.startingChoice(subtitles, preferred, languages)
    }
}
