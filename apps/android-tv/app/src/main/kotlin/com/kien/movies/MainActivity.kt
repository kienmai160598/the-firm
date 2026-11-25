package com.kien.movies

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kien.movies.debug.DebugLaunch
import com.kien.movies.design.MoviesTheme
import com.kien.movies.design.TvColors
import com.kien.movies.shell.AppRoute
import com.kien.movies.shell.PlaceholderScreen
import com.kien.movies.shell.RootScreen
import com.kien.movies.shell.RootTab
import com.kien.movies.shell.ShellViewModel
import com.kien.movies.shell.WindowBackground

/**
 * Hosts the shell once the bridge runtime is ready. Until then it shows only the window colour (no
 * splash, as tvOS shows none of its own), or the init error, each holding focus. Nothing calls a
 * `Tvos*` API before [RuntimeState.Ready].
 */
class MainActivity : ComponentActivity() {
    private val shell: ShellViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val firstCreate = savedInstanceState == null
        if (firstCreate) DebugLaunch.read(intent.extras)
        val app = application as MoviesApp
        setContent {
            MoviesTheme {
                val runtime by app.runtimeState.collectAsStateWithLifecycle()
                RuntimeGate(runtime, onReady = { if (firstCreate) openDebugLaunch() }) {
                    RootScreen(router = shell.router, stores = shell.stores, onExit = ::finish)
                }
            }
        }
    }

    private fun openDebugLaunch() {
        DebugLaunch.onRuntimeReady()
        val route = DebugLaunch.initialRoute ?: return
        if (!shell.router.isAtTabRoot) return
        // A Settings page opens over its tab, so Back lands on the Settings list as on tvOS.
        if (route is AppRoute.Settings) shell.router.selectTab(RootTab.Settings)
        shell.router.open(route)
    }
}

@Composable
private fun RuntimeGate(runtime: RuntimeState, onReady: () -> Unit, ready: @Composable () -> Unit) {
    when (runtime) {
        RuntimeState.Starting -> HoldsFocus { modifier -> Box(modifier = modifier.focusable()) }

        is RuntimeState.Failed -> FocusedPlaceholder(title = "Movies could not start", detail = runtime.message)

        RuntimeState.Ready -> {
            LaunchedEffect(Unit) { onReady() }
            ready()
        }
    }
}

/** The init error in a placeholder that takes focus. */
@Composable
private fun FocusedPlaceholder(title: String, detail: String) {
    // Before the shell exists the gate sets the window colour itself (the shell does it after).
    WindowBackground(TvColors.searchLibrary)
    HoldsFocus { modifier ->
        Box(modifier = modifier.focusGroup()) { PlaceholderScreen(title = title, detail = detail) }
    }
}

/**
 * Full screen [content] that takes focus on its first frame, so the remote is never stranded
 * before the shell exists. Starting draws nothing over the theme's window colour, which is Home's,
 * so launch goes straight to Home with no colour change.
 */
@Composable
private fun HoldsFocus(content: @Composable (Modifier) -> Unit) {
    val focus = remember { FocusRequester() }
    content(Modifier.fillMaxSize().focusRequester(focus))
    LaunchedEffect(Unit) {
        withFrameNanos {}
        focus.requestFocus()
    }
}
