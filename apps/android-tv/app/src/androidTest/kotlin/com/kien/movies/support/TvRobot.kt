package com.kien.movies.support

import android.content.Intent
import android.view.KeyEvent
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.kien.movies.MainActivity
import org.junit.Assert.assertEquals

/**
 * Drives the app like the remote does (apps/apple-tv/UITests/Support): launch with the
 * DebugLaunch extras, D-pad keys sent through the Activity's window, and waits on the semantics
 * tree. Use it with `createEmptyComposeRule()`.
 *
 * Runs on the `debug` build only. Never `connectedAndroidTest` on the box: it uninstalls the app
 * afterwards and wipes its data; use `adb install -r` and `am instrument -e class ...`. A fresh
 * state is `adb shell pm clear com.kien.movies.next` from the host before the run.
 */
class TvRobot(private val compose: ComposeTestRule) {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var scenario: ActivityScenario<MainActivity>? = null

    /** Launches MainActivity; [extras] are DebugLaunch names and values (String, Boolean or Int). */
    fun launch(vararg extras: Pair<String, Any>): TvRobot {
        val intent = Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        extras.forEach { (name, value) ->
            when (value) {
                is String -> intent.putExtra(name, value)
                is Boolean -> intent.putExtra(name, value)
                is Int -> intent.putExtra(name, value)
                else -> error("Unsupported extra type for $name: ${value::class}")
            }
        }
        scenario = ActivityScenario.launch(intent)
        return this
    }

    fun press(keyCode: Int) {
        instrumentation.sendKeyDownUpSync(keyCode)
        compose.waitForIdle()
    }

    fun back() = press(KeyEvent.KEYCODE_BACK)

    fun down() = press(KeyEvent.KEYCODE_DPAD_DOWN)

    fun select() = press(KeyEvent.KEYCODE_DPAD_CENTER)

    /** Waits until a node matching [matcher] exists. */
    fun await(matcher: SemanticsMatcher, timeoutMillis: Long = DEFAULT_TIMEOUT_MS) {
        compose.waitUntil(timeoutMillis) {
            compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Waits until no node matches [matcher], such as a screen that faded out. */
    fun awaitGone(matcher: SemanticsMatcher, timeoutMillis: Long = FOCUS_TIMEOUT_MS) {
        compose.waitUntil(timeoutMillis) {
            compose.onAllNodes(matcher).fetchSemanticsNodes().isEmpty()
        }
    }

    /** Waits until exactly one node has focus and it matches [matcher]. */
    fun awaitFocus(matcher: SemanticsMatcher, timeoutMillis: Long = FOCUS_TIMEOUT_MS) {
        compose.waitUntil(timeoutMillis) {
            compose.onAllNodes(isFocused() and matcher).fetchSemanticsNodes().size == 1
        }
    }

    /** Presses [keyCode] until a node matching [matcher] has focus, at most [maxPresses] times. */
    fun pressUntilFocused(keyCode: Int, matcher: SemanticsMatcher, maxPresses: Int = MAX_PRESSES) {
        repeat(maxPresses) {
            if (compose.onAllNodes(isFocused() and matcher).fetchSemanticsNodes().isNotEmpty()) return
            press(keyCode)
        }
        awaitFocus(matcher)
    }

    fun assertFocusedTag(tag: String) {
        awaitFocus(hasTestTag(tag))
        compose.onNodeWithTag(tag).assertIsFocused()
    }

    /** Waits until the Activity is destroyed, which is what leaving to the launcher looks like. */
    fun awaitExit(timeoutMillis: Long = DEFAULT_TIMEOUT_MS) {
        val launched = checkNotNull(scenario) { "launch() first" }
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (launched.state != Lifecycle.State.DESTROYED && System.currentTimeMillis() < deadline) {
            Thread.sleep(POLL_MS)
        }
        assertEquals("Back did not leave the app", Lifecycle.State.DESTROYED, launched.state)
    }

    fun close() {
        scenario?.close()
        scenario = null
    }

    companion object {
        /** The first launch initialises about 60 storages and installs the default addon. */
        const val DEFAULT_TIMEOUT_MS = 60_000L
        const val FOCUS_TIMEOUT_MS = 5_000L
        private const val POLL_MS = 100L
        private const val MAX_PRESSES = 8
    }
}
