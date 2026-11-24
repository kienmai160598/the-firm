package com.kien.movies

import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kien.movies.debug.DebugLaunch
import com.kien.movies.settings.SettingsPage
import com.kien.movies.settings.TRAKT_SIGN_IN_TAG
import com.kien.movies.settings.languageRowTag
import com.kien.movies.settings.settingsRowTag
import com.kien.movies.support.TvRobot
import com.nuvio.tvos.TVOS_LANGUAGE_OFF
import com.nuvio.tvos.TvosSettings
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Settings (tvOS SettingsTests.swift): the pages it lists, a language choice that sticks, and the
 * Account page of a build without Trakt credentials. ST2 needs the fresh state of
 * `adb shell pm clear com.kien.movies.next` and puts Vietnamese back when it ends.
 */
@RunWith(AndroidJUnit4::class)
class SettingsTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val tv = TvRobot(compose)

    @After
    fun closeApp() = tv.close()

    @Test
    fun st1_settingsListsItsPagesAndAboutShowsTheVersion() {
        openSettingsTab()
        SettingsPage.entries.forEach { page -> tv.await(hasTestTag(settingsRowTag(page))) }

        openPage(SettingsPage.About)

        tv.await(hasContentDescription("Version, ", substring = true))
    }

    @Test
    fun st2_subtitlesStartVietnameseAndOffSticks() {
        try {
            openSettingsTab()
            openPage(SettingsPage.Subtitles)
            tv.awaitFocus(hasTestTag(languageRowTag("vi")))

            pressUntilFocused(KeyEvent.KEYCODE_DPAD_UP, languageRowTag(TVOS_LANGUAGE_OFF))
            tv.select()
            tv.back()

            tv.awaitFocus(hasTestTag(settingsRowTag(SettingsPage.Subtitles)) and hasContentDescription("Subtitles, Off"))
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { TvosSettings.setSubtitles(FIRST_SUBTITLES) }
        }
    }

    /** Builds without TRAKT_CLIENT_ID and TRAKT_CLIENT_SECRET, as in CI, cannot sign in and say so. */
    @Test
    fun st5_accountWithoutTraktCredentialsSaysItIsNotAvailable() {
        openSettingsTab()
        val account = compose.onNode(hasTestTag(settingsRowTag(SettingsPage.Account))).fetchSemanticsNode()
        val label = account.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString().orEmpty()
        assumeTrue("This build has Trakt credentials", label.endsWith("Not Available"))

        tv.select()

        tv.await(hasTestTag(TRAKT_SIGN_IN_TAG))
        tv.awaitFocus(hasTestTag(TRAKT_SIGN_IN_TAG) and hasContentDescription("Sign In to Trakt, Not Available"))
    }

    /** Opens the Settings tab; its first row, Account, takes focus. */
    private fun openSettingsTab() {
        tv.launch(DebugLaunch.ROUTE to "tab/settings")
        tv.awaitFocus(hasTestTag(settingsRowTag(SettingsPage.Account)), TvRobot.DEFAULT_TIMEOUT_MS)
    }

    /** From the Account row, Down to [page]'s row and Select. */
    private fun openPage(page: SettingsPage) {
        pressUntilFocused(KeyEvent.KEYCODE_DPAD_DOWN, settingsRowTag(page))
        tv.select()
    }

    private fun pressUntilFocused(keyCode: Int, tag: String) {
        repeat(MAX_PRESSES) {
            if (compose.onAllNodes(isFocused() and hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()) return
            tv.press(keyCode)
        }
        tv.assertFocusedTag(tag)
    }

    private companion object {
        const val FIRST_SUBTITLES = "vi"
        const val MAX_PRESSES = 90
    }
}
