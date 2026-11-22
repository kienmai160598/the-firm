package com.kien.movies.ui.screens.addon

import com.kien.movies.ui.theme.MoviesPrimitives
import com.kien.movies.ui.theme.MoviesTheme

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusProperties
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.LocalTextStyle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kien.movies.domain.model.Addon
import com.kien.movies.domain.model.ExperienceMode
import com.kien.movies.ui.components.TvReorderList
import com.kien.movies.ui.screens.settings.SettingsVerticalScrollIndicators
import com.kien.movies.ui.components.TrackingAddonConsentDialog
import com.kien.movies.ui.components.LoadingIndicator
import com.kien.movies.ui.components.MoviesDialog
import com.kien.movies.ui.components.tvReorderDisabledFocusBorder
import com.kien.movies.ui.util.contentTextDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.TextStyle
import com.kien.movies.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import com.kien.movies.ui.components.expressive.M3eShapes
import com.kien.movies.ui.components.expressive.MoviesButton
import com.kien.movies.ui.components.expressive.MoviesButtonStyle
import com.kien.movies.ui.components.expressive.MoviesCardFocus
import com.kien.movies.ui.components.expressive.MoviesSurface
import com.kien.movies.ui.components.expressive.MoviesSwitch
import com.kien.movies.ui.components.expressive.moviesButtonColors
import com.kien.movies.ui.components.expressive.moviesButtonScale
import com.kien.movies.ui.components.expressive.moviesButtonShape
import com.kien.movies.ui.screens.settings.buttonIconSize
import com.kien.movies.ui.screens.settings.TenFootTypography
import com.kien.movies.ui.theme.ExpressiveColors
import com.kien.movies.ui.theme.tvControlHeight
import com.kien.movies.ui.theme.bodyFloor
import com.kien.movies.ui.theme.ExpressiveMotion
import com.kien.movies.ui.theme.ExpressiveFocus
import androidx.tv.material3.ButtonBorder

private sealed interface AddonExperienceModeState {
    data object Loading : AddonExperienceModeState
    data class Loaded(val mode: ExperienceMode?) : AddonExperienceModeState
}

private enum class AddonManagerFocusTarget {
    CatalogOrder,
    Collections
}

private sealed interface AddonDeleteFocusRestore {
    data class Cancel(val addonUrl: String) : AddonDeleteFocusRestore
    data class AfterDelete(
        val deletedUrl: String,
        val deletedIndex: Int,
        val targetAddonUrl: String?
    ) : AddonDeleteFocusRestore
}

internal fun resolvePostDeleteTargetAddonUrl(
    addonsBeforeDelete: List<Addon>,
    deletedUrl: String
): String? {
    val deletedIndex = addonsBeforeDelete.indexOfFirst { it.baseUrl == deletedUrl }
    return resolveSurvivingPostDeleteAddonUrl(
        addons = addonsBeforeDelete,
        deletedUrl = deletedUrl,
        preferredTargetUrl = null,
        deletedIndex = deletedIndex
    )
}

internal fun resolveSurvivingPostDeleteAddonUrl(
    addons: List<Addon>,
    deletedUrl: String,
    preferredTargetUrl: String?,
    deletedIndex: Int
): String? {
    val surviving = addons.filterNot { it.baseUrl == deletedUrl }
    if (surviving.isEmpty()) return null
    preferredTargetUrl
        ?.takeIf { target -> surviving.any { it.baseUrl == target } }
        ?.let { return it }
    val fallbackIndex = if (deletedIndex >= 0) {
        deletedIndex.coerceIn(0, surviving.lastIndex)
    } else {
        0
    }
    return surviving[fallbackIndex].baseUrl
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
internal fun Modifier.addonManagerFocusRestorer(
    pendingRequester: FocusRequester?,
    fallbackRequester: FocusRequester
): Modifier = this
    .then(
        if (pendingRequester != null) {
            Modifier.focusProperties {
                onEnter = { pendingRequester.requestFocus() }
            }
        } else {
            Modifier
        }
    )
    .focusRestorer(pendingRequester ?: fallbackRequester)

@Composable
fun AddonManagerScreen(
    viewModel: AddonManagerViewModel = hiltViewModel(),
    showBuiltInHeader: Boolean = true,
    onBackPress: () -> Unit = {},
    onNavigateToCatalogOrder: () -> Unit = {},
    onNavigateToCollections: () -> Unit = {}
) {
    TenFootTypography {
        AddonManagerScreenBody(
            viewModel = viewModel,
            showBuiltInHeader = showBuiltInHeader,
            onBackPress = onBackPress,
            onNavigateToCatalogOrder = onNavigateToCatalogOrder,
            onNavigateToCollections = onNavigateToCollections
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun AddonManagerScreenBody(
    viewModel: AddonManagerViewModel = hiltViewModel(),
    showBuiltInHeader: Boolean = true,
    onBackPress: () -> Unit = {},
    onNavigateToCatalogOrder: () -> Unit = {},
    onNavigateToCollections: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val addonToggleFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val addonRemoveFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    fun toggleFocusRequesterFor(baseUrl: String): FocusRequester =
        addonToggleFocusRequesters.getOrPut(baseUrl) { FocusRequester() }
    fun removeFocusRequesterFor(baseUrl: String): FocusRequester =
        addonRemoveFocusRequesters.getOrPut(baseUrl) { FocusRequester() }
    val refreshAddonsFocusRequester = remember { FocusRequester() }
    val catalogOrderFocusRequester = remember { FocusRequester() }
    val collectionsFocusRequester = remember { FocusRequester() }
    var pendingFocusRestore by rememberSaveable { mutableStateOf<AddonManagerFocusTarget?>(null) }
    val experienceModeState by remember(viewModel) {
        viewModel.experienceMode.map<ExperienceMode?, AddonExperienceModeState> {
            AddonExperienceModeState.Loaded(it)
        }
    }.collectAsState(initial = AddonExperienceModeState.Loading)
    val experienceMode = (experienceModeState as? AddonExperienceModeState.Loaded)?.mode
    if (experienceModeState is AddonExperienceModeState.Loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MoviesTheme.colors.Background),
            contentAlignment = Alignment.Center
        ) {
            LoadingIndicator()
        }
        return
    }
    val isEssential = experienceMode == ExperienceMode.ESSENTIAL
    val webConfigMode = viewModel.webConfigMode(experienceMode ?: ExperienceMode.ADVANCED)
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val surfaceFocusRequester = remember { FocusRequester() }
    val manageFromPhoneFocusRequester = remember { FocusRequester() }
    val installButtonFocusRequester = remember { FocusRequester() }
    val textFieldFocusRequester = remember { FocusRequester() }
    val deleteDialogFocusRequester = remember { FocusRequester() }
    var isEditing by remember { mutableStateOf(false) }
    var addonUrlPendingDeletion by remember { mutableStateOf<String?>(null) }
    var pendingDeleteFocusRestore by remember { mutableStateOf<AddonDeleteFocusRestore?>(null) }
    var restoreManageFromPhoneFocus by remember { mutableStateOf(false) }
    var hasHandledInitialFocusState by remember { mutableStateOf(false) }
    val latestUiState by rememberUpdatedState(uiState)
    val latestIsEditing by rememberUpdatedState(isEditing)
    val latestPendingFocusRestore by rememberUpdatedState(pendingFocusRestore)
    val hasHomeVisibleCatalogs = remember(uiState.installedAddons) {
        uiState.installedAddons.any { addon ->
            addon.enabled && addon.catalogs.any { catalog -> !catalog.isSearchOnlyCatalog() }
        }
    }
    val manageFromPhoneSubtitle = if (webConfigMode == com.kien.movies.core.server.AddonWebConfigMode.COLLECTIONS_ONLY) {
        stringResource(R.string.addon_manage_collections_from_phone_subtitle)
    } else {
        stringResource(R.string.addon_manage_from_phone_subtitle)
    }
    val qrInstruction = if (webConfigMode == com.kien.movies.core.server.AddonWebConfigMode.COLLECTIONS_ONLY) {
        stringResource(R.string.addon_qr_collections_scan_instruction)
    } else {
        stringResource(R.string.addon_qr_scan_instruction)
    }

    BackHandler { onBackPress() }

    val defaultRefreshAddonsSubtitle = stringResource(R.string.addon_refresh_default_subtitle)
    val refreshedAddonsSubtitle = stringResource(R.string.addon_refresh_done_subtitle)
    var refreshAddonsSubtitle by remember(defaultRefreshAddonsSubtitle) {
        mutableStateOf(defaultRefreshAddonsSubtitle)
    }

    LaunchedEffect(refreshAddonsSubtitle) {
        if (refreshAddonsSubtitle != defaultRefreshAddonsSubtitle) {
            delay(5_000)
            refreshAddonsSubtitle = defaultRefreshAddonsSubtitle
        }
    }

    // When isEditing changes to true, focus the text field and show keyboard
    LaunchedEffect(isEditing) {
        if (isEditing) {
            textFieldFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val requestInputBarFocus = {
        coroutineScope.launch {
            repeat(2) { withFrameNanos { } }
            if (pendingFocusRestore == null &&
                pendingDeleteFocusRestore == null &&
                !uiState.isQrModeActive &&
                uiState.pendingChange == null &&
                !isEditing &&
                addonUrlPendingDeletion == null
            ) {
                runCatching { surfaceFocusRequester.requestFocus() }
            }
        }
    }

    val requestManageFromPhoneFocus = {
        coroutineScope.launch {
            repeat(2) { withFrameNanos { } }
            if (pendingFocusRestore == null &&
                pendingDeleteFocusRestore == null &&
                restoreManageFromPhoneFocus &&
                !uiState.isQrModeActive &&
                uiState.pendingChange == null &&
                !isEditing &&
                addonUrlPendingDeletion == null
            ) {
                runCatching { manageFromPhoneFocusRequester.requestFocus() }
            }
        }
    }

    LaunchedEffect(uiState.isQrModeActive, uiState.pendingChange) {
        if (uiState.isQrModeActive) {
            restoreManageFromPhoneFocus = true
        }
        if (!hasHandledInitialFocusState) {
            hasHandledInitialFocusState = true
            return@LaunchedEffect
        }
        if (pendingFocusRestore == null &&
            pendingDeleteFocusRestore == null &&
            !uiState.isQrModeActive &&
            uiState.pendingChange == null &&
            !isEditing &&
            addonUrlPendingDeletion == null
        ) {
            if (restoreManageFromPhoneFocus) {
                requestManageFromPhoneFocus()
            } else {
                requestInputBarFocus()
            }
        }
    }

    LaunchedEffect(pendingDeleteFocusRestore, addonUrlPendingDeletion) {
        val restore = pendingDeleteFocusRestore ?: return@LaunchedEffect
        if (addonUrlPendingDeletion != null) return@LaunchedEffect
        if (restore is AddonDeleteFocusRestore.AfterDelete) {
            withTimeoutOrNull(600) {
                snapshotFlow { latestUiState.installedAddons }
                    .first { addons -> addons.none { it.baseUrl == restore.deletedUrl } }
            }
        }
        repeat(2) { withFrameNanos { } }
        val currentState = latestUiState
        if (addonUrlPendingDeletion == null &&
            !currentState.isQrModeActive &&
            currentState.pendingChange == null &&
            !latestIsEditing &&
            latestPendingFocusRestore == null
        ) {
            val addons = currentState.installedAddons
            val targetRequester = when (restore) {
                is AddonDeleteFocusRestore.Cancel -> {
                    if (addons.any { it.baseUrl == restore.addonUrl }) {
                        removeFocusRequesterFor(restore.addonUrl)
                    } else {
                        addons.firstOrNull()?.baseUrl?.let(::toggleFocusRequesterFor)
                            ?: refreshAddonsFocusRequester
                    }
                }
                is AddonDeleteFocusRestore.AfterDelete -> {
                    if (addons.any { it.baseUrl == restore.deletedUrl }) {
                        removeFocusRequesterFor(restore.deletedUrl)
                    } else {
                        resolveSurvivingPostDeleteAddonUrl(
                            addons = addons,
                            deletedUrl = restore.deletedUrl,
                            preferredTargetUrl = restore.targetAddonUrl,
                            deletedIndex = restore.deletedIndex
                        )?.let(::toggleFocusRequesterFor)
                            ?: refreshAddonsFocusRequester
                    }
                }
            }
            runCatching { targetRequester.requestFocus() }
        }
        if (pendingDeleteFocusRestore == restore) {
            pendingDeleteFocusRestore = null
        }
    }

    // Claim focus as soon as the list is laid out so the default initial focus
    // (URL field / Manage) never flashes before ON_RESUME restores it.
    LaunchedEffect(Unit) {
        val earlyFocusRequester = when (pendingFocusRestore) {
            AddonManagerFocusTarget.CatalogOrder -> catalogOrderFocusRequester
            AddonManagerFocusTarget.Collections -> collectionsFocusRequester
            null -> return@LaunchedEffect
        }
        repeat(2) { withFrameNanos { } }
        runCatching { earlyFocusRequester.requestFocus() }
    }

    LaunchedEffect(addonUrlPendingDeletion) {
        if (addonUrlPendingDeletion != null) {
            repeat(2) { withFrameNanos { } }
            runCatching { deleteDialogFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(uiState.transientMessage) {
        if (uiState.transientMessage != null) {
            delay(3200)
            viewModel.clearTransientMessage()
        }
    }

    DisposableEffect(
        lifecycleOwner,
        uiState.isQrModeActive,
        uiState.pendingChange,
        addonUrlPendingDeletion,
        pendingFocusRestore
    ) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val focusRequester = when (pendingFocusRestore) {
                    AddonManagerFocusTarget.CatalogOrder -> catalogOrderFocusRequester
                    AddonManagerFocusTarget.Collections -> collectionsFocusRequester
                    else -> null
                }
                if (focusRequester != null) {
                    coroutineScope.launch {
                        repeat(2) { withFrameNanos { } }
                        if (focusRequester.requestFocus()) {
                            pendingFocusRestore = null
                        }
                    }
                } else if (
                    restoreManageFromPhoneFocus &&
                    !uiState.isQrModeActive &&
                    uiState.pendingChange == null &&
                    !isEditing &&
                    addonUrlPendingDeletion == null
                ) {
                    requestManageFromPhoneFocus()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopQrMode() }
    }

    val deleteRestoreFallback = when (val restore = pendingDeleteFocusRestore) {
        is AddonDeleteFocusRestore.Cancel ->
            restore.addonUrl
                .takeIf { url -> uiState.installedAddons.any { it.baseUrl == url } }
                ?.let(::removeFocusRequesterFor)
        is AddonDeleteFocusRestore.AfterDelete ->
            resolveSurvivingPostDeleteAddonUrl(
                addons = uiState.installedAddons,
                deletedUrl = restore.deletedUrl,
                preferredTargetUrl = restore.targetAddonUrl,
                deletedIndex = restore.deletedIndex
            )?.let(::toggleFocusRequesterFor)
                ?: refreshAddonsFocusRequester
        null -> null
    }

    val addonOrderScrollState = rememberScrollState()
    val addonOrderMovingIndex = uiState.installedAddons.indexOfFirst { it.baseUrl == uiState.movingAddonKey }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MoviesTheme.colors.Background)
    ) {
        TvReorderList(
            items = uiState.installedAddons,
            key = { it.baseUrl },
            canMove = {
                !viewModel.isReadOnly &&
                    !isEssential &&
                    uiState.installedAddons.size > 1 &&
                    uiState.movingAddonKey == null
            },
            movingKey = uiState.movingAddonKey,
            onStartMove = viewModel::enterAddonMoveMode,
            onMoveBy = viewModel::moveAddonDuringDrag,
            onConfirmMove = { viewModel.confirmAddonMove() },
            onCancelMove = viewModel::cancelAddonMove,
            modifier = Modifier
                .fillMaxSize()
                .addonManagerFocusRestorer(
                    pendingRequester = when (pendingFocusRestore) {
                        AddonManagerFocusTarget.CatalogOrder -> catalogOrderFocusRequester
                        AddonManagerFocusTarget.Collections -> collectionsFocusRequester
                        null -> null
                    },
                    fallbackRequester = deleteRestoreFallback ?: surfaceFocusRequester
                ),
            scrollState = addonOrderScrollState,
            contentPadding = PaddingValues(horizontal = 36.dp, vertical = 28.dp),
            header = {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(
                text = stringResource(R.string.addon_title),
                style = MaterialTheme.typography.headlineMedium,
                color = if (showBuiltInHeader) MoviesTheme.colors.TextPrimary else Color.Transparent
            )

            if (viewModel.isReadOnly) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MoviesTheme.colors.FocusBackground
                        ),
                        shape = M3eShapes.panel
                    ) {
                        Text(
                            text = stringResource(R.string.addon_readonly_notice),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MoviesTheme.colors.TextSecondary,
                            modifier = androidx.compose.ui.Modifier.padding(MoviesTheme.spacing.lg)
                        )
                    }
            }

            if (!viewModel.isReadOnly) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        colors = CardDefaults.cardColors(containerColor = MoviesTheme.colors.BackgroundCard),
                        shape = M3eShapes.panel
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = stringResource(R.string.addon_install_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, textDirection = TextDirection.Content),
                                color = MoviesTheme.colors.TextPrimary,
                            )
                            Spacer(modifier = Modifier.height(MoviesTheme.spacing.md))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Surface always stays in the tree for stable D-pad focus
                                MoviesSurface(
                                    onClick = { isEditing = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(surfaceFocusRequester)
                                ) {
                                    Box(modifier = Modifier.padding(MoviesTheme.spacing.md)) {
                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                            BasicTextField(
                                                value = uiState.installUrl,
                                                onValueChange = viewModel::onInstallUrlChange,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .focusRequester(textFieldFocusRequester)
                                                    .onFocusChanged {
                                                        if (!it.isFocused && isEditing) {
                                                            isEditing = false
                                                            keyboardController?.hide()
                                                        }
                                                    },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = KeyboardType.Uri,
                                                    imeAction = ImeAction.Done
                                                ),
                                                keyboardActions = KeyboardActions(
                                                    onDone = {
                                                        viewModel.installAddon()
                                                        isEditing = false
                                                        keyboardController?.hide()
                                                        installButtonFocusRequester.requestFocus()
                                                    }
                                                ),
                                                textStyle = MaterialTheme.typography.bodyMedium
                                                    .bodyFloor()
                                                    .copy(
                                                    color = LocalContentColor.current,
                                                textDirection = TextDirection.Content
                                                ),
                                                cursorBrush = SolidColor(if (isEditing) MoviesTheme.colors.Primary else Color.Transparent),
                                                decorationBox = { innerTextField ->
                                                    if (uiState.installUrl.isEmpty()) {
                                                        Text(
                                                            text = stringResource(R.string.addon_install_placeholder),
                                                            style = MaterialTheme.typography
                                                                .bodyMedium.bodyFloor(),
                                                            color = LocalContentColor.current.copy(alpha = 0.64f)
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                            )
                                        }
                                    }
                                }

                                MoviesButton(
                                    onClick = {
                                        viewModel.installAddon()
                                        isEditing = false
                                        keyboardController?.hide()
                                        installButtonFocusRequester.requestFocus()
                                    },
                                    enabled = !uiState.isInstalling,
                                    modifier = Modifier
                                        .focusRequester(installButtonFocusRequester)
                                        .semantics { role = Role.Button },
                                    style = MoviesButtonStyle.Primary
                                ) {
                                    Text(text = if (uiState.isInstalling) stringResource(R.string.addon_installing) else stringResource(R.string.addon_install_btn))
                                }
                            }

                            AnimatedVisibility(visible = uiState.error != null) {
                                Text(
                                    text = uiState.error.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MoviesTheme.colors.Error,
                                    modifier = Modifier.padding(top = 10.dp)
                                )
                            }
                        }
                    }

            }

                    ManageFromPhoneCard(
                    subtitle = manageFromPhoneSubtitle,
                    onClick = { viewModel.startQrMode(webConfigMode) },
                    focusRequester = manageFromPhoneFocusRequester,
                    onFocusChanged = { isFocused ->
                        if (
                            !isFocused &&
                            !uiState.isQrModeActive &&
                            uiState.pendingChange == null
                        ) {
                            restoreManageFromPhoneFocus = false
                        }
                    }
                )

            if (!viewModel.isReadOnly && !isEssential && hasHomeVisibleCatalogs) {
                    CatalogOrderEntryCard(
                        onClick = {
                            restoreManageFromPhoneFocus = false
                            pendingFocusRestore = AddonManagerFocusTarget.CatalogOrder
                            onNavigateToCatalogOrder()
                        },
                        focusRequester = catalogOrderFocusRequester
                    )
            }

            if (!isEssential) {
                    CollectionsEntryCard(
                        onClick = {
                            restoreManageFromPhoneFocus = false
                            pendingFocusRestore = AddonManagerFocusTarget.Collections
                            onNavigateToCollections()
                        },
                        focusRequester = collectionsFocusRequester
                    )
            }

                    val firstAddonToggleRequester = if (!viewModel.isReadOnly) {
                        uiState.installedAddons.firstOrNull()?.baseUrl?.let(::toggleFocusRequesterFor)
                    } else {
                        null
                    }
                    RefreshAddonsEntryCard(
                        subtitle = refreshAddonsSubtitle,
                        onClick = {
                            viewModel.requestAddonSyncNow()
                            refreshAddonsSubtitle = refreshedAddonsSubtitle
                        },
                        modifier = Modifier
                            .focusRequester(refreshAddonsFocusRequester)
                            .then(
                                if (firstAddonToggleRequester != null) {
                                    Modifier.focusProperties {
                                        down = firstAddonToggleRequester
                                    }
                                } else {
                                    Modifier
                                }
                            )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.addon_installed_section),
                            style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Content),
                            color = MoviesTheme.colors.TextPrimary,
                        )
                        Spacer(modifier = Modifier.width(MoviesTheme.spacing.md))
                        if (uiState.isLoading && uiState.installedAddons.isEmpty()) {
                            LoadingIndicator(modifier = Modifier.height(MoviesTheme.spacing.xl))
                        }
                    }

                    if (uiState.installedAddons.isEmpty() && !uiState.isLoading) {
                        Text(
                            text = stringResource(R.string.addon_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MoviesTheme.colors.TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            },
        ) { addon, isMoving, onStartMove, onConfirmMove ->
            AddonCard(
                addon = addon,
                isMoving = isMoving,
                canMove = !isEssential && uiState.installedAddons.size > 1,
                actionsEnabled = uiState.movingAddonKey == null || isMoving,
                onStartMove = onStartMove,
                onConfirmMove = onConfirmMove,
                onRemove = {
                    pendingDeleteFocusRestore = null
                    addonUrlPendingDeletion = addon.baseUrl
                },
                onEnabledChange = { enabled -> viewModel.setAddonEnabled(addon.baseUrl, enabled) },
                isReadOnly = viewModel.isReadOnly,
                showReorder = !isEssential,
                toggleFocusRequester = if (viewModel.isReadOnly) null else toggleFocusRequesterFor(addon.baseUrl),
                removeFocusRequester = if (viewModel.isReadOnly) null else removeFocusRequesterFor(addon.baseUrl)
            )
        }

        SettingsVerticalScrollIndicators(
            state = addonOrderScrollState,
            suppressBackward = addonOrderMovingIndex == 0,
            suppressForward = addonOrderMovingIndex >= 0 &&
                addonOrderMovingIndex == uiState.installedAddons.lastIndex,
            forceBackward = addonOrderMovingIndex > 0,
            forceForward = addonOrderMovingIndex in 0 until uiState.installedAddons.lastIndex
        )

        if (uiState.isQrModeActive || uiState.pendingChange != null) {
            if (uiState.isQrModeActive) {
                QrCodeOverlay(
                    qrBitmap = uiState.qrCodeBitmap,
                    serverUrl = uiState.serverUrl,
                    instruction = qrInstruction,
                    onClose = viewModel::stopQrMode,
                    hasPendingChange = uiState.pendingChange != null
                )
            }

            uiState.pendingChange?.let { pending ->
                ConfirmAddonChangesDialog(
                    pendingChange = pending,
                    onConfirm = { viewModel.confirmPendingChange() },
                    onReject = viewModel::rejectPendingChange
                )
                if (pending.awaitingTrackingConsent) {
                    TrackingAddonConsentDialog(
                        addonName = pending.trackingAddonNames.orEmpty().joinToString(", "),
                        onInstall = { viewModel.confirmPendingChange(trackingConsentGiven = true) },
                        onDismiss = viewModel::dismissPendingTrackingConsent
                    )
                }
            }
        }

        addonUrlPendingDeletion?.let { addonUrl ->
            val dismissDeleteDialog = {
                addonUrlPendingDeletion = null
                pendingDeleteFocusRestore = AddonDeleteFocusRestore.Cancel(addonUrl)
            }
            MoviesDialog(
                onDismiss = dismissDeleteDialog,
                title = stringResource(R.string.addon_delete_confirm_title),
                subtitle = stringResource(R.string.addon_delete_confirm_message),
                suppressFirstKeyUp = false
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.md, Alignment.End)
                ) {
                    MoviesButton(
                        onClick = dismissDeleteDialog,
                        modifier = Modifier
                            .focusRequester(deleteDialogFocusRequester)
                            .semantics { role = Role.Button }
                    ) {
                        Text(stringResource(R.string.addon_delete_no))
                    }
                    MoviesButton(
                        onClick = {
                            val currentAddons = uiState.installedAddons
                            val deletedIndex = currentAddons.indexOfFirst { it.baseUrl == addonUrl }
                            val targetAddonUrl = resolvePostDeleteTargetAddonUrl(currentAddons, addonUrl)
                            pendingDeleteFocusRestore = AddonDeleteFocusRestore.AfterDelete(
                                deletedUrl = addonUrl,
                                deletedIndex = deletedIndex,
                                targetAddonUrl = targetAddonUrl
                            )
                            viewModel.removeAddon(addonUrl)
                            addonUrlPendingDeletion = null
                        },
                        modifier = Modifier.semantics { role = Role.Button },
                        style = MoviesButtonStyle.Destructive
                    ) {
                        Text(stringResource(R.string.addon_delete_yes))
                    }
                }
            }
        }

        AddonMessageOverlay(
            message = uiState.transientMessage,
            isError = uiState.transientMessageIsError
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun AddonMessageOverlay(
    message: String?,
    isError: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(MoviesTheme.spacing.xl),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val visibleMessage = message ?: return@AnimatedVisibility
            Surface(
                onClick = { },
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (isError) {
                        MoviesPrimitives.red600.copy(alpha = 0.92f)
                    } else {
                        MoviesPrimitives.green700.copy(alpha = 0.92f)
                    }
                ),
                shape = ClickableSurfaceDefaults.shape(M3eShapes.panel)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = MoviesTheme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.md)
                ) {
                    Icon(
                        imageVector = if (isError) Icons.Default.Close else Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(
                        text = visibleMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ManageFromPhoneCard(
    subtitle: String,
    onClick: () -> Unit,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    MoviesSurface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged {
                isFocused = it.isFocused
                onFocusChanged(it.isFocused)
            }
            .semantics { role = Role.Button }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = LocalContentColor.current.copy(alpha = if (isFocused) 1f else 0.64f)
                )
                Spacer(modifier = Modifier.width(MoviesTheme.spacing.lg))
                Column {
                    Text(
                        text = stringResource(R.string.addon_manage_from_phone_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, textDirection = TextDirection.Content),
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalContentColor.current.copy(alpha = 0.64f)
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.PhoneAndroid,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = LocalContentColor.current.copy(alpha = 0.64f)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CatalogOrderEntryCard(
    onClick: () -> Unit,
    focusRequester: FocusRequester
) {
    var isFocused by remember { mutableStateOf(false) }

    MoviesSurface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .semantics { role = Role.Button }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Reorder,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = LocalContentColor.current.copy(alpha = if (isFocused) 1f else 0.64f)
                )
                Spacer(modifier = Modifier.width(MoviesTheme.spacing.lg))
                Column {
                    Text(
                        text = stringResource(R.string.addon_reorder_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, textDirection = TextDirection.Content),
                    )
                    Text(
                        text = stringResource(R.string.addon_reorder_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalContentColor.current.copy(alpha = 0.64f)
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = LocalContentColor.current.copy(alpha = 0.64f)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CollectionsEntryCard(
    onClick: () -> Unit,
    focusRequester: FocusRequester
) {
    var isFocused by remember { mutableStateOf(false) }

    MoviesSurface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .semantics { role = Role.Button }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = LocalContentColor.current.copy(alpha = if (isFocused) 1f else 0.64f)
                )
                Spacer(modifier = Modifier.width(MoviesTheme.spacing.lg))
                Column {
                    Text(
                        text = stringResource(R.string.collections_card_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, textDirection = TextDirection.Content),
                    )
                    Text(
                        text = stringResource(R.string.collections_card_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalContentColor.current.copy(alpha = 0.64f)
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = LocalContentColor.current.copy(alpha = 0.64f)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RefreshAddonsEntryCard(
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    MoviesSurface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .semantics { role = Role.Button }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = LocalContentColor.current.copy(alpha = if (isFocused) 1f else 0.64f)
                )
                Spacer(modifier = Modifier.width(MoviesTheme.spacing.lg))
                Column {
                    Text(
                        text = stringResource(R.string.addon_refresh_action),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, textDirection = TextDirection.Content),
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalContentColor.current.copy(alpha = 0.64f)
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = LocalContentColor.current.copy(alpha = 0.64f)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun QrCodeOverlay(
    qrBitmap: Bitmap?,
    serverUrl: String?,
    instruction: String,
    onClose: () -> Unit,
    hasPendingChange: Boolean = false,
    qrSize: Dp = 220.dp,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(hasPendingChange) {
        if (!hasPendingChange) {
            focusRequester.requestFocus()
        }
    }

    BackHandler { onClose() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = instruction,
                style = MaterialTheme.typography.bodyMedium,
                color = MoviesTheme.colors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(MoviesTheme.spacing.lg))

            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.cd_qr_code),
                    modifier = Modifier.size(qrSize),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(MoviesTheme.spacing.md))

            if (serverUrl != null) {
                Text(
                    text = serverUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MoviesTheme.colors.TextTertiary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(MoviesTheme.spacing.xl))

            MoviesSurface(
                onClick = onClose,
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .semantics { role = Role.Button }.tvControlHeight(),
                shape = M3eShapes.capsule
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = MoviesTheme.spacing.xl, vertical = MoviesTheme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(buttonIconSize(18.dp))
                    )
                    Spacer(modifier = Modifier.width(MoviesTheme.spacing.sm))
                    Text(
                        text = stringResource(R.string.addon_qr_close),
                        style = LocalTextStyle.current.copy(textDirection = TextDirection.Content),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun ConfirmAddonChangesDialog(
    pendingChange: PendingChangeInfo,
    onConfirm: () -> Unit,
    onReject: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    LaunchedEffect(pendingChange.isApplying, pendingChange.awaitingTrackingConsent) {
        if (!pendingChange.isApplying && !pendingChange.awaitingTrackingConsent) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    BackHandler { onReject() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = { },
            modifier = Modifier
                .width(560.dp)
                .heightIn(max = 640.dp),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = MoviesTheme.colors.SurfaceVariant
            ),
            shape = ClickableSurfaceDefaults.shape(M3eShapes.panel)
        ) {
            Column(
                modifier = Modifier.padding(MoviesTheme.spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.addon_confirm_title),
                    style = MaterialTheme.typography.headlineSmall.copy(textDirection = TextDirection.Content),
                    color = MoviesTheme.colors.TextPrimary,
                )

                Spacer(modifier = Modifier.height(MoviesTheme.spacing.lg))

                Text(
                    text = stringResource(R.string.addon_confirm_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MoviesTheme.colors.TextSecondary
                )

                Spacer(modifier = Modifier.height(MoviesTheme.spacing.md))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .background(
                            color = MoviesTheme.colors.Surface,
                            shape = M3eShapes.panel
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MoviesTheme.spacing.md)
                            .verticalScroll(scrollState)
                    ) {
                        if (pendingChange.addedUrls.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.addon_confirm_added),
                                style = MaterialTheme.typography.titleSmall,
                                color = MoviesTheme.colors.Success,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = MoviesTheme.spacing.xs)
                            )
                            pendingChange.addedUrls.forEach { url ->
                                val displayName = pendingChange.addedNames[url] ?: url
                                Text(
                                    text = "+ $displayName",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MoviesTheme.colors.Success,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = MoviesTheme.spacing.sm, bottom = MoviesTheme.spacing.xxs)
                                )
                            }
                            Spacer(modifier = Modifier.height(MoviesTheme.spacing.sm))
                        }

                        if (pendingChange.removedUrls.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.addon_confirm_removed),
                                style = MaterialTheme.typography.titleSmall,
                                color = MoviesTheme.colors.Error,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = MoviesTheme.spacing.xs)
                            )
                            pendingChange.removedUrls.forEach { url ->
                                val displayName = pendingChange.removedNames[url] ?: url
                                Text(
                                    text = "- $displayName",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MoviesTheme.colors.Error,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = MoviesTheme.spacing.sm, bottom = MoviesTheme.spacing.xxs)
                                )
                            }
                            Spacer(modifier = Modifier.height(MoviesTheme.spacing.sm))
                        }

                        if (pendingChange.catalogsReordered) {
                            Text(
                                text = stringResource(R.string.addon_confirm_catalog_reordered),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MoviesTheme.colors.TextSecondary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            )
                        }

                        if (pendingChange.disabledCatalogNames.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.addon_confirm_catalogs_disabled),
                                style = MaterialTheme.typography.titleSmall,
                                color = MoviesTheme.colors.Error,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = MoviesTheme.spacing.xs)
                            )
                            pendingChange.disabledCatalogNames.forEach { name ->
                                Text(
                                    text = "- $name",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MoviesTheme.colors.Error,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = MoviesTheme.spacing.sm, bottom = MoviesTheme.spacing.xxs)
                                )
                            }
                            Spacer(modifier = Modifier.height(MoviesTheme.spacing.sm))
                        }

                        if (pendingChange.enabledCatalogNames.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.addon_confirm_catalogs_enabled),
                                style = MaterialTheme.typography.titleSmall,
                                color = MoviesTheme.colors.Success,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = MoviesTheme.spacing.xs)
                            )
                            pendingChange.enabledCatalogNames.forEach { name ->
                                Text(
                                    text = "+ $name",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MoviesTheme.colors.Success,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = MoviesTheme.spacing.sm, bottom = MoviesTheme.spacing.xxs)
                                )
                            }
                            Spacer(modifier = Modifier.height(MoviesTheme.spacing.sm))
                        }

                        if (pendingChange.collectionsChanged) {
                            Text(
                                text = stringResource(R.string.addon_pending_collections_updated),
                                style = MaterialTheme.typography.titleSmall,
                                color = MoviesTheme.colors.TextPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = MoviesTheme.spacing.xs)
                            )
                            Text(
                                text = stringResource(R.string.addon_pending_collections_replace, pendingChange.proposedCollectionCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MoviesTheme.colors.TextSecondary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = MoviesTheme.spacing.sm, bottom = MoviesTheme.spacing.xxs)
                            )
                            Spacer(modifier = Modifier.height(MoviesTheme.spacing.sm))
                        }

                        if (
                            pendingChange.addedUrls.isEmpty() &&
                            pendingChange.removedUrls.isEmpty() &&
                            !pendingChange.catalogsReordered &&
                            pendingChange.disabledCatalogNames.isEmpty() &&
                            pendingChange.enabledCatalogNames.isEmpty() &&
                            !pendingChange.collectionsChanged
                        ) {
                            Text(
                                text = stringResource(R.string.addon_confirm_no_changes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MoviesTheme.colors.TextSecondary
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.addon_confirm_total_addons, pendingChange.proposedUrls.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MoviesTheme.colors.TextTertiary,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.addon_confirm_total_catalogs, pendingChange.proposedCatalogOrderKeys.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MoviesTheme.colors.TextTertiary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(MoviesTheme.spacing.xl))

                if (pendingChange.isApplying) {
                    LoadingIndicator(modifier = Modifier.size(36.dp))
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.lg)
                    ) {
                        MoviesSurface(
                            onClick = onReject,
                            modifier = Modifier.semantics { role = Role.Button }.tvControlHeight(),
                            shape = M3eShapes.capsule
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = MoviesTheme.spacing.xl, vertical = MoviesTheme.spacing.md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(buttonIconSize(18.dp))
                                )
                                Spacer(modifier = Modifier.width(MoviesTheme.spacing.sm))
                                Text(
                                    text = stringResource(R.string.addon_confirm_reject),
                                    style = LocalTextStyle.current.copy(textDirection = TextDirection.Content),
                                )
                            }
                        }

                        MoviesSurface(
                            onClick = onConfirm,
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .semantics { role = Role.Button }.tvControlHeight(),
                            shape = M3eShapes.capsule,
                            resting = ExpressiveColors.frostedStrong
                        ) {
                            Text(
                                text = stringResource(R.string.addon_confirm_confirm),
                                modifier = Modifier.padding(horizontal = MoviesTheme.spacing.xl, vertical = MoviesTheme.spacing.md)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun AddonCard(
    modifier: Modifier = Modifier,
    addon: Addon,
    isMoving: Boolean,
    canMove: Boolean,
    actionsEnabled: Boolean,
    onStartMove: () -> Unit,
    onConfirmMove: () -> Unit,
    onRemove: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    isReadOnly: Boolean = false,
    showReorder: Boolean = true,
    toggleFocusRequester: FocusRequester? = null,
    removeFocusRequester: FocusRequester? = null
) {
    if (isReadOnly) {
        MoviesSurface(
            onClick = { },
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(),
            focus = MoviesCardFocus.Frosted,
            shape = M3eShapes.panel,
            resting = MoviesTheme.colors.BackgroundCard
        ) {
            AddonCardContent(addon = addon, isReadOnly = true)
        }
    } else {
        val internalToggleFocusRequester = remember { FocusRequester() }
        val effectiveToggleFocusRequester = toggleFocusRequester ?: internalToggleFocusRequester
        val cardColor by animateColorAsState(
            targetValue = if (isMoving) {
                MoviesTheme.colors.FocusBackground
            } else {
                MoviesTheme.colors.BackgroundCard
            },
            animationSpec = ExpressiveMotion.defaultEffects(),
            label = "addonCardColor",
        )
        Card(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize()
                .focusProperties {
                    enter = { effectiveToggleFocusRequester }
                },
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = M3eShapes.panel
        ) {
            AddonCardContent(
                addon = addon,
                isReadOnly = false,
                isMoving = isMoving,
                canMove = canMove,
                actionsEnabled = actionsEnabled,
                onStartMove = onStartMove,
                onConfirmMove = onConfirmMove,
                onRemove = onRemove,
                onEnabledChange = onEnabledChange,
                showReorder = showReorder,
                toggleFocusRequester = effectiveToggleFocusRequester,
                removeFocusRequester = removeFocusRequester
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AddonCardContent(
    addon: Addon,
    isReadOnly: Boolean,
    isMoving: Boolean = false,
    canMove: Boolean = false,
    actionsEnabled: Boolean = true,
    onStartMove: () -> Unit = {},
    onConfirmMove: () -> Unit = {},
    onRemove: () -> Unit = {},
    onEnabledChange: (Boolean) -> Unit = {},
    showReorder: Boolean = true,
    toggleFocusRequester: FocusRequester? = null,
    removeFocusRequester: FocusRequester? = null
) {
    Column(modifier = Modifier.padding(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = addon.displayName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, textDirection = TextDirection.Content),
                    color = MoviesTheme.colors.TextPrimary,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (addon.version.isNotBlank()) {
                        Text(
                            text = "v${addon.version}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MoviesTheme.colors.TextSecondary
                        )
                    }
                    if (!addon.enabled) {
                        Text(
                            text = stringResource(R.string.addons_badge_disabled),
                            style = MaterialTheme.typography.labelSmall,
                            color = MoviesTheme.colors.TextSecondary
                        )
                    }
                }
            }
            if (!isReadOnly) {
                // The card carries the name as visible text, but the toggle is its own node, so
                // without this a screen reader hears only "switch, on".
                val toggleLabel = stringResource(R.string.cd_enable_addon, addon.displayName)
                var isToggleFocused by remember { mutableStateOf(false) }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MoviesSurface(
                        onClick = {
                            if (actionsEnabled && !isMoving) onEnabledChange(!addon.enabled)
                        },
                        modifier = Modifier
                            .focusRequester(toggleFocusRequester ?: remember { FocusRequester() })
                            .semantics {
                                role = Role.Switch
                                contentDescription = toggleLabel
                                toggleableState = if (addon.enabled) ToggleableState.On else ToggleableState.Off
                            }
                            .onFocusChanged { isToggleFocused = it.isFocused },
                        shape = M3eShapes.capsule,
                        resting = Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ListToggleSwitch(checked = addon.enabled, focused = isToggleFocused)
                        }
                    }
                    if (showReorder) {
                        Button(
                            onClick = if (isMoving) onConfirmMove else onStartMove,
                            enabled = isMoving || (canMove && actionsEnabled),
                            modifier = Modifier.semantics { role = Role.Button }.tvControlHeight(),
                            colors = moviesButtonColors(
                                if (isMoving) MoviesButtonStyle.Selected else MoviesButtonStyle.Secondary
                            ),
                            border = reorderButtonBorder(),
                            shape = moviesButtonShape,
                            scale = moviesButtonScale
                        ) {
                            Icon(
                                imageVector = Icons.Default.Reorder,
                                contentDescription = stringResource(R.string.cd_reorder)
                            )
                        }
                    }
                    val removeButtonModifier = if (removeFocusRequester != null) {
                        Modifier.focusRequester(removeFocusRequester)
                    } else {
                        Modifier
                    }
                    Button(
                        onClick = onRemove,
                        enabled = actionsEnabled && !isMoving,
                        modifier = removeButtonModifier
                            .semantics { role = Role.Button }
                            .tvControlHeight(),
                        colors = moviesButtonColors(MoviesButtonStyle.Destructive),
                        border = reorderButtonBorder(),
                        shape = moviesButtonShape,
                        scale = moviesButtonScale
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.cd_delete)
                        )
                    }
                }
            }
        }

        if (!addon.description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(MoviesTheme.spacing.sm))
            Text(
                text = addon.description ?: "",
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDirection = (addon.description ?: "").contentTextDirection()
                ),
                color = MoviesTheme.colors.TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(MoviesTheme.spacing.sm))
        Text(
            text = stringResource(R.string.addon_catalogs_types, addon.catalogs.size, addon.rawTypes.joinToString()),
            style = MaterialTheme.typography.bodySmall,
            color = MoviesTheme.colors.TextTertiary
        )
    }
}

/** A switch in a list row: the M3E kit switch, drawn inside the focusable row. */
@Composable
internal fun ListToggleSwitch(checked: Boolean, focused: Boolean, enabled: Boolean = true) {
    MoviesSwitch(checked = checked, enabled = enabled, focused = focused)
}

/**
 * A reorder or remove button keeps a faint ring while focused but disabled, the only cue that
 * focus sits on it; every other state draws no ring, as in the M3E kit.
 */
@Composable
internal fun reorderButtonBorder(): ButtonBorder {
    val shape = RoundedCornerShape(percent = ExpressiveFocus.PILL_PERCENT)
    return ButtonDefaults.border(
        border = Border.None,
        focusedBorder = Border.None,
        pressedBorder = Border.None,
        disabledBorder = Border.None,
        focusedDisabledBorder = tvReorderDisabledFocusBorder(shape)
    )
}
