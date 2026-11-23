package com.kien.movies.domain.model

import androidx.compose.runtime.Immutable
import com.kien.movies.core.util.languageCodeToName

@Immutable
data class Subtitle(
    val id: String,
    val url: String,
    val lang: String,
    val addonName: String,
    val addonLogo: String?,
    val isStreamProvided: Boolean = false,
    val headers: Map<String, String>? = null,
    val label: String? = null
) {
    fun getDisplayLanguage(): String = languageCodeToName(lang)

    companion object {
        fun languageCodeToName(code: String): String = com.kien.movies.core.util.languageCodeToName(code)
    }
}
