package com.kien.movies.addons.phone

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleStartEffect
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Where the phone server is, for the Add Addon preview (docs/android-tv-rebuild.md, 5b). */
sealed interface AddonQrSetup {
    data object Starting : AddonQrSetup

    data object NoNetwork : AddonQrSetup

    data object NoFreePort : AddonQrSetup

    /** Stopped after 10 minutes without a request from a phone. */
    data object Expired : AddonQrSetup

    data class Running(val server: PhoneAddonServer) : AddonQrSetup
}

/**
 * The phone server of the Addons screen (tvOS AddonQrServer.swift), started on the TV's LAN
 * address while the screen is composed and the app is in the foreground: it stops when the screen
 * leaves (an addon's page, Back), at ON_STOP (and starts again with a new token at ON_START), and
 * after 10 idle minutes. Closing twice is harmless.
 */
@Composable
fun rememberAddonQrServer(install: PhoneAddonInstaller): AddonQrServer {
    val context = LocalContext.current.applicationContext
    val currentInstall by rememberUpdatedState(install)
    val qr = remember { AddonQrServer() }
    val scope = rememberCoroutineScope()
    LifecycleStartEffect(qr) {
        val run = scope.launch { qr.run(context) { urls, onDone -> currentInstall(urls, onDone) } }
        // Cancelling reaches the finally below at once, without waiting for a recomposition.
        onStopOrDispose { run.cancel() }
    }
    return qr
}

@Stable
class AddonQrServer internal constructor() {
    var setup: AddonQrSetup by mutableStateOf(AddonQrSetup.Starting)
        private set

    internal suspend fun run(context: Context, install: PhoneAddonInstaller) {
        setup = AddonQrSetup.Starting
        var server: PhoneAddonServer? = null
        try {
            // Not cancellable, and the server kept outside it: one that binds while the screen
            // leaves must still reach the stop below.
            val started = withContext(NonCancellable + Dispatchers.IO) {
                val host = LanAddress.current(context) ?: return@withContext AddonQrSetup.NoNetwork
                server = PhoneAddonServer.start(host = host, clock = SystemClock::elapsedRealtime, install = install)
                server?.let(AddonQrSetup::Running) ?: AddonQrSetup.NoFreePort
            }
            setup = started
            while (server != null) {
                delay(IDLE_CHECK_MS)
                if (server?.isIdle() == true) {
                    setup = AddonQrSetup.Expired
                    return
                }
            }
        } finally {
            server?.let(::closeOffMainThread)
            if (setup is AddonQrSetup.Running) setup = AddonQrSetup.Starting
        }
    }
}

/** `NanoHTTPD.stop` joins the accept thread: never on the main thread. */
private fun closeOffMainThread(server: PhoneAddonServer) {
    thread(name = "phone-addon-stop", isDaemon = true) { server.close() }
}

private const val IDLE_CHECK_MS = 15_000L
