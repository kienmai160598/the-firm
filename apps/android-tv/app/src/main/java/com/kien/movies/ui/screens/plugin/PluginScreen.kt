@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.screens.plugin

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.domain.model.LocalScraperResult
import com.kien.movies.domain.model.PluginRepository
import com.kien.movies.domain.model.ScraperInfo
import com.kien.movies.ui.components.LoadingIndicator
import com.kien.movies.ui.components.MoviesDialog
import com.kien.movies.ui.components.expressive.M3eShapes
import com.kien.movies.ui.components.expressive.MoviesButton
import com.kien.movies.ui.components.expressive.MoviesButtonStyle
import com.kien.movies.ui.components.expressive.MoviesCardFocus
import com.kien.movies.ui.components.expressive.MoviesSurface
import com.kien.movies.ui.components.expressive.MoviesSwitch
import com.kien.movies.ui.screens.settings.TenFootTypography
import com.kien.movies.ui.theme.ExpressiveColors
import com.kien.movies.ui.theme.ExpressiveMotion
import com.kien.movies.ui.theme.MoviesPrimitives
import com.kien.movies.ui.theme.MoviesTheme
import com.kien.movies.ui.theme.TvSizeFloors
import com.kien.movies.ui.theme.bodyFloor
import com.kien.movies.ui.theme.tvControlHeight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun PluginScreen(viewModel: PluginViewModel = hiltViewModel(), onBackPress: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler { onBackPress() }

    TenFootTypography {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MoviesTheme.colors.Background)
        ) {
            PluginScreenContent(
                uiState = uiState,
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun PluginScreenContent(
    uiState: PluginUiState = PluginUiState(),
    viewModel: PluginViewModel = hiltViewModel(),
    showHeader: Boolean = true
) {
    var repoUrl by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val manageFromPhoneFocusRequester = remember { FocusRequester() }
    val deleteDialogFocusRequester = remember { FocusRequester() }
    val pluginGroupingFocusRequester = remember { FocusRequester() }
    val repoDeleteFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    var repoPendingDeletion by remember { mutableStateOf<PluginRepository?>(null) }
    var repoRefocusRequester by remember { mutableStateOf<FocusRequester?>(null) }
    var hasEnteredQrMode by remember { mutableStateOf(false) }

    LaunchedEffect(repoPendingDeletion) {
        if (repoPendingDeletion != null) {
            repeat(2) { withFrameNanos { } }
            runCatching { deleteDialogFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(repoRefocusRequester) {
        val requester = repoRefocusRequester ?: return@LaunchedEffect
        repeat(2) { withFrameNanos { } }
        runCatching { requester.requestFocus() }
        repoRefocusRequester = null
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopQrMode() }
    }

    LaunchedEffect(uiState.isQrModeActive) {
        if (uiState.isQrModeActive) {
            hasEnteredQrMode = true
        } else if (hasEnteredQrMode && uiState.pendingRepoChange == null) {
            repeat(2) { withFrameNanos { } }
            manageFromPhoneFocusRequester.requestFocus()
            hasEnteredQrMode = false
        }
    }

    // Clear messages after delay
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            delay(3000)
            viewModel.onEvent(PluginUiEvent.ClearSuccess)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            delay(5000)
            viewModel.onEvent(PluginUiEvent.ClearError)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MoviesTheme.colors.Background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MoviesTheme.spacing.xxxl, vertical = MoviesTheme.spacing.xl)
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = MoviesTheme.spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.lg),
                modifier = Modifier
                    .fillMaxSize()
                    .focusRestorer(manageFromPhoneFocusRequester)
            ) {
                if (viewModel.isReadOnly) {
                    item {
                        androidx.compose.material3.Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = MoviesTheme.colors.FocusBackground
                            ),
                            shape = M3eShapes.card
                        ) {
                            androidx.tv.material3.Text(
                                text = stringResource(R.string.plugin_readonly_notice),
                                style = androidx.tv.material3.MaterialTheme.typography.bodyMedium,
                                color = com.kien.movies.ui.theme.MoviesTheme.colors.TextSecondary,
                                modifier = Modifier.padding(MoviesTheme.spacing.lg)
                            )
                        }
                    }
                }

                if (!viewModel.isReadOnly) {
                    item {
                        AddRepositoryInline(
                            url = repoUrl,
                            onUrlChange = { repoUrl = it },
                            onConfirm = {
                                if (repoUrl.isNotBlank()) {
                                    viewModel.onEvent(PluginUiEvent.AddRepository(repoUrl))
                                    repoUrl = ""
                                }
                            },
                            isLoading = uiState.isAddingRepo
                        )
                    }

                    // Manage from phone card
                    item {
                        ManageFromPhoneCard(
                            onClick = { viewModel.onEvent(PluginUiEvent.StartQrMode) },
                            focusRequester = manageFromPhoneFocusRequester
                        )
                    }
                }

                item {
                    PluginsEnabledCard(
                        pluginsEnabled = uiState.pluginsEnabled,
                        isReadOnly = viewModel.isReadOnly,
                        onPluginsEnabledChange = {
                            viewModel.onEvent(PluginUiEvent.SetPluginsEnabled(it))
                        }
                    )
                }

                item {
                    PluginStreamGroupingCard(
                        groupStreamsByRepository = uiState.groupStreamsByRepository,
                        isReadOnly = viewModel.isReadOnly,
                        focusRequester = pluginGroupingFocusRequester,
                        onGroupStreamsByRepositoryChange = {
                            viewModel.onEvent(PluginUiEvent.SetGroupStreamsByRepository(it))
                        }
                    )
                }

                // Repositories section
                item {
                    Text(
                        text = stringResource(
                            R.string.plugin_repositories_section,
                            uiState.repositories.size
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        color = MoviesTheme.colors.TextPrimary
                    )
                }

                if (uiState.repositories.isEmpty()) {
                    item {
                        EmptyState(
                            message = stringResource(R.string.plugin_no_repos),
                            modifier = Modifier.padding(vertical = MoviesTheme.spacing.xl)
                        )
                    }
                }

                items(uiState.repositories, key = { it.id }) { repo ->
                    val repoScrapers = uiState.scrapers.filter { it.repositoryId == repo.id }
                    RepositoryCard(
                        repository = repo,
                        repoScrapers = repoScrapers,
                        onRefresh = { viewModel.onEvent(PluginUiEvent.RefreshRepository(repo.id)) },
                        onRemove = { repoPendingDeletion = repo },
                        onToggleAll = { enabled ->
                            viewModel.onEvent(
                                PluginUiEvent.ToggleAllScrapersForRepo(repo.id, enabled)
                            )
                        },
                        removeFocusRequester = repoDeleteFocusRequesters.getOrPut(repo.id) {
                            FocusRequester()
                        },
                        isLoading = uiState.isLoading,
                        isReadOnly = viewModel.isReadOnly
                    )
                }

                // Scrapers section
                if (uiState.scrapers.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(MoviesTheme.spacing.lg))
                        Text(
                            text = stringResource(
                                R.string.plugin_providers_section,
                                uiState.scrapers.size
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            color = MoviesTheme.colors.TextPrimary
                        )
                    }

                    items(uiState.scrapers, key = { it.id }) { scraper ->
                        ScraperCard(
                            scraper = scraper,
                            onToggle = { enabled ->
                                viewModel.onEvent(PluginUiEvent.ToggleScraper(scraper.id, enabled))
                            },
                            onTest = { viewModel.onEvent(PluginUiEvent.TestScraper(scraper.id)) },
                            isTesting = uiState.isTesting && uiState.testScraperId == scraper.id,
                            testResults = if (uiState.testScraperId ==
                                scraper.id
                            ) {
                                uiState.testResults
                            } else {
                                null
                            },
                            testDiagnostics = if (uiState.testScraperId ==
                                scraper.id
                            ) {
                                uiState.testDiagnostics
                            } else {
                                null
                            },
                            isReadOnly = viewModel.isReadOnly
                        )
                    }
                }
            }
        }

        // Success/Error Messages
        MessageOverlay(
            successMessage = uiState.successMessage,
            errorMessage = uiState.errorMessage
        )

        if (uiState.isQrModeActive || uiState.pendingRepoChange != null ||
            uiState.pendingScraperEnable != null
        ) {
            if (uiState.isQrModeActive) {
                QrCodeOverlay(
                    qrBitmap = uiState.qrCodeBitmap,
                    serverUrl = uiState.serverUrl,
                    onClose = { viewModel.onEvent(PluginUiEvent.StopQrMode) },
                    hasPendingChange = uiState.pendingRepoChange != null
                )
            }

            uiState.pendingRepoChange?.let { pending ->
                ConfirmRepoChangesDialog(
                    pendingChange = pending,
                    onConfirm = { viewModel.onEvent(PluginUiEvent.ConfirmPendingRepoChange) },
                    onReject = { viewModel.onEvent(PluginUiEvent.RejectPendingRepoChange) }
                )
            }

            uiState.pendingScraperEnable?.let { pending ->
                ConfirmScraperEnableDialog(
                    scraperName = pending.scraperName,
                    onConfirm = { viewModel.onEvent(PluginUiEvent.ConfirmPendingScraperEnable) },
                    onDismiss = { viewModel.onEvent(PluginUiEvent.DismissPendingScraperEnable) }
                )
            }
        }

        repoPendingDeletion?.let { repo ->
            MoviesDialog(
                onDismiss = {
                    repoRefocusRequester = repoDeleteFocusRequesters[repo.id]
                    repoPendingDeletion = null
                },
                title = stringResource(R.string.plugin_repo_delete_confirm_title),
                subtitle = stringResource(R.string.plugin_repo_delete_confirm_message),
                suppressFirstKeyUp = false
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        MoviesTheme.spacing.md,
                        Alignment.End
                    )
                ) {
                    MoviesButton(
                        onClick = {
                            repoRefocusRequester = repoDeleteFocusRequesters[repo.id]
                            repoPendingDeletion = null
                        },
                        modifier = Modifier
                            .focusRequester(deleteDialogFocusRequester)
                            .semantics { role = Role.Button }
                    ) {
                        Text(stringResource(R.string.plugin_repo_delete_no))
                    }
                    MoviesButton(
                        onClick = {
                            val index = uiState.repositories.indexOfFirst { it.id == repo.id }
                            val neighborId = uiState.repositories.getOrNull(index + 1)?.id
                                ?: uiState.repositories.getOrNull(index - 1)?.id
                            repoRefocusRequester = neighborId?.let { repoDeleteFocusRequesters[it] }
                                ?: pluginGroupingFocusRequester
                            viewModel.onEvent(PluginUiEvent.RemoveRepository(repo.id))
                            repoPendingDeletion = null
                        },
                        modifier = Modifier.semantics { role = Role.Button },
                        style = MoviesButtonStyle.Destructive
                    ) {
                        Text(stringResource(R.string.plugin_repo_delete_yes))
                    }
                }
            }
        }
    }
}

@Composable
private fun PluginStreamGroupingCard(
    groupStreamsByRepository: Boolean,
    isReadOnly: Boolean,
    focusRequester: FocusRequester,
    onGroupStreamsByRepositoryChange: (Boolean) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    MoviesSurface(
        onClick = {
            if (!isReadOnly) {
                onGroupStreamsByRepositoryChange(!groupStreamsByRepository)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .semantics {
                role = Role.Switch
                toggleableState =
                    if (groupStreamsByRepository) ToggleableState.On else ToggleableState.Off
            }
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.plugin_group_by_repository_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(MoviesTheme.spacing.xs))
                Text(
                    text = stringResource(R.string.plugin_group_by_repository_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalContentColor.current.copy(alpha = 0.64f)
                )
            }

            Spacer(modifier = Modifier.width(MoviesTheme.spacing.lg))

            MoviesSwitch(checked = groupStreamsByRepository, enabled = true, focused = isFocused)
        }
    }
}

@Composable
private fun PluginsEnabledCard(
    pluginsEnabled: Boolean,
    isReadOnly: Boolean,
    onPluginsEnabledChange: (Boolean) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    MoviesSurface(
        onClick = {
            if (!isReadOnly) {
                onPluginsEnabledChange(!pluginsEnabled)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Switch
                toggleableState = if (pluginsEnabled) ToggleableState.On else ToggleableState.Off
            }
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.plugin_enable_plugins_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(MoviesTheme.spacing.xs))
                Text(
                    text = stringResource(R.string.plugin_enable_plugins_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalContentColor.current.copy(alpha = 0.64f)
                )
            }

            Spacer(modifier = Modifier.width(MoviesTheme.spacing.lg))

            MoviesSwitch(checked = pluginsEnabled, enabled = true, focused = isFocused)
        }
    }
}

@Composable
private fun AddRepositoryInline(
    url: String,
    onUrlChange: (String) -> Unit,
    onConfirm: () -> Unit,
    isLoading: Boolean
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val textFieldFocusRequester = remember { FocusRequester() }
    var isEditing by remember { mutableStateOf(false) }

    // When isEditing changes to true, focus the text field and show keyboard
    LaunchedEffect(isEditing) {
        if (isEditing) {
            textFieldFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MoviesTheme.colors.BackgroundCard),
        shape = M3eShapes.card
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.plugin_add_repository),
                style = MaterialTheme.typography.titleMedium,
                color = MoviesTheme.colors.TextPrimary
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
                    modifier = Modifier.weight(1f),
                    focus = MoviesCardFocus.Frosted
                ) {
                    Box(modifier = Modifier.padding(MoviesTheme.spacing.md)) {
                        BasicTextField(
                            value = url,
                            onValueChange = onUrlChange,
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
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done,
                                autoCorrectEnabled = false
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    onConfirm()
                                    isEditing = false
                                    keyboardController?.hide()
                                    focusManager.clearFocus(force = true)
                                }
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium.bodyFloor().copy(
                                color = MoviesTheme.colors.TextPrimary,
                                textDirection = TextDirection.Content
                            ),
                            cursorBrush = SolidColor(
                                if (isEditing) MoviesTheme.colors.Primary else Color.Transparent
                            ),
                            decorationBox = { innerTextField ->
                                if (url.isEmpty()) {
                                    Text(
                                        text = stringResource(
                                            R.string.plugin_url_or_short_code_placeholder
                                        ),
                                        style = MaterialTheme.typography.bodyMedium.bodyFloor(),
                                        color = MoviesTheme.colors.TextTertiary
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                MoviesButton(
                    onClick = {
                        onConfirm()
                        isEditing = false
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                    },
                    enabled = !isLoading && url.isNotBlank(),
                    modifier = Modifier
                        .focusProperties { right = FocusRequester.Cancel }
                        .semantics { role = Role.Button }
                        .tvControlHeight(),
                    style = MoviesButtonStyle.Primary
                ) {
                    if (isLoading) {
                        LoadingIndicator(modifier = Modifier.size(TvSizeFloors.controlIcon))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.cd_add),
                            modifier = Modifier.size(TvSizeFloors.controlIcon)
                        )
                    }
                    Spacer(modifier = Modifier.width(MoviesTheme.spacing.sm))
                    Text(stringResource(R.string.plugin_add_btn))
                }
            }
        }
    }
}

@Composable
private fun ManageFromPhoneCard(onClick: () -> Unit, focusRequester: FocusRequester) {
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
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(MoviesTheme.spacing.lg))
                Column {
                    Text(
                        text = stringResource(R.string.plugin_manage_from_phone_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = stringResource(R.string.plugin_manage_from_phone_subtitle),
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

@Composable
private fun QrCodeOverlay(
    qrBitmap: Bitmap?,
    serverUrl: String?,
    onClose: () -> Unit,
    hasPendingChange: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(hasPendingChange) {
        if (!hasPendingChange) {
            focusRequester.requestFocus()
        }
    }

    BackHandler { onClose() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.plugin_qr_instruction),
                style = MaterialTheme.typography.bodyMedium,
                color = MoviesTheme.colors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(MoviesTheme.spacing.lg))

            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.cd_qr_code),
                    modifier = Modifier.size(220.dp),
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
                    modifier = Modifier.padding(
                        horizontal = MoviesTheme.spacing.xl,
                        vertical = MoviesTheme.spacing.md
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(TvSizeFloors.controlIcon)
                    )
                    Spacer(modifier = Modifier.width(MoviesTheme.spacing.sm))
                    Text(
                        text = stringResource(R.string.plugin_qr_close)
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfirmRepoChangesDialog(
    pendingChange: PendingRepoChangeInfo,
    onConfirm: () -> Unit,
    onReject: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
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
                    text = stringResource(R.string.plugin_confirm_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MoviesTheme.colors.TextPrimary
                )

                Spacer(modifier = Modifier.height(MoviesTheme.spacing.lg))

                Text(
                    text = stringResource(R.string.plugin_confirm_subtitle),
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
                            shape = M3eShapes.card
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
                                text = stringResource(R.string.plugin_confirm_added),
                                style = MaterialTheme.typography.titleSmall,
                                color = MoviesTheme.colors.Success,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = MoviesTheme.spacing.xs)
                            )
                            pendingChange.addedUrls.forEach { url ->
                                Text(
                                    text = "+ $url",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MoviesTheme.colors.Success,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = MoviesTheme.spacing.sm,
                                            bottom = MoviesTheme.spacing.xxs
                                        )
                                )
                            }
                            Spacer(modifier = Modifier.height(MoviesTheme.spacing.sm))
                        }

                        if (pendingChange.removedUrls.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.plugin_confirm_removed),
                                style = MaterialTheme.typography.titleSmall,
                                color = MoviesTheme.colors.Error,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = MoviesTheme.spacing.xs)
                            )
                            pendingChange.removedUrls.forEach { url ->
                                Text(
                                    text = "- $url",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MoviesTheme.colors.Error,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = MoviesTheme.spacing.sm,
                                            bottom = MoviesTheme.spacing.xxs
                                        )
                                )
                            }
                            Spacer(modifier = Modifier.height(MoviesTheme.spacing.sm))
                        }

                        if (pendingChange.addedUrls.isEmpty() &&
                            pendingChange.removedUrls.isEmpty()
                        ) {
                            Text(
                                text = stringResource(R.string.plugin_confirm_no_changes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MoviesTheme.colors.TextSecondary
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(
                        R.string.plugin_confirm_total,
                        pendingChange.proposedUrls.size
                    ),
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
                                modifier = Modifier.padding(
                                    horizontal = MoviesTheme.spacing.xl,
                                    vertical = MoviesTheme.spacing.md
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(TvSizeFloors.controlIcon)
                                )
                                Spacer(modifier = Modifier.width(MoviesTheme.spacing.sm))
                                Text(
                                    text = stringResource(R.string.plugin_confirm_reject)
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
                                text = stringResource(R.string.plugin_confirm_confirm),
                                modifier = Modifier.padding(
                                    horizontal = MoviesTheme.spacing.xl,
                                    vertical = MoviesTheme.spacing.md
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmScraperEnableDialog(
    scraperName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    BackHandler { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = { },
            modifier = Modifier.width(560.dp),
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
                    text = stringResource(R.string.plugin_risky_enable_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MoviesTheme.colors.TextPrimary
                )

                Spacer(modifier = Modifier.height(MoviesTheme.spacing.lg))

                Text(
                    text = stringResource(R.string.plugin_risky_enable_message, scraperName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MoviesTheme.colors.TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(MoviesTheme.spacing.xl))

                Row(horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.lg)) {
                    MoviesSurface(
                        onClick = onDismiss,
                        modifier = Modifier.semantics { role = Role.Button }.tvControlHeight(),
                        shape = M3eShapes.capsule
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = MoviesTheme.spacing.xl,
                                vertical = MoviesTheme.spacing.md
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(TvSizeFloors.controlIcon)
                            )
                            Spacer(modifier = Modifier.width(MoviesTheme.spacing.sm))
                            Text(
                                text = stringResource(R.string.plugin_risky_enable_cancel)
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
                        Row(
                            modifier = Modifier.padding(
                                horizontal = MoviesTheme.spacing.xl,
                                vertical = MoviesTheme.spacing.md
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(TvSizeFloors.controlIcon)
                            )
                            Spacer(modifier = Modifier.width(MoviesTheme.spacing.sm))
                            Text(
                                text = stringResource(R.string.plugin_risky_enable_confirm)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RepositoryCard(
    repository: PluginRepository,
    repoScrapers: List<ScraperInfo>,
    onRefresh: () -> Unit,
    onRemove: () -> Unit,
    onToggleAll: (Boolean) -> Unit,
    removeFocusRequester: FocusRequester,
    isLoading: Boolean,
    isReadOnly: Boolean = false
) {
    val enabledCount = repoScrapers.count { it.enabled }
    val anyEnabled = enabledCount > 0
    var isToggleFocused by remember { mutableStateOf(false) }
    var isRefreshFocused by remember { mutableStateOf(false) }
    var isRemoveFocused by remember { mutableStateOf(false) }
    val isCardFocused = isToggleFocused || isRefreshFocused || isRemoveFocused
    val cardScale by animateFloatAsState(
        targetValue = if (isCardFocused) 1.01f else 1f,
        animationSpec = ExpressiveMotion.expressiveFastSpatial(),
        label = "repositoryCardScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .background(
                color = MoviesTheme.colors.BackgroundCard,
                shape = M3eShapes.card
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MoviesTheme.spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = repository.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MoviesTheme.colors.TextPrimary
                )
                Spacer(modifier = Modifier.height(MoviesTheme.spacing.xs))
                Text(
                    text = stringResource(R.string.plugin_providers_count, repository.scraperCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MoviesTheme.colors.TextSecondary
                )
                Text(
                    text = stringResource(
                        R.string.plugin_updated_format,
                        formatDate(repository.lastUpdated)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MoviesTheme.colors.TextSecondary
                )
            }

            if (!isReadOnly) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (repoScrapers.isNotEmpty()) {
                        MoviesSurface(
                            onClick = { onToggleAll(!anyEnabled) },
                            modifier = Modifier
                                .onFocusChanged { isToggleFocused = it.isFocused }
                                .semantics {
                                    role = Role.Switch
                                    toggleableState =
                                        if (anyEnabled) ToggleableState.On else ToggleableState.Off
                                },
                            shape = M3eShapes.capsule
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = MoviesTheme.spacing.md,
                                    vertical = MoviesTheme.spacing.sm
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "$enabledCount/${repoScrapers.size}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                MoviesSwitch(
                                    checked = anyEnabled,
                                    enabled = true,
                                    focused = isToggleFocused
                                )
                            }
                        }
                    }

                    MoviesButton(
                        onClick = onRefresh,
                        enabled = !isLoading,
                        modifier = Modifier
                            .onFocusChanged { isRefreshFocused = it.isFocused }
                            .semantics { role = Role.Button }
                            .tvControlHeight()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.cd_refresh)
                        )
                    }

                    MoviesButton(
                        onClick = onRemove,
                        enabled = !isLoading,
                        modifier = Modifier
                            .focusRequester(removeFocusRequester)
                            .onFocusChanged { isRemoveFocused = it.isFocused }
                            .semantics { role = Role.Button }
                            .tvControlHeight(),
                        style = MoviesButtonStyle.Destructive
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.cd_remove)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScraperCard(
    scraper: ScraperInfo,
    onToggle: (Boolean) -> Unit,
    onTest: () -> Unit,
    isTesting: Boolean,
    testResults: List<LocalScraperResult>?,
    testDiagnostics: com.kien.movies.core.plugin.TestDiagnostics? = null,
    isReadOnly: Boolean = false
) {
    var showResults by remember { mutableStateOf(false) }
    var isTestFocused by remember { mutableStateOf(false) }
    var isToggleFocused by remember { mutableStateOf(false) }
    val isCardFocused = isTestFocused || isToggleFocused
    val cardScale by animateFloatAsState(
        targetValue = if (isCardFocused) 1.01f else 1f,
        animationSpec = ExpressiveMotion.expressiveFastSpatial(),
        label = "scraperCardScale"
    )

    LaunchedEffect(testResults, testDiagnostics) {
        showResults = testResults != null || testDiagnostics != null
    }

    // Use Box instead of focusable Surface to allow child focus
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .background(
                color = MoviesTheme.colors.BackgroundCard,
                shape = M3eShapes.card
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MoviesTheme.spacing.lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm)
                    ) {
                        Text(
                            text = scraper.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MoviesTheme.colors.TextPrimary
                        )

                        // Type badges
                        scraper.supportedTypes.forEach { type ->
                            TypeBadge(type = type)
                        }
                    }

                    Spacer(modifier = Modifier.height(MoviesTheme.spacing.xs))

                    Text(
                        text = stringResource(R.string.plugin_version, scraper.version),
                        style = MaterialTheme.typography.bodySmall,
                        color = MoviesTheme.colors.TextSecondary
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Test button
                    MoviesButton(
                        onClick = onTest,
                        enabled = !isTesting && scraper.enabled,
                        modifier = Modifier
                            .focusProperties { left = FocusRequester.Cancel }
                            .onFocusChanged { isTestFocused = it.isFocused }
                            .semantics { role = Role.Button }
                            .tvControlHeight()
                    ) {
                        if (isTesting) {
                            LoadingIndicator(
                                modifier = Modifier.size(
                                    TvSizeFloors.atLeast(
                                        MoviesTheme.spacing.lg,
                                        TvSizeFloors.controlIcon
                                    )
                                )
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.cd_test),
                                modifier = Modifier.size(
                                    TvSizeFloors.atLeast(
                                        MoviesTheme.spacing.lg,
                                        TvSizeFloors.controlIcon
                                    )
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(MoviesTheme.spacing.xs))
                        Text(stringResource(R.string.plugin_test_btn))
                    }

                    // Enable toggle
                    if (!isReadOnly) {
                        // The card carries the name as visible text, but the toggle is its own
                        // node, so without this a screen reader hears only "switch, on".
                        val toggleLabel = stringResource(R.string.cd_enable_scraper, scraper.name)
                        MoviesSurface(
                            onClick = { onToggle(!scraper.enabled) },
                            modifier = Modifier
                                .onFocusChanged { isToggleFocused = it.isFocused }
                                .semantics {
                                    role = Role.Switch
                                    contentDescription = toggleLabel
                                    toggleableState =
                                        if (scraper.enabled) ToggleableState.On else ToggleableState.Off
                                },
                            shape = M3eShapes.capsule
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MoviesSwitch(
                                    checked = scraper.enabled,
                                    enabled = true,
                                    focused = isToggleFocused
                                )
                            }
                        }
                    }
                }
            }

            // Test results with diagnostics
            AnimatedVisibility(
                visible =
                    showResults && (testResults != null || testDiagnostics != null)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MoviesTheme.spacing.md)
                ) {
                    // Diagnostic steps: collapsible, click to expand/collapse
                    if (testDiagnostics != null && testDiagnostics.steps.isNotEmpty()) {
                        var diagnosticsExpanded by remember { mutableStateOf(false) }
                        MoviesSurface(
                            onClick = { diagnosticsExpanded = !diagnosticsExpanded },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { role = Role.Button }
                        ) {
                            Column(modifier = Modifier.padding(MoviesTheme.spacing.sm)) {
                                Text(
                                    text = if (diagnosticsExpanded) {
                                        stringResource(
                                            R.string.plugin_diagnostics_collapse
                                        )
                                    } else {
                                        stringResource(R.string.plugin_diagnostics_expand)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LocalContentColor.current.copy(alpha = 0.64f)
                                )
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = diagnosticsExpanded
                                ) {
                                    Text(
                                        text = testDiagnostics.steps.joinToString("\n"),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(top = MoviesTheme.spacing.xs)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(MoviesTheme.spacing.sm))
                    }

                    Text(
                        text = stringResource(R.string.plugin_test_results, testResults?.size ?: 0),
                        style = MaterialTheme.typography.bodySmall,
                        color = MoviesTheme.colors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(MoviesTheme.spacing.sm))

                    testResults?.take(3)?.forEach { result ->
                        TestResultItem(result = result)
                        Spacer(modifier = Modifier.height(MoviesTheme.spacing.xs))
                    }

                    if ((testResults?.size ?: 0) > 3) {
                        Text(
                            text = stringResource(R.string.plugin_and_more, testResults!!.size - 3),
                            style = MaterialTheme.typography.bodySmall,
                            color = MoviesTheme.colors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeBadge(type: String) {
    val color = when (type.lowercase()) {
        "movie" -> MoviesTheme.colors.Success
        "series", "show", "tv" -> MoviesTheme.colors.Info
        else -> MoviesTheme.colors.TextSecondary
    }

    Box(
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.2f),
                shape = BadgeShape
            )
            .padding(horizontal = 6.dp, vertical = MoviesTheme.spacing.xxs)
    ) {
        Text(
            text = type.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun TestResultItem(result: LocalScraperResult) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MoviesTheme.colors.Surface,
                shape = M3eShapes.card
            )
            .padding(MoviesTheme.spacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MoviesTheme.colors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                result.quality?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MoviesTheme.colors.Primary
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MoviesPrimitives.success,
                modifier = Modifier.size(MoviesTheme.spacing.lg)
            )
        }
    }
}

@Composable
private fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MoviesTheme.colors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MessageOverlay(successMessage: String?, errorMessage: String?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(MoviesTheme.spacing.xl),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = successMessage != null || errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val isSuccess = successMessage != null
            val message = successMessage ?: errorMessage ?: ""

            Surface(
                onClick = { },
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (isSuccess) {
                        MoviesPrimitives.green700.copy(alpha = 0.9f)
                    } else {
                        MoviesPrimitives.red600.copy(alpha = 0.9f)
                    }
                ),
                shape = ClickableSurfaceDefaults.shape(M3eShapes.card)
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 20.dp,
                        vertical = MoviesTheme.spacing.md
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.md)
                ) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val locale = Locale.getDefault()
    return SimpleDateFormat(
        android.text.format.DateFormat.getBestDateTimePattern(locale, "dMMMy"),
        locale
    ).format(Date(timestamp))
}

private val BadgeShape = RoundedCornerShape(percent = 50)
