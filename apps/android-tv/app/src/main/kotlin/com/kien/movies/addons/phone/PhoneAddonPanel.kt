package com.kien.movies.addons.phone

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.kien.movies.addons.ERROR_RED
import com.kien.movies.addons.SectionTitle
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvControls
import com.kien.movies.design.TvGlass
import com.kien.movies.design.TvType
import com.nuvio.tvos.phone.AddonInstallOutcome
import com.nuvio.tvos.phone.AddonPairingLimits
import com.nuvio.tvos.phone.AddonPairingPhase
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** What the panel shows before and while the server runs (docs/android-tv-rebuild.md, 5b). */
private sealed interface PhoneAddonSetup {
    data object Starting : PhoneAddonSetup

    data object NoNetwork : PhoneAddonSetup

    data object NoFreePort : PhoneAddonSetup

    data class Running(val server: PhoneAddonServer) : PhoneAddonSetup
}

/**
 * "Add from phone" in the left column of Addons: starts the server on the TV's LAN address, shows
 * the QR code and the address as text, the confirmation dialog for a proposal and the results.
 *
 * The server lives exactly as long as this panel: it stops when the panel closes (Close, Back, a
 * tab change), when the app goes to the background (ON_STOP closes it and the panel) and after 10
 * idle minutes; closing twice is harmless. Every state holds a focusable Close button, [closeFocus].
 */
@Composable
fun PhoneAddonPanel(
    install: PhoneAddonInstaller,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    closeFocus: FocusRequester = remember { FocusRequester() }
) {
    val context = LocalContext.current.applicationContext
    val currentInstall by rememberUpdatedState(install)
    val currentOnClose by rememberUpdatedState(onClose)
    var setup by remember { mutableStateOf<PhoneAddonSetup>(PhoneAddonSetup.Starting) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(attempt) {
        setup = PhoneAddonSetup.Starting
        // Not cancellable: a server that bound while the panel closed must still reach the stop below.
        val started = withContext(NonCancellable + Dispatchers.IO) {
            val host = LanAddress.current(context) ?: return@withContext PhoneAddonSetup.NoNetwork
            val server = PhoneAddonServer.start(
                host = host,
                clock = SystemClock::elapsedRealtime,
                install = { urls, onDone -> currentInstall(urls, onDone) }
            )
            server?.let(PhoneAddonSetup::Running) ?: PhoneAddonSetup.NoFreePort
        }
        val server = (started as? PhoneAddonSetup.Running)?.server
        try {
            setup = started
            while (server != null) {
                delay(IDLE_CHECK_MS)
                if (server.isIdle()) currentOnClose()
            }
        } finally {
            server?.let(::closeOffMainThread)
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        // Stop at once: recomposition, which would reach the effect's finally, pauses in the background.
        (setup as? PhoneAddonSetup.Running)?.server?.let(::closeOffMainThread)
        currentOnClose()
    }
    BackHandler(onBack = onClose)
    LaunchedEffect(Unit) {
        withFrameNanos {}
        closeFocus.requestFocus()
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).focusGroup(),
        verticalArrangement = Arrangement.spacedBy(PANEL_SPACING)
    ) {
        SectionTitle("Add from phone", spacingAfter = PANEL_SPACING)
        when (val current = setup) {
            PhoneAddonSetup.Starting -> Text(text = "Starting", style = TvType.caption1, color = TvColors.textSecondary)
            PhoneAddonSetup.NoNetwork -> Problem(NO_NETWORK, onRetry = { attempt++ })
            PhoneAddonSetup.NoFreePort -> Problem(NO_FREE_PORT, onRetry = { attempt++ })
            is PhoneAddonSetup.Running -> RunningServer(current.server)
        }
        Button(
            onClick = onClose,
            colors = TvGlass.buttonColors(),
            scale = TvGlass.buttonScale(),
            shape = TvGlass.pillShape(),
            border = TvGlass.buttonBorder(),
            contentPadding = TvGlass.pillPadding,
            modifier = Modifier.height(TvControls.pillHeight).focusRequester(closeFocus).testTag(PHONE_CLOSE_TAG)
        ) {
            Text(text = "Close", style = TvType.bodyEmphasized)
        }
    }
}

@Composable
private fun RunningServer(server: PhoneAddonServer) {
    val session by server.session.collectAsStateWithLifecycle()
    val address = remember(server) { server.address }
    Text(text = "Scan the code with a phone on the same network as the TV.", style = TvType.caption2, color = TvColors.textSecondary)
    QrCodeImage(content = address, minSide = QR_MIN_SIDE, modifier = Modifier.testTag(PHONE_QR_TAG))
    Text(text = address, style = TvType.caption2, color = TvColors.textSecondary, modifier = Modifier.testTag(PHONE_ADDRESS_TAG))
    when (val phase = session.phase) {
        AddonPairingPhase.Off, AddonPairingPhase.Waiting -> Waiting()

        is AddonPairingPhase.Proposed -> {
            Text(text = "Confirm on the TV", style = TvType.caption1)
            PhoneAddonDialog(
                proposal = phase.proposal,
                onInstall = { server.confirm(phase.proposal.id) },
                onCancel = { server.cancel(phase.proposal.id) }
            )
        }

        is AddonPairingPhase.Installing -> Text(text = "Installing", style = TvType.caption1)

        is AddonPairingPhase.Done -> {
            Outcomes(phase.outcomes)
            Waiting()
        }
    }
}

@Composable
private fun Waiting() {
    Text(text = "Waiting for your phone", style = TvType.caption1, modifier = Modifier.testTag(PHONE_WAITING_TAG))
}

@Composable
private fun Outcomes(outcomes: List<AddonInstallOutcome>) {
    Column(verticalArrangement = Arrangement.spacedBy(OUTCOME_GAP)) {
        outcomes.forEach { outcome ->
            when (outcome) {
                is AddonInstallOutcome.Installed -> Text(text = "Installed ${outcome.name}", style = TvType.caption2, color = TvColors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                is AddonInstallOutcome.Failed -> Text(text = "Couldn't add ${outcome.url}: ${outcome.message}", style = TvType.caption2, color = ERROR_RED, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun Problem(message: String, onRetry: () -> Unit) {
    Text(text = message, style = TvType.caption1, color = TvColors.textSecondary, modifier = Modifier.testTag(PHONE_PROBLEM_TAG))
    Button(
        onClick = onRetry,
        colors = TvGlass.buttonColors(),
        scale = TvGlass.buttonScale(),
        shape = TvGlass.pillShape(),
        border = TvGlass.buttonBorder(),
        contentPadding = TvGlass.pillPadding,
        modifier = Modifier.height(TvControls.pillHeight)
    ) {
        Text(text = "Try Again", style = TvType.bodyEmphasized)
    }
}

/** `NanoHTTPD.stop` joins the accept thread: never on the main thread. */
private fun closeOffMainThread(server: PhoneAddonServer) {
    thread(name = "phone-addon-stop", isDaemon = true) { server.close() }
}

const val PHONE_CLOSE_TAG = "phone-addon-close"
const val PHONE_QR_TAG = "phone-addon-qr"
const val PHONE_ADDRESS_TAG = "phone-addon-address"
const val PHONE_WAITING_TAG = "phone-addon-waiting"
const val PHONE_PROBLEM_TAG = "phone-addon-problem"
const val NO_NETWORK = "Connect the TV to Wi-Fi or Ethernet."
val NO_FREE_PORT = "Ports ${AddonPairingLimits.FIRST_PORT} to ${AddonPairingLimits.LAST_PORT} are all in use. Try again in a moment."

/** At least 180 dp so a phone reads it across the room (5b). */
private val QR_MIN_SIDE = 180.dp
private val PANEL_SPACING = 12.dp
private val OUTCOME_GAP = 4.dp
private const val IDLE_CHECK_MS = 15_000L
