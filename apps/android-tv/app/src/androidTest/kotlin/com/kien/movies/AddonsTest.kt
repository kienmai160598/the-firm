package com.kien.movies

import android.view.KeyEvent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kien.movies.addons.ADDON_ROW_TAG
import com.kien.movies.addons.EMPTY_TITLE
import com.kien.movies.addons.INSTALL_FAILED
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
 * Addons ACs A1 to A4 (docs/android-tv-rebuild.md, section 6), ported from
 * apps/apple-tv/UITests/AddonsTests.swift. A2 and A3 need the test addon of
 * apps/apple-tv/TestAddon on 127.0.0.1:8765 (adb reverse) and are skipped without it. A4 removes
 * every installed addon: run it on a cleared app (`pm clear com.kien.movies.next`).
 */
@RunWith(AndroidJUnit4::class)
class AddonsTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val tv = TvRobot(compose)

    @After
    fun closeApp() = tv.close()

    @Test
    fun a1_aManifestThatCannotBeReachedShowsWhyItFailed() {
        openAddons()
        compose.onNodeWithTag(URL_SURFACE_TAG).requestFocus()
        tv.select()
        tv.awaitFocus(hasTestTag(URL_FIELD_TAG))

        compose.onNodeWithTag(URL_FIELD_TAG).performTextInput("http://127.0.0.1:9")
        compose.onNodeWithTag(URL_FIELD_TAG).performImeAction()

        tv.await(hasText(INSTALL_FAILED), INSTALL_TIMEOUT_MS)
        tv.assertFocusedTag(URL_SURFACE_TAG)
    }

    @Test
    fun a2_turnOffMarksTheAddonOff() {
        openAddons(DebugLaunch.ADDON_URL to TEST_ADDON_URL)
        assumeTestAddon()
        focusRow(TEST_ADDON_NAME)

        tv.select()
        tv.awaitFocus(hasTestTag(dialogButtonTag("Turn Off")))
        tv.select()

        tv.await(hasTestTag(ADDON_ROW_TAG) and hasText(TEST_ADDON_NAME, substring = true) and hasText("Off"))
    }

    @Test
    fun a3_moveUpPutsTheAddonFirst() {
        openAddons(DebugLaunch.ADDON_URL to TEST_ADDON_URL)
        assumeTestAddon()
        focusRow(TEST_ADDON_NAME)
        tv.select()
        tv.awaitFocus(hasTestTag(dialogButtonTag("Turn Off")) or hasTestTag(dialogButtonTag("Turn On")))
        pressUntilFocused(KeyEvent.KEYCODE_DPAD_DOWN, hasTestTag(dialogButtonTag("Move Up")))

        tv.select()

        compose.waitUntil(TvRobot.FOCUS_TIMEOUT_MS) {
            val firstRow = compose.onAllNodes(hasTestTag(ADDON_ROW_TAG)).fetchSemanticsNodes().firstOrNull()
            firstRow != null && hasText(TEST_ADDON_NAME, substring = true).matches(firstRow)
        }
    }

    @Test
    fun a4_removingEveryAddonShowsTheEmptyStateAndKeepsFocus() {
        openAddons()
        repeat(MAX_ADDONS_TO_REMOVE) {
            if (compose.onAllNodes(hasTestTag(ADDON_ROW_TAG)).fetchSemanticsNodes().isNotEmpty()) removeFirstAddon()
        }

        tv.await(hasText(EMPTY_TITLE), TvRobot.FOCUS_TIMEOUT_MS)

        tv.assertFocusedTag(URL_SURFACE_TAG)
    }

    private fun removeFirstAddon() {
        compose.onAllNodes(hasTestTag(ADDON_ROW_TAG)).onFirstRequestFocus()
        tv.select()
        tv.awaitFocus(hasTestTag(dialogButtonTag("Turn Off")) or hasTestTag(dialogButtonTag("Turn On")))
        pressUntilFocused(KeyEvent.KEYCODE_DPAD_DOWN, hasTestTag(dialogButtonTag("Remove")))
        tv.select()
        tv.await(hasText("Remove ", substring = true) and hasText("?", substring = true))
        tv.awaitFocus(hasTestTag(dialogButtonTag("Cancel")))
        tv.press(KeyEvent.KEYCODE_DPAD_RIGHT)
        tv.awaitFocus(hasTestTag(dialogButtonTag("Remove")))
        tv.select()
        compose.waitUntil(TvRobot.FOCUS_TIMEOUT_MS) { compose.onAllNodes(isFocused()).fetchSemanticsNodes().size == 1 }
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteractionCollection.onFirstRequestFocus() {
        get(0).requestFocus()
        compose.waitForIdle()
    }

    private fun openAddons(vararg extras: Pair<String, Any>) {
        tv.launch(DebugLaunch.ROUTE to "tab/addons", *extras)
        tv.await(hasTestTag(URL_SURFACE_TAG))
    }

    private fun assumeTestAddon() {
        val listed = runCatching { tv.await(hasText(TEST_ADDON_NAME, substring = true), TEST_ADDON_TIMEOUT_MS) }.isSuccess
        assumeTrue("Test addon server is not running", listed)
    }

    private fun focusRow(name: String) {
        compose.onAllNodes(hasTestTag(ADDON_ROW_TAG)).onFirstRequestFocus()
        pressUntilFocused(KeyEvent.KEYCODE_DPAD_DOWN, hasTestTag(ADDON_ROW_TAG) and hasText(name, substring = true))
    }

    private fun pressUntilFocused(keyCode: Int, matcher: androidx.compose.ui.test.SemanticsMatcher) {
        repeat(MAX_PRESSES) {
            if (compose.onAllNodes(isFocused() and matcher).fetchSemanticsNodes().isNotEmpty()) return
            tv.press(keyCode)
        }
        tv.awaitFocus(matcher)
    }

    private companion object {
        const val TEST_ADDON_URL = "http://127.0.0.1:8765/manifest.json"
        const val TEST_ADDON_NAME = "Nuvio tvOS test films"
        const val INSTALL_TIMEOUT_MS = 30_000L
        const val TEST_ADDON_TIMEOUT_MS = 20_000L
        const val MAX_ADDONS_TO_REMOVE = 4
        const val MAX_PRESSES = 5
    }
}
