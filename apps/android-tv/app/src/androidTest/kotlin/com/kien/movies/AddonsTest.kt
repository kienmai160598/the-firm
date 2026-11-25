package com.kien.movies

import android.view.KeyEvent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kien.movies.addons.ADDON_ROW_TAG
import com.kien.movies.addons.EMPTY_TITLE
import com.kien.movies.addons.ENABLED_TAG
import com.kien.movies.addons.INSTALL_FAILED
import com.kien.movies.addons.MOVE_UP_TAG
import com.kien.movies.addons.REMOVE_TAG
import com.kien.movies.addons.URL_FIELD_TAG
import com.kien.movies.addons.URL_SURFACE_TAG
import com.kien.movies.addons.dialogButtonTag
import com.kien.movies.debug.DebugLaunch
import com.kien.movies.support.TvRobot
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Addons ACs A1 to A4 (docs/android-tv-rebuild.md, section 6), from apps/apple-tv/UITests/AddonsTests.swift.
 * D-pad only. A2 and A3 need the test addon on 127.0.0.1:8765 (adb reverse) and are skipped
 * without it. A4 removes every addon: run it on a cleared app (`pm clear com.kien.movies.next`).
 */
@RunWith(AndroidJUnit4::class)
class AddonsTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val tv = TvRobot(compose)

    @After
    fun closeApp() = tv.close()

    /** A1, and tvOS `defaultFocus`: Add Addon has focus when Addons opens. */
    @Test
    fun a1_aManifestThatCannotBeReachedShowsWhyItFailed() {
        openAddons()
        tv.select()
        tv.awaitFocus(hasTestTag(URL_FIELD_TAG))

        compose.onNodeWithTag(URL_FIELD_TAG).performTextInput("http://127.0.0.1:9")
        compose.onNodeWithTag(URL_FIELD_TAG).performImeAction()

        tv.await(hasText(INSTALL_FAILED), INSTALL_TIMEOUT_MS)
        tv.assertFocusedTag(URL_SURFACE_TAG)
    }

    /** A2: Enabled turns the addon off in place, and Back lands on its row, which says Off. */
    @Test
    fun a2_anAddonPageTurnsItOffInPlaceAndTheListSaysOff() {
        openTestAddonPage()
        if (compose.onAllNodes(isFocused() and hasTestTag(ENABLED_TAG) and hasText("Off")).fetchSemanticsNodes().isNotEmpty()) tv.select()
        tv.awaitFocus(hasTestTag(ENABLED_TAG) and hasText("On"))

        tv.select()

        tv.awaitFocus(hasTestTag(ENABLED_TAG) and hasText("Off"))
        tv.back()
        tv.awaitFocus(testAddonRow and hasText("Off"))
        turnTestAddonBackOn() // for the other tests
    }

    @Test
    fun a3_moveUpPutsTheAddonFirst() {
        openTestAddonPage()
        tv.pressUntilFocused(KeyEvent.KEYCODE_DPAD_DOWN, hasTestTag(MOVE_UP_TAG))

        tv.select()
        tv.back()

        compose.waitUntil(TvRobot.FOCUS_TIMEOUT_MS) {
            val firstRow = compose.onAllNodes(hasTestTag(ADDON_ROW_TAG)).fetchSemanticsNodes().firstOrNull()
            firstRow != null && hasText(TEST_ADDON_NAME, substring = true).matches(firstRow)
        }
    }

    /** A4: Remove Addon asks first and returns to the list; at the end only Add Addon is left. */
    @Test
    fun a4_removingEveryAddonShowsTheEmptyStateAndKeepsFocus() {
        openAddons()
        repeat(MAX_ADDONS_TO_REMOVE) {
            if (compose.onAllNodes(hasTestTag(ADDON_ROW_TAG)).fetchSemanticsNodes().isNotEmpty()) removeAnAddon()
        }

        tv.await(hasText(EMPTY_TITLE), TvRobot.FOCUS_TIMEOUT_MS)

        tv.assertFocusedTag(URL_SURFACE_TAG)
    }

    private fun removeAnAddon() {
        tv.pressUntilFocused(KeyEvent.KEYCODE_DPAD_DOWN, hasTestTag(ADDON_ROW_TAG))
        tv.select()
        tv.awaitFocus(hasTestTag(ENABLED_TAG))
        tv.pressUntilFocused(KeyEvent.KEYCODE_DPAD_DOWN, hasTestTag(REMOVE_TAG))
        tv.select()
        tv.await(hasText("Remove ", substring = true) and hasText("?", substring = true))
        tv.awaitFocus(hasTestTag(dialogButtonTag("Cancel")))
        tv.press(KeyEvent.KEYCODE_DPAD_RIGHT)
        tv.awaitFocus(hasTestTag(dialogButtonTag("Remove")))
        tv.select()
        tv.awaitFocus(hasTestTag(ADDON_ROW_TAG) or hasTestTag(URL_SURFACE_TAG))
    }

    private fun openTestAddonPage() {
        openAddons(DebugLaunch.ADDON_URL to TEST_ADDON_URL)
        assumeTestAddon()
        tv.pressUntilFocused(KeyEvent.KEYCODE_DPAD_DOWN, testAddonRow)
        tv.select()
        tv.awaitFocus(hasTestTag(ENABLED_TAG))
    }

    private fun turnTestAddonBackOn() {
        tv.select()
        tv.awaitFocus(hasTestTag(ENABLED_TAG) and hasText("Off"))
        tv.select()
        tv.awaitFocus(hasTestTag(ENABLED_TAG) and hasText("On"))
        tv.back()
        tv.awaitFocus(testAddonRow and hasText("On"))
    }

    private val testAddonRow = hasTestTag(ADDON_ROW_TAG) and hasText(TEST_ADDON_NAME, substring = true)

    private fun openAddons(vararg extras: Pair<String, Any>) {
        tv.launch(DebugLaunch.ROUTE to "settings/addons", *extras)
        tv.awaitFocus(hasTestTag(URL_SURFACE_TAG), TvRobot.DEFAULT_TIMEOUT_MS)
    }

    private fun assumeTestAddon() {
        val listed = runCatching { tv.await(hasText(TEST_ADDON_NAME, substring = true), TEST_ADDON_TIMEOUT_MS) }.isSuccess
        assumeTrue("Test addon server is not running", listed)
    }

    private companion object {
        const val TEST_ADDON_URL = "http://127.0.0.1:8765/manifest.json"
        const val TEST_ADDON_NAME = "Nuvio tvOS test films"
        const val INSTALL_TIMEOUT_MS = 30_000L
        const val TEST_ADDON_TIMEOUT_MS = 20_000L
        const val MAX_ADDONS_TO_REMOVE = 4
    }
}
