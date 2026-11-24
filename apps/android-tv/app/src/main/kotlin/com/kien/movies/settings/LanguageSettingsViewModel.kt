package com.kien.movies.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kien.movies.bridge.BridgeFlows
import com.nuvio.tvos.TVOS_LANGUAGE_DEVICE
import com.nuvio.tvos.TVOS_LANGUAGE_OFF
import com.nuvio.tvos.TvosLanguageChoice
import com.nuvio.tvos.TvosSettings
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Which language a page picks: the audio a film starts in, or its subtitles. */
enum class LanguageKind(val page: SettingsPage, val listTitle: String) {
    Audio(SettingsPage.Playback, "Audio Language"),
    Subtitles(SettingsPage.Subtitles, "Subtitle Language");

    /** The fixed choices first (Off, Automatic), then the languages by name. */
    fun choices(codes: List<String> = sharedCodes()): List<String> {
        val fixed = codes.takeWhile(LanguageName::isFixed)
        return fixed + codes.drop(fixed.size).sortedBy(LanguageName::of)
    }

    private fun sharedCodes(): List<String> = if (this == Audio) TvosSettings.audioChoices else TvosSettings.subtitleChoices
}

/** The chosen languages, kept in the shared settings (tvOS LanguageSettingsModel.swift). */
class LanguageSettingsViewModel(
    initial: TvosLanguageChoice = TvosSettings.current(),
    choices: Flow<TvosLanguageChoice> = BridgeFlows.languageChoice()
) : ViewModel() {
    val choice: StateFlow<TvosLanguageChoice> = choices.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), initial)

    fun select(code: String, kind: LanguageKind) {
        when (kind) {
            LanguageKind.Audio -> TvosSettings.setAudio(code)
            LanguageKind.Subtitles -> TvosSettings.setSubtitles(code)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** The code a page shows as chosen. */
fun TvosLanguageChoice.selected(kind: LanguageKind): String = if (kind == LanguageKind.Audio) audio else subtitles

/** Names a language code in English, the app's language: Automatic and Off for the two fixed choices. */
object LanguageName {
    fun isFixed(code: String): Boolean = code == TVOS_LANGUAGE_DEVICE || code == TVOS_LANGUAGE_OFF

    fun of(code: String): String = when (code) {
        TVOS_LANGUAGE_DEVICE -> "Automatic"
        TVOS_LANGUAGE_OFF -> "Off"
        else -> Locale.forLanguageTag(code).getDisplayName(Locale.ENGLISH).takeIf(String::isNotBlank) ?: code
    }

    /** The device language for Automatic, as a base code ("en" for "en-GB"). */
    val device: String get() = Locale.getDefault().language.ifBlank { "en" }
}
