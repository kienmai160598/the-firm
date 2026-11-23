package com.kien.movies

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.focusGroup
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
import com.kien.movies.shell.PlaceholderScreen
import com.kien.movies.shell.RootScreen
import com.kien.movies.shell.ShellViewModel

/**
 * Hosts the shell once the bridge runtime is ready. Until then it shows a loading screen, or the
 * init error, each with one focusable control. Nothing calls a `Tvos*` API before
 * [RuntimeState.Ready].
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
        if (shell.router.isAtTabRoot) shell.router.open(route)
    }
}

@Composable
private fun RuntimeGate(runtime: RuntimeState, onReady: () -> Unit, ready: @Composable () -> Unit) {
    when (runtime) {
        RuntimeState.Starting -> FocusedPlaceholder(title = "Movies", detail = "Starting")

        is RuntimeState.Failed -> FocusedPlaceholder(title = "Movies could not start", detail = runtime.message)

        RuntimeState.Ready -> {
            LaunchedEffect(Unit) { onReady() }
            ready()
        }
    }
}

/** A placeholder that takes focus, so the remote is never stranded while the runtime starts. */
@Composable
private fun FocusedPlaceholder(title: String, detail: String) {
    val focus = remember { FocusRequester() }
    Box(modifier = Modifier.fillMaxSize().focusRequester(focus).focusGroup()) {
        PlaceholderScreen(title = title, detail = detail)
    }
    LaunchedEffect(Unit) {
        withFrameNanos {}
        focus.requestFocus()
    }
}
