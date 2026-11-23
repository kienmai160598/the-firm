package com.kien.movies.core.plugin.cloudstream

import android.util.Log
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.extractorApis
import com.lagradost.cloudstream3.utils.loadExtractor
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ExtExtractorRegistry"

/**
 * Registry of loaded extractors from external extensions.
 * Bridges Movies's extractor management with the CloudStream library's
 * global [extractorApis] list and [loadExtractor] function.
 */
@Singleton
class ExternalExtractorRegistry @Inject constructor() {

    private val missingExtractorDomains = mutableSetOf<String>()
    private var installed = false

    fun registerExtractor(extractor: ExtractorApi) {
        // Avoid duplicates by mainUrl
        if (extractorApis.any { it.mainUrl == extractor.mainUrl }) return
        extractorApis.add(extractor)
        Log.d(TAG, "Registered extractor: ${extractor.name} (${extractor.mainUrl})")
    }

    fun registerAll(extractorList: List<ExtractorApi>) {
        extractorList.forEach { registerExtractor(it) }
    }

    fun clear() {
        missingExtractorDomains.clear()
    }

    /**
     * Install this registry. The library's loadExtractor function uses the global
     * extractorApis list directly, so no delegate setup is needed.
     * This method ensures the library's built-in extractors are available.
     */
    fun installGlobal() {
        if (installed) return
        installed = true
        Log.d(
            TAG,
            "installGlobal: library extractorApis has ${extractorApis.size} built-in extractors"
        )
    }

    fun getMissingExtractorDomains(): Set<String> = missingExtractorDomains.toSet()
}
