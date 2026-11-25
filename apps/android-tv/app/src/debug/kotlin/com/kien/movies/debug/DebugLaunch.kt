package com.kien.movies.debug

import android.os.Bundle
import com.kien.movies.settings.SettingsPage
import com.kien.movies.shell.AppRoute
import com.kien.movies.shell.RootTab
import com.nuvio.tvos.TvosDebugAddon
import com.nuvio.tvos.TvosMetaDetails

/**
 * Launch options for UI tests and manual checks, debug builds only (tvOS DebugLaunch.swift; the
 * `box` build has a twin in src/box that answers "off" to everything). tvOS reads launch
 * arguments; Android reads the same names as intent extras on MainActivity:
 *
 * ```
 * adb shell am start -n com.kien.movies.next/com.kien.movies.MainActivity \
 *   -e MoviesDebugRoute streams/movie/tt0032138 --ez MoviesDebugOpenFirstStream true
 * ```
 *
 * | Extra | Type | Effect |
 * |---|---|---|
 * | MoviesDebugRoute | string | `details/<type>/<id>`, `streams/<type>/<id>` (Details, which then opens Streams once), `tab/<home,search,library,settings>` or `settings/<playback,subtitles,addons,about>` (Android only) |
 * | MoviesDebugAddonUrl | string | installs this manifest after the default addon and removes other loopback addons |
 * | MoviesDebugStreamFixture | boolean | Streams shows the fixture instead of loading |
 * | MoviesDebugOpenFirstStream | boolean | Streams plays its first stream once |
 * | MoviesDebugStartSecondsBeforeEnd | int | the player starts this many seconds before the end |
 * | MoviesDebugResumeChoice | string | `resume` or `startOver` answers the resume dialog |
 * | MoviesDebugSeason | int | the season picker starts on this season |
 *
 * Not mirrored: `MoviesDebugFreshState`. The storages are read in `Application.onCreate`, before
 * any Activity sees an intent, so a fresh state is `adb shell pm clear com.kien.movies.next` run
 * from the host before `am instrument`.
 */
object DebugLaunch {
    private var extras: Bundle = Bundle.EMPTY
    private var hasOpenedStreams = false
    private var hasOpenedFirstStream = false

    /** Called by MainActivity on its first creation. */
    fun read(intentExtras: Bundle?) {
        extras = intentExtras ?: Bundle.EMPTY
        hasOpenedStreams = false
        hasOpenedFirstStream = false
    }

    /** Called once the bridge runtime is ready; repeats `TvosDebugAddon.sync` of NuvioTvApp.swift. */
    fun onRuntimeReady() {
        TvosDebugAddon.sync(manifestUrl = addonUrl)
    }

    val initialRoute: AppRoute?
        get() {
            val parts = extras.getString(ROUTE)?.split("/", limit = 3) ?: return null
            return when {
                parts.size == 2 && parts[0] == "tab" ->
                    RootTab.entries.firstOrNull { tab -> tab.name.equals(parts[1], ignoreCase = true) }?.let(AppRoute::Tab)

                parts.size == 2 && parts[0] == "settings" ->
                    SettingsPage.entries.firstOrNull { page -> page.name.equals(parts[1], ignoreCase = true) }?.let(AppRoute::Settings)

                parts.size == 3 && (parts[0] == "details" || parts[0] == "streams") -> AppRoute.Details(type = parts[1], id = parts[2])

                else -> null
            }
        }

    val usesStreamFixture: Boolean get() = extras.getBoolean(STREAM_FIXTURE)

    val opensFirstStream: Boolean get() = !hasOpenedFirstStream && extras.getBoolean(OPEN_FIRST_STREAM)

    fun markFirstStreamOpened() {
        hasOpenedFirstStream = true
    }

    val addonUrl: String? get() = extras.getString(ADDON_URL)

    val startSecondsBeforeEnd: Long? get() = extras.getInt(START_SECONDS_BEFORE_END).takeIf { seconds -> seconds > 0 }?.toLong()

    val resumeChoice: Boolean?
        get() = when (extras.getString(RESUME_CHOICE)) {
            "resume" -> true
            "startOver" -> false
            else -> null
        }

    val seasonNumber: Int? get() = extras.getInt(SEASON).takeIf { season -> season > 0 }

    /** The Streams route of [details]' play action, once, when launched with `streams/...`. */
    fun streamsRouteOnce(details: TvosMetaDetails): AppRoute? {
        val action = details.primaryAction
        if (hasOpenedStreams || extras.getString(ROUTE)?.startsWith("streams/") != true || action == null) return null
        hasOpenedStreams = true
        return AppRoute.Streams(action.target)
    }

    const val ROUTE = "MoviesDebugRoute"
    const val ADDON_URL = "MoviesDebugAddonUrl"
    const val STREAM_FIXTURE = "MoviesDebugStreamFixture"
    const val OPEN_FIRST_STREAM = "MoviesDebugOpenFirstStream"
    const val START_SECONDS_BEFORE_END = "MoviesDebugStartSecondsBeforeEnd"
    const val RESUME_CHOICE = "MoviesDebugResumeChoice"
    const val SEASON = "MoviesDebugSeason"
}
