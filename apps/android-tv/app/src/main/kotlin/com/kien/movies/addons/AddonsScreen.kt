package com.kien.movies.addons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.kien.movies.addons.phone.AddonQrSetup
import com.kien.movies.addons.phone.PhoneAddonProposal
import com.kien.movies.addons.phone.rememberAddonQrServer
import com.kien.movies.settings.SettingsMetrics
import com.kien.movies.settings.SettingsPage
import com.kien.movies.settings.SettingsSectionHeader
import com.kien.movies.settings.SettingsSplit
import com.kien.movies.shell.AppRoute
import com.kien.movies.shell.Router
import com.nuvio.tvos.TvosAddon

/**
 * Settings, Addons (tvOS AddonsView.swift), laid out like the Settings app: Add Addon and the
 * installed addons on the right, and on the left a preview of the focused row, the addon's logo and
 * summary, or the QR code while Add Addon has focus. Add Addon has first focus; a row opens the
 * addon's page, and Back lands on that row, or on its neighbour once the page removed it.
 */
@Composable
fun AddonsScreen(router: Router) {
    val viewModel = viewModel { AddonsViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val qr = rememberAddonQrServer(install = viewModel::installFromPhone)
    var manifestUrl by rememberSaveable { mutableStateOf("") }
    // The focused row, null for Add Addon; the preview follows it and Back returns to it.
    var focusedUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var lastOrder by rememberSaveable { mutableStateOf(state.addons.map(TvosAddon::manifestUrl)) }
    val addFocus = remember { FocusRequester() }
    val rowFocus = remember { mutableMapOf<String, FocusRequester>() }
    val urls = state.addons.map(TvosAddon::manifestUrl)

    LaunchedEffect(state.installStatus) {
        if (state.installStatus is AddonInstallStatus.Installed) manifestUrl = ""
    }
    // The page removed the focused addon: focus its neighbour in the order it had, else Add Addon.
    LaunchedEffect(urls) {
        val removed = focusedUrl?.takeIf { url -> url !in urls }
        if (removed != null) {
            focusedUrl = neighbourAfterRemoval(lastOrder, removed)?.takeIf { url -> url in urls }
            withFrameNanos {}
            (focusedUrl?.let(rowFocus::get) ?: addFocus).requestFocus()
        }
        lastOrder = urls
    }

    SettingsSplit(
        title = SettingsPage.Addons.title,
        preview = {
            val addon = state.addon(focusedUrl)
            if (addon != null) AddonSummary(addon) else AddonInstallPanel(qr.setup, state.installStatus, hasAddons = urls.isNotEmpty())
        },
        list = {
            val landing = focusedUrl?.takeIf { url -> url in urls }?.let { url -> rowFocus.getOrPut(url) { FocusRequester() } } ?: addFocus
            LazyColumn(
                modifier = Modifier.focusRestorer(landing).testTag(ADDON_LIST_TAG),
                contentPadding = PaddingValues(bottom = BOTTOM_ROOM),
                verticalArrangement = Arrangement.spacedBy(SettingsMetrics.rowGap)
            ) {
                item(key = ADD_KEY, contentType = ADD_KEY) {
                    AddonUrlField(
                        value = manifestUrl,
                        onValueChange = { manifestUrl = it },
                        onSubmit = { viewModel.install(manifestUrl) },
                        focusRequester = addFocus,
                        modifier = Modifier.onFocusChanged { focus -> if (focus.hasFocus) focusedUrl = null }
                    )
                }
                if (state.addons.isNotEmpty()) {
                    item(key = HEADER_KEY, contentType = HEADER_KEY) { SettingsSectionHeader(INSTALLED) }
                }
                items(state.addons, key = TvosAddon::manifestUrl, contentType = { "addon" }) { addon ->
                    AddonRow(
                        addon = addon,
                        onClick = { router.open(AppRoute.Addon(addon.manifestUrl)) },
                        modifier = Modifier
                            .focusRequester(rowFocus.getOrPut(addon.manifestUrl) { FocusRequester() })
                            .onFocusChanged { focus -> if (focus.hasFocus) focusedUrl = addon.manifestUrl }
                    )
                }
            }
        }
    )

    (qr.setup as? AddonQrSetup.Running)?.let { running -> PhoneAddonProposal(running.server) }
}

const val ADDON_LIST_TAG = "addon-list"

private const val INSTALLED = "Installed"
private const val ADD_KEY = "add"
private const val HEADER_KEY = "installed"
private val BOTTOM_ROOM = 27.dp // the bottom safe margin, as on the language pages
