package com.kien.movies.addons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kien.movies.settings.SettingsLinkLabel
import com.kien.movies.settings.SettingsMetrics
import com.kien.movies.settings.SettingsSectionFooter
import com.kien.movies.settings.SettingsSectionHeader
import com.kien.movies.settings.SettingsSplit
import com.kien.movies.shell.Router
import com.nuvio.tvos.TvosAddon

/**
 * One addon's page (tvOS AddonDetailView.swift), like an app's page in Settings: Enabled turns it
 * off in place and has first focus, Move changes its order, About lists what it is, and Remove
 * asks first, then goes back to the list.
 */
@Composable
fun AddonDetailScreen(manifestUrl: String, router: Router) {
    val viewModel = viewModel { AddonsViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmsRemoval by remember { mutableStateOf(false) }
    val addon = state.addon(manifestUrl) ?: return

    SettingsSplit(
        title = addon.name,
        preview = { AddonSummary(addon, showsDescription = false) },
        list = {
            AddonPageRows(
                addon = addon,
                canMoveUp = moveDestination(state.addons, manifestUrl, AddonMove.Up) != null,
                canMoveDown = moveDestination(state.addons, manifestUrl, AddonMove.Down) != null,
                onToggle = { viewModel.toggle(addon) },
                onMove = { move -> viewModel.move(addon, move) },
                onRemove = { confirmsRemoval = true }
            )
        }
    )
    if (confirmsRemoval) {
        RemoveAddonDialog(
            addon = addon,
            onRemove = {
                confirmsRemoval = false
                viewModel.remove(addon)
                router.back()
            },
            onDismiss = { confirmsRemoval = false }
        )
    }
}

@Composable
private fun AddonPageRows(
    addon: TvosAddon,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: () -> Unit,
    onMove: (AddonMove) -> Unit,
    onRemove: () -> Unit
) {
    val enabledFocus = remember { FocusRequester() }
    val moveFocus = remember { AddonMove.entries.associateWith { FocusRequester() } }
    // The row that last took focus; kept when it leaves, so a row that went away is still known.
    var focusedRow by remember { mutableStateOf("") }
    val tracked = { key: String -> Modifier.onFocusChanged { focus -> if (focus.hasFocus) focusedRow = key } }
    val shown = { move: AddonMove -> if (move == AddonMove.Up) canMoveUp else canMoveDown }
    // A move to the edge takes its row away while it has focus: the other move, else Enabled, takes it.
    LaunchedEffect(canMoveUp, canMoveDown) {
        val lost = AddonMove.entries.firstOrNull { move -> focusedRow == move.name && !shown(move) } ?: return@LaunchedEffect
        val other = AddonMove.entries.first { move -> move != lost }
        withFrameNanos {}
        (if (shown(other)) moveFocus.getValue(other) else enabledFocus).requestFocus()
    }
    val moveRow = @Composable { move: AddonMove ->
        SettingsLinkLabel(
            title = if (move == AddonMove.Up) "Move Up" else "Move Down",
            onClick = { onMove(move) },
            trailingIcon = null,
            modifier = Modifier
                .focusRequester(moveFocus.getValue(move))
                .then(tracked(move.name))
                .testTag(if (move == AddonMove.Up) MOVE_UP_TAG else MOVE_DOWN_TAG)
        )
    }

    LazyColumn(
        modifier = Modifier.focusRestorer(enabledFocus),
        contentPadding = PaddingValues(bottom = BOTTOM_ROOM),
        verticalArrangement = Arrangement.spacedBy(SettingsMetrics.rowGap)
    ) {
        item(key = "enabled") {
            SettingsLinkLabel(
                title = "Enabled",
                value = if (addon.enabled) "On" else "Off",
                onClick = onToggle,
                trailingIcon = null,
                modifier = Modifier.focusRequester(enabledFocus).then(tracked("enabled")).testTag(ENABLED_TAG)
            )
        }
        AddonMove.entries.filter(shown).forEach { move -> item(key = move.name) { moveRow(move) } }
        item(key = "about") { SettingsSectionHeader(ABOUT) }
        if (addon.version.isNotEmpty()) item(key = "version") { Fact("Version", addon.version, tracked("version")) }
        if (addon.capabilities.isNotEmpty()) item(key = "provides") { Fact("Provides", addon.capabilities.joinToString(", "), tracked("provides")) }
        if (addon.summary.isNotEmpty()) item(key = "summary") { SettingsSectionFooter(addon.summary) }
        item(key = "remove") {
            SettingsLinkLabel(title = "Remove Addon", onClick = onRemove, trailingIcon = null, modifier = tracked("remove").testTag(REMOVE_TAG))
        }
    }
}

/** An About row: focusable but inert, as the About page's rows, so the remote can read it. */
@Composable
private fun Fact(title: String, value: String, modifier: Modifier) {
    SettingsLinkLabel(title = title, value = value, onClick = {}, trailingIcon = null, modifier = modifier)
}

const val ENABLED_TAG = "addon-enabled"
const val MOVE_UP_TAG = "addon-move-up"
const val MOVE_DOWN_TAG = "addon-move-down"
const val REMOVE_TAG = "addon-remove"

private const val ABOUT = "About"
private val BOTTOM_ROOM = 27.dp // the bottom safe margin, as on the language pages
