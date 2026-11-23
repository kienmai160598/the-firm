package com.kien.movies.addons

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import com.kien.movies.addons.phone.PhoneAddonPanel
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvGrid
import com.kien.movies.design.TvInsets
import com.kien.movies.design.TvType
import com.nuvio.tvos.TvosAddon

/**
 * Addons tab root (tvOS AddonsView.swift): the install panel on the left, the installed addons on
 * the right, on the column grid (left one of three columns, list two) and the Search slate. Selecting an addon opens its actions (turn off or on, move up or down, remove with a
 * confirmation). First focus is the first addon, as tvOS `defaultFocus`; with no addons, the URL
 * field. "Add from phone" replaces the install panel while it is open (5b).
 */
@Composable
fun AddonsScreen() {
    val viewModel = viewModel { AddonsViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var chosenUrl by remember { mutableStateOf<String?>(null) }
    var removalUrl by remember { mutableStateOf<String?>(null) }
    var showsPhonePanel by remember { mutableStateOf(false) }
    var phoneButtonFocusTick by remember { mutableIntStateOf(0) }
    var focusAfterRemoval by remember { mutableStateOf<FocusAfterRemoval?>(null) }
    val fieldFocus = remember { FocusRequester() }
    val phoneButtonFocus = remember { FocusRequester() }
    val phoneCloseFocus = remember { FocusRequester() }
    val firstRowFocus = remember { FocusRequester() }
    val rowFocus = remember { mutableMapOf<String, FocusRequester>() }

    Row(
        modifier = Modifier
            .fillMaxSize()
            // lead: drop this fill when RootScreen gives Search, Library and Addons the slate as
            // the window background; it is one opaque full screen layer until then.
            .background(TvColors.searchLibrary)
            .padding(start = TvGrid.sideMargin, end = TvGrid.sideMargin, top = TvInsets.tabContentTop)
            .focusRestorer(if (state.addons.isEmpty()) fieldFocus else firstRowFocus)
            .focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(PANEL_GAP)
    ) {
        Box(modifier = Modifier.width(PANEL_WIDTH).fillMaxHeight().padding(bottom = TvInsets.verticalMargin).focusGroup()) {
            if (showsPhonePanel) {
                PhoneAddonPanel(
                    install = viewModel::installFromPhone,
                    closeFocus = phoneCloseFocus,
                    onClose = {
                        showsPhonePanel = false
                        phoneButtonFocusTick++
                    }
                )
            } else {
                AddonInstallPanel(
                    state = state,
                    onInstall = viewModel::install,
                    onAddFromPhone = { showsPhonePanel = true },
                    fieldFocus = fieldFocus,
                    phoneButtonFocus = phoneButtonFocus
                )
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxHeight().focusGroup()) {
            if (state.addons.isEmpty()) {
                EmptyState()
            } else {
                InstalledList(
                    addons = state.addons,
                    firstRowFocus = firstRowFocus,
                    rowFocus = rowFocus,
                    onChoose = { addon -> chosenUrl = addon.manifestUrl }
                )
            }
        }
    }

    state.addons.firstOrNull { addon -> addon.manifestUrl == chosenUrl }?.let { addon ->
        AddonActionDialog(
            addon = addon,
            canMoveUp = moveDestination(state.addons, addon.manifestUrl, AddonMove.Up) != null,
            canMoveDown = moveDestination(state.addons, addon.manifestUrl, AddonMove.Down) != null,
            onToggle = { viewModel.toggle(addon) },
            onMove = { move -> viewModel.move(addon, move) },
            onRemove = { removalUrl = addon.manifestUrl },
            onDismiss = { chosenUrl = null }
        )
    }
    state.addons.firstOrNull { addon -> addon.manifestUrl == removalUrl }?.let { addon ->
        RemoveAddonDialog(
            addon = addon,
            onRemove = {
                focusAfterRemoval = FocusAfterRemoval(removed = addon.manifestUrl, next = neighbourAfterRemoval(state.addons, addon.manifestUrl))
                viewModel.remove(addon)
                removalUrl = null
            },
            onDismiss = { removalUrl = null }
        )
    }

    // After a removal the focused row is gone: focus its neighbour, or the panel when none is left.
    LaunchedEffect(state.addons, focusAfterRemoval) {
        val pending = focusAfterRemoval ?: return@LaunchedEffect
        if (state.addons.any { addon -> addon.manifestUrl == pending.removed }) return@LaunchedEffect
        withFrameNanos {}
        val target = pending.next?.let(rowFocus::get) ?: if (showsPhonePanel) phoneCloseFocus else fieldFocus
        target.requestFocus()
        focusAfterRemoval = null
    }
    LaunchedEffect(phoneButtonFocusTick) {
        if (phoneButtonFocusTick == 0) return@LaunchedEffect
        withFrameNanos {}
        phoneButtonFocus.requestFocus()
    }
}

private data class FocusAfterRemoval(val removed: String, val next: String?)

@Composable
private fun InstalledList(
    addons: List<TvosAddon>,
    firstRowFocus: FocusRequester,
    rowFocus: MutableMap<String, FocusRequester>,
    onChoose: (TvosAddon) -> Unit
) {
    val urls = addons.map(TvosAddon::manifestUrl).toSet()
    rowFocus.keys.retainAll(urls)
    LazyColumn(
        modifier = Modifier.fillMaxSize().focusRestorer(firstRowFocus).testTag(ADDON_LIST_TAG),
        contentPadding = PaddingValues(bottom = TvInsets.verticalMargin),
        verticalArrangement = Arrangement.spacedBy(ROW_GAP)
    ) {
        item(key = "header", contentType = "header") {
            SectionTitle("Installed", spacingAfter = ROW_GAP)
        }
        items(addons, key = TvosAddon::manifestUrl, contentType = { "addon" }) { addon ->
            val requester = rowFocus.getOrPut(addon.manifestUrl) { FocusRequester() }
            val first = if (addon.manifestUrl == addons.first().manifestUrl) Modifier.focusRequester(firstRowFocus) else Modifier
            AddonRow(
                addon = addon,
                onClick = { onChoose(addon) },
                modifier = Modifier.focusRequester(requester).then(first)
            )
        }
        item(key = "footer", contentType = "footer") {
            Text(text = FOOTER, style = TvType.caption2, color = TvColors.textSecondary, modifier = Modifier.padding(top = FOOTER_TOP))
        }
    }
}

/**
 * A section title on the shelf rhythm (spec: Callout bold, secondary, baseline 15 dp above what
 * follows), shared by both columns so they start on one line. [spacingAfter] is the spacing the
 * parent adds after it.
 */
@Composable
internal fun SectionTitle(text: String, spacingAfter: Dp, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = TvType.rowTitle,
        color = TvColors.textSecondary,
        maxLines = 1,
        modifier = modifier.paddingFromBaseline(top = SECTION_TITLE_BASELINE, bottom = SECTION_TITLE_TO_CONTENT - spacingAfter)
    )
}

/** The spec's empty state. Not focusable itself: the install panel holds focus beside it. */
@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = TvInsets.verticalMargin),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = EMPTY_TITLE, style = TvType.headline, textAlign = TextAlign.Center)
        Text(
            text = "Add a manifest URL to get catalogs and streams.",
            style = TvType.caption2,
            color = TvColors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.paddingFromBaseline(top = EMPTY_MESSAGE_BASELINE)
        )
    }
}

/** tvOS `confirmationDialog`: the addon's name, On or Off, and its actions. */
@Composable
private fun AddonActionDialog(
    addon: TvosAddon,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: () -> Unit,
    onMove: (AddonMove) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val then = { action: () -> Unit ->
        {
            onDismiss()
            action()
        }
    }
    AddonDialog(title = addon.name, message = if (addon.enabled) "On" else "Off", onDismiss = onDismiss) {
        AddonDialogButton(label = if (addon.enabled) "Turn Off" else "Turn On", onClick = then(onToggle), focusFirst = true)
        if (canMoveUp) AddonDialogButton(label = "Move Up", onClick = then { onMove(AddonMove.Up) })
        if (canMoveDown) AddonDialogButton(label = "Move Down", onClick = then { onMove(AddonMove.Down) })
        AddonDialogButton(label = "Remove", onClick = then(onRemove))
        AddonDialogButton(label = "Cancel", onClick = onDismiss)
    }
}

/** tvOS `alert`: Remove or Cancel; focus starts on Cancel, the safe answer. */
@Composable
private fun RemoveAddonDialog(addon: TvosAddon, onRemove: () -> Unit, onDismiss: () -> Unit) {
    AddonDialog(title = "Remove ${addon.name}?", message = REMOVAL_MESSAGE, onDismiss = onDismiss, width = REMOVE_DIALOG_WIDTH) {
        Row(horizontalArrangement = Arrangement.spacedBy(DIALOG_BUTTON_GAP)) {
            AddonDialogButton(label = "Cancel", onClick = onDismiss, focusFirst = true, modifier = Modifier.weight(1f))
            AddonDialogButton(label = "Remove", onClick = onRemove, modifier = Modifier.weight(1f))
        }
    }
}

const val ADDON_LIST_TAG = "addon-list"
const val EMPTY_TITLE = "No addons"

private const val FOOTER = "Select an addon to turn it off, move it or remove it."
private const val REMOVAL_MESSAGE = "Its catalogs, streams and subtitles leave the app."

/** The install panel is one of three grid columns; the list takes the other two. */
private val PANEL_WIDTH = TvGrid.itemWidth(columns = 3)
private val PANEL_GAP = TvGrid.gutter
private val ROW_GAP = 8.dp
private val FOOTER_TOP = 4.dp
private val DIALOG_BUTTON_GAP = 12.dp
private val REMOVE_DIALOG_WIDTH = 360.dp
private val SECTION_TITLE_BASELINE = 16.dp // cap height of 16 sp plus a little room
private val SECTION_TITLE_TO_CONTENT = 15.dp // spec: baseline 30 pt above the cards
private val EMPTY_MESSAGE_BASELINE = 37.dp // 61-library.png: baselines 42 dp apart
