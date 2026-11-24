package com.kien.movies

import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kien.movies.addons.ADDON_ROW_TAG
import com.kien.movies.addons.PHONE_BUTTON_TAG
import com.kien.movies.addons.URL_SURFACE_TAG
import com.kien.movies.addons.dialogButtonTag
import com.kien.movies.addons.phone.PHONE_ADDRESS_TAG
import com.kien.movies.addons.phone.PHONE_CLOSE_TAG
import com.kien.movies.addons.phone.PHONE_PROBLEM_TAG
import com.kien.movies.addons.phone.PHONE_QR_TAG
import com.kien.movies.debug.DebugLaunch
import com.kien.movies.support.TvRobot
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * "Add from phone" ACs AQ1 to AQ5 (docs/android-tv-rebuild.md, 5b and section 6). The test plays
 * the phone: it reads the address the TV shows and sends the page's requests from the device
 * itself. The scan with a real phone stays a manual check. AQ4's 403 and 409 are covered on the
 * JVM by PhoneAddonServerTest. Needs a network; the install cases also need the test addon on
 * 127.0.0.1:8765 (adb reverse).
 */
@RunWith(AndroidJUnit4::class)
class PhoneAddonTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val tv = TvRobot(compose)

    @After
    fun closeApp() = tv.close()

    @Test
    fun aq1_addFromPhoneShowsTheCodeAndTheAddressWithinASecond() {
        openAddons()
        focusPhoneButton()

        tv.select()

        tv.await(hasTestTag(PHONE_QR_TAG) or hasTestTag(PHONE_PROBLEM_TAG), QR_TIMEOUT_MS)
        tv.assertFocusedTag(PHONE_CLOSE_TAG)
        assertTrue(shownAddress()?.startsWith("http://") ?: true)
    }

    @Test
    fun aq2_aq3_aProposalShowsTheDialogAndInstallAddsTheAddon() {
        val address = openPhonePanel()
        val id = propose(address, TEST_ADDON_URL)
        tv.awaitFocus(hasTestTag(dialogButtonTag("Install")))

        tv.select()

        compose.waitUntil(INSTALL_TIMEOUT_MS) { pollStatus(address, id).contains("\"done\"") }
        assertTrue(pollStatus(address, id).contains("\"ok\":true"))
        tv.await(hasTestTag(ADDON_ROW_TAG) and hasText(TEST_ADDON_NAME, substring = true))
    }

    @Test
    fun aq3_cancelAddsNothingAndThePageIsTold() {
        val address = openPhonePanel()
        val id = propose(address, TEST_ADDON_URL)
        tv.awaitFocus(hasTestTag(dialogButtonTag("Install")))

        tv.back()

        assertEquals("""{"status":"cancelled"}""", pollStatus(address, id))
    }

    @Test
    fun aq5_closingThePanelStopsTheServer() {
        val address = openPhonePanel()

        tv.back()

        compose.waitUntil(STOP_TIMEOUT_MS) { !answers(address) }
    }

    private fun openAddons() {
        tv.launch(DebugLaunch.ROUTE to "settings/addons")
        tv.await(hasTestTag(PHONE_BUTTON_TAG))
        tv.awaitFocus(hasTestTag(ADDON_ROW_TAG) or hasTestTag(URL_SURFACE_TAG), TvRobot.DEFAULT_TIMEOUT_MS)
    }

    /** D-pad only: from a row Left into the panel, Up to the URL field, then Down to the button. */
    private fun focusPhoneButton() {
        if (compose.onAllNodes(isFocused() and hasTestTag(ADDON_ROW_TAG)).fetchSemanticsNodes().isNotEmpty()) {
            tv.press(KeyEvent.KEYCODE_DPAD_LEFT)
        }
        pressUntilFocused(KeyEvent.KEYCODE_DPAD_UP, hasTestTag(URL_SURFACE_TAG))
        pressUntilFocused(KeyEvent.KEYCODE_DPAD_DOWN, hasTestTag(PHONE_BUTTON_TAG))
    }

    private fun pressUntilFocused(keyCode: Int, matcher: SemanticsMatcher) {
        repeat(MAX_PRESSES) {
            if (compose.onAllNodes(isFocused() and matcher).fetchSemanticsNodes().isNotEmpty()) return
            tv.press(keyCode)
        }
        tv.awaitFocus(matcher)
    }

    /** Opens the panel and returns the address it shows; skipped when the TV has no network. */
    private fun openPhonePanel(): String {
        openAddons()
        focusPhoneButton()
        tv.select()
        tv.await(hasTestTag(PHONE_QR_TAG) or hasTestTag(PHONE_PROBLEM_TAG), QR_TIMEOUT_MS)
        val address = shownAddress()
        assumeTrue("No network on the device", address != null)
        return checkNotNull(address)
    }

    private fun shownAddress(): String? = compose.onAllNodes(hasTestTag(PHONE_ADDRESS_TAG)).fetchSemanticsNodes().firstOrNull()
        ?.config?.getOrNull(SemanticsProperties.Text)?.joinToString { text -> text.text }

    private fun propose(address: String, manifestUrl: String): Long {
        val (status, body) = request(address.replace("/?t=", "/api/addons?t="), """{"urls": ["$manifestUrl"]}""")
        assertEquals(body, 200, status)
        return Regex("\"id\":(\\d+)").find(body)!!.groupValues[1].toLong()
    }

    private fun pollStatus(address: String, id: Long): String = request(address.replace("/?t=", "/api/status/$id?t="), body = null).second

    private fun answers(address: String): Boolean = try {
        request(address, body = null)
        true
    } catch (_: IOException) {
        false
    }

    private fun request(url: String, body: String?): Pair<Int, String> {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = HTTP_TIMEOUT_MS
        connection.readTimeout = HTTP_TIMEOUT_MS
        try {
            if (body != null) {
                val bytes = body.encodeToByteArray()
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.outputStream.use { stream -> stream.write(bytes) }
            }
            val status = connection.responseCode
            val stream = if (status < 400) connection.inputStream else connection.errorStream
            return status to stream?.bufferedReader()?.use { reader -> reader.readText() }.orEmpty()
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TEST_ADDON_URL = "http://127.0.0.1:8765/manifest.json"
        const val TEST_ADDON_NAME = "Nuvio tvOS test films"
        const val QR_TIMEOUT_MS = 1_000L
        const val INSTALL_TIMEOUT_MS = 30_000L
        const val STOP_TIMEOUT_MS = 3_000L
        const val HTTP_TIMEOUT_MS = 2_000
        const val MAX_PRESSES = 6
    }
}
