package com.nuvio.tvos

import com.nuvio.app.features.player.AvailableLanguageOptions
import com.nuvio.app.features.player.PlayerSettingsRepository
import com.nuvio.app.features.player.PlayerSettingsStorage
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** What the Settings pages show: the chosen audio and subtitle language codes. */
data class TvosLanguageChoice(
    val audio: String,
    val subtitles: String,
)

/**
 * Playback and subtitle languages for both TV apps, kept in core's player settings. Codes are
 * [TVOS_LANGUAGE_DEVICE], [TVOS_LANGUAGE_OFF] (subtitles only) or a language code; each platform
 * names them with its own locale API.
 */
object TvosSettings {
    private val mainScope = MainScope()

    val audioChoices: List<String> = listOf(TVOS_LANGUAGE_DEVICE) + languageCodes()
    val subtitleChoices: List<String> = listOf(TVOS_LANGUAGE_OFF, TVOS_LANGUAGE_DEVICE) + languageCodes()

    fun current(): TvosLanguageChoice {
        load()
        return PlayerSettingsRepository.uiState.value.let { state ->
            TvosLanguageChoice(state.preferredAudioLanguage, state.preferredSubtitleLanguage)
        }
    }

    fun observe(onChange: (TvosLanguageChoice) -> Unit): TvosCancellable {
        load()
        val job = mainScope.launch {
            PlayerSettingsRepository.uiState
                .map { state ->
                    TvosLanguageChoice(state.preferredAudioLanguage, state.preferredSubtitleLanguage)
                }
                .distinctUntilChanged()
                .collect(onChange)
        }
        return TvosCancellable(job)
    }

    fun setAudio(code: String) {
        load()
        PlayerSettingsRepository.setPreferredAudioLanguage(code)
    }

    fun setSubtitles(code: String) {
        load()
        PlayerSettingsRepository.setPreferredSubtitleLanguage(code)
    }

    /** The languages a player tries, in order, with "Automatic" as [deviceLanguage]. */
    fun playerLanguages(deviceLanguage: String): TvosPlayerLanguages {
        load()
        val state = PlayerSettingsRepository.uiState.value
        return playerLanguages(
            audio = listOf(state.preferredAudioLanguage, state.secondaryPreferredAudioLanguage),
            subtitles = listOf(state.preferredSubtitleLanguage, state.secondaryPreferredSubtitleLanguage),
            deviceLanguage = deviceLanguage,
        )
    }

    private fun load() {
        PlayerSettingsRepository.ensureLoaded()
        if (PlayerSettingsStorage.loadPreferredSubtitleLanguage() == null) seedTvSubtitles()
    }

    // Before Settings the TV apps always showed Vietnamese, then English subtitles. A box or
    // Apple TV that never chose keeps that.
    private fun seedTvSubtitles() {
        PlayerSettingsRepository.setPreferredSubtitleLanguage(TV_DEFAULT_SUBTITLES)
        PlayerSettingsRepository.setSecondaryPreferredSubtitleLanguage(TV_FALLBACK_SUBTITLES)
    }

    private fun languageCodes(): List<String> = AvailableLanguageOptions.map { option -> option.code }
}

private const val TV_DEFAULT_SUBTITLES = "vi"
private const val TV_FALLBACK_SUBTITLES = "en"
