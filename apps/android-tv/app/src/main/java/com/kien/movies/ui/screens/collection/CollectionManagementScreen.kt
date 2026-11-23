package com.kien.movies.ui.screens.collection

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.data.local.ValidationResult
import com.kien.movies.domain.model.Collection
import com.kien.movies.ui.components.LoadingIndicator
import com.kien.movies.ui.components.MoviesDialog
import com.kien.movies.ui.components.TvReorderList
import com.kien.movies.ui.components.buttonLabelStyle
import com.kien.movies.ui.components.expressive.MoviesButton
import com.kien.movies.ui.components.expressive.MoviesButtonStyle
import com.kien.movies.ui.screens.settings.SettingsVerticalScrollIndicators
import com.kien.movies.ui.theme.MoviesTheme
import com.kien.movies.ui.theme.metaFloor
import com.kien.movies.ui.theme.screenTitleFloor
import com.kien.movies.ui.theme.tvControlHeight
import com.kien.movies.ui.util.contentTextDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CollectionManagementScreen(
    viewModel: CollectionManagementViewModel = hiltViewModel(),
    onNavigateToEditor: (String?) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val collectionsScrollState = rememberScrollState()
    val collectionRowHeightCache = remember { mutableMapOf<String, Float>() }
    var lastFocusedTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingFocusTarget by remember { mutableStateOf<String?>(null) }
    var hasAppliedInitialFocus by remember { mutableStateOf(false) }
    val importButtonFocusRequester = remember { FocusRequester() }
    val newButtonFocusRequester = remember { FocusRequester() }
    val itemFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val itemDeleteFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    var collectionToDelete by remember { mutableStateOf<Collection?>(null) }
    val deleteDialogFocusRequester = remember { FocusRequester() }

    LaunchedEffect(uiState.collections) {
        val validIds = uiState.collections.mapTo(mutableSetOf()) { it.id }
        itemFocusRequesters.keys.retainAll(validIds)
        itemDeleteFocusRequesters.keys.retainAll(validIds)
    }

    LaunchedEffect(collectionToDelete) {
        if (collectionToDelete != null) {
            requestFocusWhenReady(deleteDialogFocusRequester)
        }
    }

    val activeFocusTarget = pendingFocusTarget ?: if (!hasAppliedInitialFocus) {
        lastFocusedTarget ?: "new"
    } else {
        null
    }
    val ensureComposedCollectionId = when {
        activeFocusTarget == null -> null
        activeFocusTarget.startsWith("edit:") -> activeFocusTarget.removePrefix("edit:")
        activeFocusTarget.startsWith("delete:") -> activeFocusTarget.removePrefix("delete:")
        else -> null
    }

    LaunchedEffect(
        uiState.isLoading,
        uiState.showImportDialog,
        collectionToDelete,
        pendingFocusTarget,
        uiState.collections.size
    ) {
        if (uiState.isLoading || uiState.showImportDialog ||
            collectionToDelete != null
        ) {
            return@LaunchedEffect
        }
        val target = pendingFocusTarget ?: if (!hasAppliedInitialFocus) {
            lastFocusedTarget ?: "new"
        } else {
            return@LaunchedEffect
        }
        val requester = when {
            target == "import" -> importButtonFocusRequester

            target == "new" -> newButtonFocusRequester

            target.startsWith("edit:") -> {
                val id = target.removePrefix("edit:")
                if (uiState.collections.any { it.id == id }) {
                    itemFocusRequesters.getOrPut(id) { FocusRequester() }
                } else {
                    newButtonFocusRequester
                }
            }

            target.startsWith("delete:") -> {
                val id = target.removePrefix("delete:")
                if (uiState.collections.any { it.id == id }) {
                    itemDeleteFocusRequesters.getOrPut(id) { FocusRequester() }
                } else {
                    newButtonFocusRequester
                }
            }

            else -> newButtonFocusRequester
        }
        val focused = requestFocusWhenReady(requester)
        if (!focused && requester !== newButtonFocusRequester) {
            requestFocusWhenReady(newButtonFocusRequester)
        }
        hasAppliedInitialFocus = true
        if (pendingFocusTarget == target) {
            pendingFocusTarget = null
        }
    }

    var exportMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(exportMessage) {
        if (exportMessage != null) {
            kotlinx.coroutines.delay(3000)
            exportMessage = null
        }
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingIndicator()
        }
        return
    }

    if (uiState.showImportDialog) {
        BackHandler { viewModel.hideImportDialog() }
        ImportContent(
            importText = uiState.importText,
            importError = uiState.importError,
            onTextChange = { viewModel.updateImportText(it) },
            onImport = { viewModel.importCollections() },
            onPaste = {},
            onBack = { viewModel.hideImportDialog() },
            importMode = uiState.importMode,
            onModeChange = { viewModel.setImportMode(it) },
            importUrl = uiState.importUrl,
            onUrlChange = { viewModel.updateImportUrl(it) },
            onFetchUrl = { viewModel.fetchUrl() },
            validationResult = uiState.validationResult,
            isLoadingImport = uiState.isLoadingImport,
            onValidate = { viewModel.validateCurrentText() },
            onPickFile = {
                scope.launch {
                    viewModel.loadFromFile(context)
                }
            },
            onConfirmImport = { viewModel.confirmImport() }
        )
        return
    }

    val collectionsHeader: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.collections_header),
                style = MaterialTheme.typography.headlineMedium
                    .screenTitleFloor()
                    .copy(textDirection = TextDirection.Content),
                color = MoviesTheme.colors.TextPrimary
            )
            val headerActionsEnabled = uiState.movingCollectionKey == null
            Row(
                horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm),
                modifier = Modifier.focusEntryTarget(newButtonFocusRequester)
            ) {
                if (uiState.collections.isNotEmpty() &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ) {
                    MoviesButton(
                        onClick = {
                            scope.launch {
                                try {
                                    val json = viewModel.getExportJson()
                                    withContext(Dispatchers.IO) {
                                        val resolver = context.contentResolver
                                        // Delete existing file first
                                        val existingUri = android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
                                        resolver.delete(
                                            existingUri,
                                            "${android.provider.MediaStore.Downloads.DISPLAY_NAME} = ?",
                                            arrayOf("movies-collections.json")
                                        )
                                        // Write new file
                                        val values = android.content.ContentValues().apply {
                                            put(
                                                android.provider.MediaStore.Downloads.DISPLAY_NAME,
                                                "movies-collections.json"
                                            )
                                            put(
                                                android.provider.MediaStore.Downloads.MIME_TYPE,
                                                "application/json"
                                            )
                                        }
                                        val uri = resolver.insert(existingUri, values)
                                        uri?.let {
                                            resolver.openOutputStream(it)?.use { os ->
                                                os.write(json.toByteArray())
                                            }
                                        }
                                    }
                                    exportMessage = "saved"
                                } catch (_: Exception) {
                                    exportMessage = "failed"
                                }
                            }
                        },
                        enabled = headerActionsEnabled
                    ) {
                        Text(
                            exportMessage?.let {
                                when (it) {
                                    "saved" -> stringResource(R.string.collections_saved_downloads)
                                    "failed" -> stringResource(R.string.collections_export_failed)
                                    else -> it
                                }
                            } ?: stringResource(R.string.collections_export_file)
                        )
                    }
                }
                MoviesButton(
                    onClick = {
                        lastFocusedTarget = "import"
                        pendingFocusTarget = "import"
                        viewModel.showImportDialog()
                    },
                    modifier = Modifier.focusRequester(importButtonFocusRequester),
                    enabled = headerActionsEnabled
                ) {
                    Text(stringResource(R.string.collections_import))
                }
                val firstCollectionEditRequester = uiState.collections.firstOrNull()?.id?.let {
                        firstId
                    ->
                    itemFocusRequesters.getOrPut(firstId) { FocusRequester() }
                }
                MoviesButton(
                    onClick = {
                        lastFocusedTarget = "new"
                        onNavigateToEditor(null)
                    },
                    modifier = Modifier
                        .focusRequester(newButtonFocusRequester)
                        .focusProperties {
                            if (firstCollectionEditRequester != null) {
                                down = firstCollectionEditRequester
                            }
                        },
                    enabled = headerActionsEnabled
                ) {
                    Text(stringResource(R.string.collections_new))
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.collections.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = MoviesTheme.spacing.xxxl,
                        start = MoviesTheme.spacing.xxxl,
                        end = MoviesTheme.spacing.xxxl
                    )
            ) {
                collectionsHeader()
                Spacer(modifier = Modifier.height(MoviesTheme.spacing.xl))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.collections_empty),
                        color = MoviesTheme.colors.TextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            val collectionsMovingIndex = uiState.collections.indexOfFirst {
                it.id ==
                    uiState.movingCollectionKey
            }
            TvReorderList(
                items = uiState.collections,
                key = { it.id },
                canMove = { uiState.collections.size > 1 && uiState.movingCollectionKey == null },
                movingKey = uiState.movingCollectionKey,
                onStartMove = viewModel::enterCollectionMoveMode,
                onMoveBy = viewModel::moveCollectionDuringDrag,
                onConfirmMove = { viewModel.confirmCollectionMove() },
                onCancelMove = viewModel::cancelCollectionMove,
                modifier = Modifier.fillMaxSize(),
                scrollState = collectionsScrollState,
                ensureComposedKey = ensureComposedCollectionId,
                rowHeightCache = collectionRowHeightCache,
                contentPadding = PaddingValues(
                    top = MoviesTheme.spacing.xxxl,
                    start = MoviesTheme.spacing.xxxl,
                    end = MoviesTheme.spacing.xxxl,
                    bottom = MoviesTheme.spacing.xxxl
                ),
                header = {
                    Column {
                        collectionsHeader()
                        Spacer(modifier = Modifier.height(MoviesTheme.spacing.xl))
                    }
                }
            ) { collection, isMoving, onStartMove, onConfirmMove ->
                val editFocusRequester = itemFocusRequesters.getOrPut(collection.id) {
                    FocusRequester()
                }
                val deleteFocusRequester = itemDeleteFocusRequesters.getOrPut(collection.id) {
                    FocusRequester()
                }
                CollectionListItem(
                    collection = collection,
                    isMoving = isMoving,
                    canMove = uiState.collections.size > 1,
                    actionsEnabled = uiState.movingCollectionKey == null || isMoving,
                    editFocusRequester = editFocusRequester,
                    deleteFocusRequester = deleteFocusRequester,
                    onEdit = {
                        lastFocusedTarget = "edit:${collection.id}"
                        onNavigateToEditor(collection.id)
                    },
                    onDelete = { collectionToDelete = collection },
                    onStartMove = onStartMove,
                    onConfirmMove = onConfirmMove
                )
            }
            SettingsVerticalScrollIndicators(
                state = collectionsScrollState,
                suppressBackward = collectionsMovingIndex == 0,
                suppressForward = collectionsMovingIndex >= 0 &&
                    collectionsMovingIndex == uiState.collections.lastIndex,
                forceBackward = collectionsMovingIndex > 0,
                forceForward = collectionsMovingIndex in 0 until uiState.collections.lastIndex
            )
        }

        collectionToDelete?.let { collection ->
            MoviesDialog(
                onDismiss = {
                    collectionToDelete = null
                    pendingFocusTarget = "delete:${collection.id}"
                },
                title = stringResource(R.string.collections_delete_confirm_title),
                subtitle = stringResource(
                    R.string.collections_delete_confirm_subtitle,
                    collection.title
                ),
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
                            collectionToDelete = null
                            pendingFocusTarget = "delete:${collection.id}"
                        },
                        modifier = Modifier
                            .focusRequester(deleteDialogFocusRequester)
                            .semantics { role = Role.Button }
                            .tvControlHeight()
                    ) {
                        Text(
                            stringResource(R.string.collections_cancel),
                            style = buttonLabelStyle()
                        )
                    }
                    MoviesButton(
                        onClick = {
                            val currentIndex = uiState.collections.indexOfFirst {
                                it.id == collection.id
                            }
                            val nextTargetId = uiState.collections
                                .filterNot { it.id == collection.id }
                                .let { remaining ->
                                    if (remaining.isEmpty()) {
                                        null
                                    } else {
                                        remaining[currentIndex.coerceIn(0, remaining.lastIndex)].id
                                    }
                                }
                            viewModel.deleteCollection(collection.id)
                            collectionToDelete = null
                            val nextTarget = nextTargetId?.let { "edit:$it" } ?: "new"
                            lastFocusedTarget = nextTarget
                            pendingFocusTarget = nextTarget
                        },
                        modifier = Modifier.semantics { role = Role.Button }.tvControlHeight(),
                        style = MoviesButtonStyle.Destructive
                    ) {
                        Text(
                            stringResource(R.string.collections_delete_confirm),
                            style = buttonLabelStyle()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun ImportContent(
    importText: String,
    importError: String?,
    onTextChange: (String) -> Unit,
    onImport: () -> Unit,
    onPaste: () -> Unit,
    onBack: () -> Unit,
    importMode: ImportMode,
    onModeChange: (ImportMode) -> Unit,
    importUrl: String,
    onUrlChange: (String) -> Unit,
    onFetchUrl: () -> Unit,
    validationResult: ValidationResult?,
    isLoadingImport: Boolean,
    onValidate: () -> Unit,
    onPickFile: () -> Unit,
    onConfirmImport: () -> Unit
) {
    val modeFocusRequesters = remember {
        ImportMode.entries.associateWith { FocusRequester() }
    }
    val fileTabFocusRequester = modeFocusRequesters.getValue(ImportMode.FILE)
    LaunchedEffect(Unit) {
        requestFocusWhenReady(modeFocusRequesters[importMode] ?: fileTabFocusRequester)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = MoviesTheme.spacing.xxxl,
            start = MoviesTheme.spacing.xxxl,
            end = MoviesTheme.spacing.xxxl,
            bottom = MoviesTheme.spacing.xxxl
        ),
        verticalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.none)
    ) {
        item(key = "import_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.collections_import_header),
                    style = MaterialTheme.typography.headlineMedium
                        .screenTitleFloor()
                        .copy(textDirection = TextDirection.Content),
                    color = MoviesTheme.colors.TextPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm)) {
                    MoviesButton(
                        onClick = onBack,
                        modifier = Modifier.blockHorizontalFocus()
                    ) { Text(stringResource(R.string.collections_cancel)) }
                }
            }

            Spacer(modifier = Modifier.height(MoviesTheme.spacing.lg))
        }

        item {
            Text(
                text = stringResource(R.string.collections_import_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MoviesTheme.colors.TextSecondary
            )

            Spacer(modifier = Modifier.height(MoviesTheme.spacing.md))

            // Mode tabs
            Row(
                horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm),
                modifier = Modifier.focusEntryTarget(
                    modeFocusRequesters[importMode] ?: fileTabFocusRequester
                )
            ) {
                ImportMode.entries.filter { it != ImportMode.PASTE }.forEach { mode ->
                    val label = when (mode) {
                        ImportMode.PASTE -> stringResource(R.string.collections_mode_paste)
                        ImportMode.FILE -> stringResource(R.string.collections_mode_file)
                        ImportMode.URL -> stringResource(R.string.collections_mode_url)
                    }
                    val tabModifier = Modifier.focusRequester(modeFocusRequesters.getValue(mode))
                    val isSelected = mode == importMode
                    MoviesButton(
                        onClick = { onModeChange(mode) },
                        modifier = tabModifier.semantics {
                            role = Role.Tab
                            this.selected = isSelected
                        }.tvControlHeight(),
                        style = if (isSelected) MoviesButtonStyle.Selected else MoviesButtonStyle.Secondary
                    ) {
                        Text(label)
                    }
                }
            }

            Spacer(modifier = Modifier.height(MoviesTheme.spacing.md))
        }

        item {
            when (importMode) {
                ImportMode.PASTE -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm),
                        modifier = Modifier.trapHorizontalFocus()
                    ) {
                        MoviesButton(onClick = onPaste) {
                            Text(stringResource(R.string.collections_paste_clipboard))
                        }
                        if (importText.isNotBlank()) {
                            MoviesButton(onClick = onValidate) {
                                Text(stringResource(R.string.collections_validate))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(MoviesTheme.spacing.md))

                    Surface(
                        shape = RoundedCornerShape(MoviesTheme.radii.md),
                        colors = SurfaceDefaults.colors(
                            containerColor = MoviesTheme.colors.BackgroundElevated
                        ),
                        border = Border(
                            border = BorderStroke(
                                MoviesTheme.spacing.hairline,
                                if (importError !=
                                    null
                                ) {
                                    MoviesTheme.colors.Error
                                } else {
                                    MoviesTheme.colors.Border
                                }
                            ),
                            shape = RoundedCornerShape(MoviesTheme.radii.md)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        Box(modifier = Modifier.padding(MoviesTheme.spacing.md)) {
                            BasicTextField(
                                value = importText,
                                onValueChange = onTextChange,
                                modifier = Modifier.fillMaxSize(),
                                textStyle = MaterialTheme.typography.bodySmall
                                    .metaFloor()
                                    .copy(
                                        color = MoviesTheme.colors.TextPrimary,
                                        textDirection = TextDirection.Content
                                    ),
                                cursorBrush = SolidColor(MoviesTheme.colors.Primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                decorationBox = { innerTextField ->
                                    if (importText.isEmpty()) {
                                        Text(
                                            text = stringResource(
                                                R.string.collections_paste_hint
                                            ),
                                            style = MaterialTheme.typography.bodySmall
                                                .metaFloor(),
                                            color = MoviesTheme.colors.TextTertiary
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }
                }

                ImportMode.FILE -> {
                    Text(
                        text = stringResource(R.string.collections_file_description),
                        style = MaterialTheme.typography.bodySmall.metaFloor(),
                        color = MoviesTheme.colors.TextTertiary
                    )
                    Spacer(modifier = Modifier.height(MoviesTheme.spacing.sm))
                    MoviesButton(
                        onClick = onPickFile,
                        modifier = Modifier.blockHorizontalFocus()
                    ) { Text(stringResource(R.string.collections_load_file)) }

                    if (importText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(MoviesTheme.spacing.md))
                        Text(
                            text = stringResource(
                                R.string.collections_file_loaded,
                                importText.length
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MoviesTheme.colors.TextSecondary
                        )
                    }
                }

                ImportMode.URL -> {
                    MoviesTextField(
                        value = importUrl,
                        onValueChange = onUrlChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .blockHorizontalFocus(),
                        placeholder = stringResource(
                            R.string.collections_import_url_placeholder
                        )
                    )

                    Spacer(modifier = Modifier.height(MoviesTheme.spacing.md))

                    MoviesButton(
                        onClick = onFetchUrl,
                        modifier = Modifier.blockHorizontalFocus()
                    ) {
                        Text(
                            if (isLoadingImport) {
                                stringResource(
                                    R.string.collections_fetching
                                )
                            } else {
                                stringResource(R.string.collections_fetch_url)
                            }
                        )
                    }

                    if (isLoadingImport) {
                        Spacer(modifier = Modifier.height(MoviesTheme.spacing.md))
                        LoadingIndicator()
                    }
                }
            }
        }

        // Validation result preview
        if (validationResult != null && validationResult.valid) {
            item {
                Spacer(modifier = Modifier.height(MoviesTheme.spacing.lg))
                Surface(
                    shape = RoundedCornerShape(MoviesTheme.radii.md),
                    colors = SurfaceDefaults.colors(
                        containerColor = MoviesTheme.colors.BackgroundCard
                    ),
                    border = Border(
                        border = BorderStroke(
                            MoviesTheme.spacing.hairline,
                            MoviesTheme.colors.Primary
                        ),
                        shape = RoundedCornerShape(MoviesTheme.radii.md)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(MoviesTheme.spacing.lg)) {
                        Text(
                            text = stringResource(R.string.collections_valid_json),
                            style = MaterialTheme.typography.titleMedium,
                            color = MoviesTheme.colors.Primary
                        )
                        Spacer(modifier = Modifier.height(MoviesTheme.spacing.xs))
                        Text(
                            text = stringResource(
                                R.string.collections_valid_summary,
                                validationResult.collectionCount,
                                validationResult.folderCount
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MoviesTheme.colors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(MoviesTheme.spacing.md))
                        MoviesButton(
                            onClick = onConfirmImport,
                            modifier = Modifier.blockHorizontalFocus()
                        ) {
                            Text(stringResource(R.string.collections_confirm_import))
                        }
                    }
                }
            }
        }

        if (importError != null) {
            item {
                Spacer(modifier = Modifier.height(MoviesTheme.spacing.sm))
                Text(
                    text = importError,
                    style = MaterialTheme.typography.bodySmall.metaFloor(),
                    color = MoviesTheme.colors.Error
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CollectionListItem(
    collection: Collection,
    isMoving: Boolean,
    canMove: Boolean,
    actionsEnabled: Boolean,
    editFocusRequester: FocusRequester? = null,
    deleteFocusRequester: FocusRequester? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStartMove: () -> Unit,
    onConfirmMove: () -> Unit
) {
    val cardColor by animateColorAsState(
        targetValue = if (isMoving) {
            MoviesTheme.colors.FocusBackground
        } else {
            MoviesTheme.colors.BackgroundCard
        },
        animationSpec = com.kien.movies.ui.theme.MoviesMotion.focusTween(),
        label = "collectionCardColor"
    )
    val cardBorder = if (isMoving) {
        Border(
            border = MoviesTheme.focusRing.border(MoviesTheme.spacing.xxs),
            shape = RoundedCornerShape(MoviesTheme.radii.md)
        )
    } else {
        Border.None
    }
    Surface(
        shape = RoundedCornerShape(MoviesTheme.radii.md),
        colors = SurfaceDefaults.colors(containerColor = cardColor),
        border = cardBorder,
        modifier = Modifier.fillMaxWidth()
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
                    text = collection.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDirection = collection.title.contentTextDirection()
                    ),
                    color = MoviesTheme.colors.TextPrimary
                )
                val summary = buildString {
                    append(
                        stringResource(R.string.collections_folder_count, collection.folders.size)
                    )
                    if (collection.pinToTop) {
                        append(" · ")
                        append(stringResource(R.string.collections_pinned))
                    }
                }
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall.metaFloor(),
                    color = MoviesTheme.colors.TextTertiary
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm),
                modifier = Modifier.trapHorizontalFocus()
            ) {
                MoviesButton(
                    modifier = Modifier.tvControlHeight(),
                    onClick = if (isMoving) onConfirmMove else onStartMove,
                    enabled = isMoving || (canMove && actionsEnabled),
                    style = if (isMoving) MoviesButtonStyle.Selected else MoviesButtonStyle.Secondary
                ) {
                    Icon(
                        imageVector = Icons.Default.Reorder,
                        contentDescription = stringResource(R.string.cd_reorder)
                    )
                }

                MoviesButton(
                    onClick = onEdit,
                    enabled = actionsEnabled && !isMoving,
                    modifier = Modifier
                        .tvControlHeight()
                        .then(
                            editFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier
                        )
                ) {
                    Icon(Icons.Default.Edit, stringResource(R.string.cd_edit))
                }

                MoviesButton(
                    onClick = onDelete,
                    enabled = actionsEnabled && !isMoving,
                    modifier = Modifier
                        .tvControlHeight()
                        .then(
                            deleteFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier
                        ),
                    style = MoviesButtonStyle.Destructive
                ) {
                    Icon(Icons.Default.Delete, stringResource(R.string.cd_delete))
                }
            }
        }
    }
}
