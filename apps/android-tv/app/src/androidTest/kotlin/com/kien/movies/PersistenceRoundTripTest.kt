package com.kien.movies

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nuvio.tvos.TvosAddonInstallResult
import com.nuvio.tvos.TvosAddons
import com.nuvio.tvos.TvosPlayback
import com.nuvio.tvos.TvosStream
import com.nuvio.tvos.TvosStreamTarget
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves the bridge storages persist across a process restart: run one installs the test addon
 * and saves watch progress, `adb shell am force-stop com.kien.movies.next` ends the process,
 * run two reads both back. Two separate `am instrument` runs, never connectedAndroidTest (it
 * uninstalls the app afterwards and wipes the data); see `make box-roundtrip`.
 *
 * Skipped unless the runner gets `-e persistenceRoundTrip true`. `-e addonUrl <manifest>`
 * overrides the test addon served by apps/apple-tv/TestAddon/serve_test_addon.py.
 */
@RunWith(AndroidJUnit4::class)
class PersistenceRoundTripTest {
    private val arguments = InstrumentationRegistry.getArguments()
    private val app = InstrumentationRegistry.getInstrumentation()
        .targetContext.applicationContext as MoviesApp
    private val addonUrl = arguments.getString("addonUrl") ?: DEFAULT_ADDON_URL

    @Before
    fun runtimeReady() {
        assumeTrue(arguments.getString("persistenceRoundTrip") == "true")
        awaitTrue("TvRuntime ready") { app.runtimeState.value == RuntimeState.Ready }
    }

    @Test
    fun run1_installsTestAddonAndSavesProgress() {
        if (addonUrl !in installedAddonUrls()) {
            assertTrue("install failed: ${install(addonUrl)}", addonUrl in installedAddonUrls())
        }
        val saved = TvosPlayback.startSession(TARGET, STREAM).save(
            positionMs = POSITION_MS,
            durationMs = DURATION_MS,
            isPlaying = false,
            isEnded = false,
            syncRemote = false
        )
        assertTrue("progress save was rejected", saved)

        // The storages write with SharedPreferences.apply(); wait until both writes are on
        // disk, or a force-stop right after this run could drop them.
        awaitTrue("addon URL on disk") { preferencesFile("nuvio_addons").contains(addonUrl) }
        awaitTrue("progress on disk") {
            preferencesFile("nuvio_watch_progress").contains(TARGET.videoId)
        }
    }

    @Test
    fun run2_readsAddonAndProgressBack() {
        awaitTrue("test addon restored") { addonUrl in installedAddonUrls() }
        awaitTrue("progress restored") {
            TvosPlayback.resumePositionMs(TARGET, DURATION_MS) > 0L
        }
        assertEquals(POSITION_MS, TvosPlayback.resumePositionMs(TARGET, DURATION_MS))
    }

    private fun installedAddonUrls(): List<String> = onMain { TvosAddons.current().map { addon -> addon.manifestUrl } }

    private fun install(url: String): TvosAddonInstallResult {
        val result = AtomicReference<TvosAddonInstallResult>()
        val done = CountDownLatch(1)
        onMain {
            TvosAddons.install(url) { outcome ->
                result.set(outcome)
                done.countDown()
            }
        }
        assertTrue("no install result for $url", done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
        return result.get()
    }

    private fun preferencesFile(name: String): String = File(app.dataDir, "shared_prefs/$name.xml").takeIf(File::exists)?.readText().orEmpty()

    private fun <T> onMain(block: () -> T): T {
        val result = AtomicReference<T>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync { result.set(block()) }
        return result.get()
    }

    private fun awaitTrue(what: String, condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS)
        while (!condition()) {
            check(System.nanoTime() < deadline) { "timed out waiting for $what" }
            Thread.sleep(POLL_MS)
        }
    }

    private companion object {
        const val DEFAULT_ADDON_URL = "http://127.0.0.1:8765/manifest.json"
        const val TIMEOUT_SECONDS = 30L
        const val POLL_MS = 200L
        const val POSITION_MS = 123_000L
        const val DURATION_MS = 600_000L

        val TARGET = TvosStreamTarget(
            type = "movie",
            videoId = "tvostest-roundtrip",
            parentMetaId = "tvostest-roundtrip",
            seasonNumber = null,
            episodeNumber = null,
            title = "Round trip test",
            subtitle = null,
            backdropUrl = null,
            posterUrl = null,
            logoUrl = null,
            episodeTitle = null,
            episodeThumbnailUrl = null
        )

        val STREAM = TvosStream(
            key = "roundtrip",
            name = "Round trip stream",
            title = null,
            detail = null,
            badges = emptyList(),
            sizeLabel = null,
            kind = "url",
            url = "http://127.0.0.1:8765/media/pattern.mp4",
            externalUrl = null,
            infoHash = null,
            fileIndex = null,
            filename = null,
            trackers = emptyList(),
            addonId = "dev.nuvio.tvos.testaddon",
            addonName = "Nuvio tvOS test films",
            playableUrl = "http://127.0.0.1:8765/media/pattern.mp4",
            requestHeaders = emptyMap()
        )
    }
}
