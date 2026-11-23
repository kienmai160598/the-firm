package com.kien.movies.debug

import android.os.Bundle
import com.kien.movies.shell.AppRoute
import com.nuvio.tvos.TvosMetaDetails

/**
 * The `box` build's twin of the debug DebugLaunch (src/debug): the same API, every option off,
 * intent extras ignored. Keep the two in step: `make check` compiles the debug one, `make
 * android-tv` this one.
 */
object DebugLaunch {
    @Suppress("UNUSED_PARAMETER")
    fun read(intentExtras: Bundle?) = Unit

    fun onRuntimeReady() = Unit

    val initialRoute: AppRoute? get() = null

    val usesStreamFixture: Boolean get() = false

    val opensFirstStream: Boolean get() = false

    fun markFirstStreamOpened() = Unit

    val addonUrl: String? get() = null

    val startSecondsBeforeEnd: Long? get() = null

    val resumeChoice: Boolean? get() = null

    val seasonNumber: Int? get() = null

    @Suppress("UNUSED_PARAMETER")
    fun streamsRouteOnce(details: TvosMetaDetails): AppRoute? = null
}
