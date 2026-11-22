package com.kien.movies.ui.screens.collection

import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.tv.material3.LocalContentColor
import com.kien.movies.ui.components.expressive.MoviesSwitch
import com.kien.movies.ui.components.expressive.MoviesCard
import com.kien.movies.ui.components.expressive.MoviesButtonStyle
import com.kien.movies.ui.components.expressive.MoviesButton
import com.kien.movies.ui.components.buttonLabelStyle
import com.kien.movies.ui.theme.MoviesTheme

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import coil3.compose.AsyncImage
import com.kien.movies.domain.model.AddonCatalogCollectionSource
import com.kien.movies.domain.model.CollectionFolder
import com.kien.movies.domain.model.CollectionSource
import com.kien.movies.domain.model.FolderViewMode
import com.kien.movies.domain.model.PosterShape
import com.kien.movies.domain.model.TmdbCollectionFilters
import com.kien.movies.domain.model.TmdbCollectionMediaType
import com.kien.movies.domain.model.TmdbCollectionSort
import com.kien.movies.domain.model.TmdbCollectionSource
import com.kien.movies.domain.model.TmdbCollectionSourceType
import com.kien.movies.ui.components.LoadingIndicator
import com.kien.movies.ui.components.MoviesDialog
import com.kien.movies.ui.components.TvReorderList
import com.kien.movies.ui.screens.settings.SettingsVerticalScrollIndicators
import com.kien.movies.ui.theme.bodyFloor
import com.kien.movies.ui.theme.metaFloor
import com.kien.movies.ui.theme.screenTitleFloor
import com.kien.movies.ui.theme.tvControlHeight
import com.kien.movies.ui.util.contentTextDirection
import com.kien.movies.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.TextStyle

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CollectionEditorScreen(
    viewModel: CollectionEditorViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val foldersScrollState = rememberScrollState()
    val folderRowHeightCache = remember { mutableMapOf<String, Float>() }
    var lastFocusedFolderTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingFocusTarget by remember { mutableStateOf<String?>(null) }
    var hasAppliedInitialFocus by remember { mutableStateOf(false) }

    val titleFocusRequester = remember { FocusRequester() }
    val addFolderFocusRequester = remember { FocusRequester() }
    val folderFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val folderDeleteFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    var folderToDelete by remember { mutableStateOf<CollectionFolder?>(null) }
    val deleteDialogFocusRequester = remember { FocusRequester() }
    var showUnsavedCollectionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.folders) {
        val validIds = uiState.folders.mapTo(mutableSetOf()) { it.id }
        folderFocusRequesters.keys.retainAll(validIds)
        folderDeleteFocusRequesters.keys.retainAll(validIds)
    }

    LaunchedEffect(folderToDelete) {
        if (folderToDelete != null) {
            requestFocusWhenReady(deleteDialogFocusRequester)
        }
    }

    val activeFocusTarget = pendingFocusTarget ?: if (!hasAppliedInitialFocus) {
        lastFocusedFolderTarget ?: "title"
    } else {
        null
    }
    val ensureComposedFolderId = when {
        activeFocusTarget == null -> null
        activeFocusTarget.startsWith("folder_edit:") -> activeFocusTarget.removePrefix("folder_edit:")
        activeFocusTarget.startsWith("folder_delete:") -> activeFocusTarget.removePrefix("folder_delete:")
        else -> null
    }

    LaunchedEffect(uiState.isLoading, uiState.showFolderEditor, folderToDelete, showUnsavedCollectionDialog, pendingFocusTarget, uiState.folders.size) {
        if (uiState.isLoading || uiState.showFolderEditor || folderToDelete != null || showUnsavedCollectionDialog) {
            return@LaunchedEffect
        }
        val target = pendingFocusTarget ?: if (!hasAppliedInitialFocus) {
            lastFocusedFolderTarget ?: "title"
        } else {
            return@LaunchedEffect
        }
        val requester = when {
            target == "title" -> titleFocusRequester
            target == "add_folder" -> addFolderFocusRequester
            target.startsWith("folder_edit:") -> {
                val id = target.removePrefix("folder_edit:")
                if (uiState.folders.any { it.id == id }) {
                    folderFocusRequesters.getOrPut(id) { FocusRequester() }
                } else {
                    addFolderFocusRequester
                }
            }
            target.startsWith("folder_delete:") -> {
                val id = target.removePrefix("folder_delete:")
                if (uiState.folders.any { it.id == id }) {
                    folderDeleteFocusRequesters.getOrPut(id) { FocusRequester() }
                } else {
                    addFolderFocusRequester
                }
            }
            else -> titleFocusRequester
        }
        val focused = requestFocusWhenReady(requester)
        if (!focused && requester !== titleFocusRequester) {
            requestFocusWhenReady(addFolderFocusRequester) || requestFocusWhenReady(titleFocusRequester)
        }
        hasAppliedInitialFocus = true
        if (pendingFocusTarget == target) {
            pendingFocusTarget = null
        }
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingIndicator()
        }
        return
    }

    if (uiState.showFolderEditor) {
        FolderEditorContent(
            viewModel = viewModel,
            uiState = uiState
        )
        return
    }

    BackHandler(
        enabled = (uiState.hasUnsavedCollectionChanges || uiState.isSaving) &&
            uiState.movingFolderKey == null &&
            folderToDelete == null &&
            !showUnsavedCollectionDialog
    ) {
        if (!uiState.isSaving) {
            showUnsavedCollectionDialog = true
        }
    }

    val foldersMovingIndex = uiState.folders.indexOfFirst { it.id == uiState.movingFolderKey }
    Box(modifier = Modifier.fillMaxSize()) {
    TvReorderList(
        items = uiState.folders,
        key = { it.id },
        canMove = { uiState.folders.size > 1 && uiState.movingFolderKey == null },
        movingKey = uiState.movingFolderKey,
        onStartMove = viewModel::enterFolderMoveMode,
        onMoveBy = viewModel::moveFolderDuringDrag,
        onConfirmMove = { viewModel.confirmFolderMove() },
        onCancelMove = viewModel::cancelFolderMove,
        modifier = Modifier.fillMaxSize(),
        scrollState = foldersScrollState,
        ensureComposedKey = ensureComposedFolderId,
        rowHeightCache = folderRowHeightCache,
        contentPadding = PaddingValues(
            top = MoviesTheme.spacing.xxxl,
            start = MoviesTheme.spacing.xxxl,
            end = MoviesTheme.spacing.xxxl,
            bottom = MoviesTheme.spacing.xxxl
        ),
        header = {
            Column {
                Text(
                    text = if (uiState.isNew) stringResource(R.string.collections_new) else stringResource(R.string.collections_editor_edit_collection),
                    style = MaterialTheme.typography.headlineMedium
                        .screenTitleFloor()
                        .copy(textDirection = TextDirection.Content),
                    color = MoviesTheme.colors.TextPrimary,
                )
                Spacer(modifier = Modifier.height(MoviesTheme.spacing.xl))

                Text(
                    text = stringResource(R.string.collections_editor_row_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MoviesTheme.colors.TextSecondary
                )
                Spacer(modifier = Modifier.height(MoviesTheme.spacing.sm))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusEntryTarget(titleFocusRequester),
                    horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MoviesTextField(
                        value = uiState.title,
                        onValueChange = { viewModel.setTitle(it) },
                        modifier = Modifier.weight(1f),
                        placeholder = stringResource(R.string.collections_editor_placeholder_name),
                        focusRequester = titleFocusRequester
                    )
                    MoviesButton(onClick = { viewModel.save { onBack() } }, enabled = uiState.canSaveCollection) {
                        Text(stringResource(R.string.collections_editor_save))
                    }
                }
                Spacer(modifier = Modifier.height(MoviesTheme.spacing.lg))

                Text(
                    text = stringResource(R.string.collections_editor_backdrop),
                    style = MaterialTheme.typography.labelLarge,
                    color = MoviesTheme.colors.TextSecondary
                )
                Spacer(modifier = Modifier.height(MoviesTheme.spacing.sm))
                MoviesTextField(
                    value = uiState.backdropImageUrl,
                    onValueChange = { viewModel.setBackdropImageUrl(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .blockHorizontalFocus(),
                    placeholder = stringResource(R.string.collections_editor_placeholder_backdrop)
                )
                Spacer(modifier = Modifier.height(MoviesTheme.spacing.lg))

                val pinToTopRowInteraction = remember { MutableInteractionSource() }
                val pinToTopRowFocused by pinToTopRowInteraction.collectIsFocusedAsState()
                MoviesCard(
                    onClick = { viewModel.setPinToTop(!uiState.pinToTop) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .blockHorizontalFocus()
                        .semantics {
                            role = Role.Switch
                            toggleableState = if (uiState.pinToTop) ToggleableState.On else ToggleableState.Off
                        },
                    interactionSource = pinToTopRowInteraction
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.collections_editor_pin_above),
                                style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Content)
                            )
                            Spacer(modifier = Modifier.height(MoviesTheme.spacing.xs))
                            Text(
                                text = stringResource(R.string.collections_editor_pin_above_desc),
                                style = MaterialTheme.typography.bodySmall.metaFloor(),
                                color = LocalContentColor.current.copy(alpha = SECONDARY_CONTENT_ALPHA)
                            )
                        }
                        Spacer(modifier = Modifier.width(MoviesTheme.spacing.md))
                        MoviesSwitch(checked = uiState.pinToTop, enabled = true, focused = pinToTopRowFocused)
                    }
                }
                Spacer(modifier = Modifier.height(MoviesTheme.spacing.lg))

                val focusGlowEnabledRowInteraction = remember { MutableInteractionSource() }
                val focusGlowEnabledRowFocused by focusGlowEnabledRowInteraction.collectIsFocusedAsState()
                MoviesCard(
                    onClick = { viewModel.setFocusGlowEnabled(!uiState.focusGlowEnabled) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .blockHorizontalFocus()
                        .semantics {
                            role = Role.Switch
                            toggleableState = if (uiState.focusGlowEnabled) ToggleableState.On else ToggleableState.Off
                        },
                    interactionSource = focusGlowEnabledRowInteraction
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.collections_editor_focus_glow),
                                style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Content)
                            )
                            Spacer(modifier = Modifier.height(MoviesTheme.spacing.xs))
                            Text(
                                text = stringResource(R.string.collections_editor_focus_glow_desc),
                                style = MaterialTheme.typography.bodySmall.metaFloor(),
                                color = LocalContentColor.current.copy(alpha = SECONDARY_CONTENT_ALPHA)
                            )
                        }
                        Spacer(modifier = Modifier.width(MoviesTheme.spacing.md))
                        MoviesSwitch(checked = uiState.focusGlowEnabled, enabled = true, focused = focusGlowEnabledRowFocused)
                    }
                }
                Spacer(modifier = Modifier.height(MoviesTheme.spacing.lg))

                Text(
                    text = stringResource(R.string.collections_editor_view_mode),
                    style = MaterialTheme.typography.labelLarge,
                    color = MoviesTheme.colors.TextSecondary
                )
                Spacer(modifier = Modifier.height(MoviesTheme.spacing.sm))
                val viewModeFocusRequesters = remember {
                    FolderViewMode.entries.associateWith { FocusRequester() }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusEntryTarget(viewModeFocusRequesters[uiState.viewMode] ?: FocusRequester.Default),
                    horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm)
                ) {
                    val viewModes = listOf(
                        FolderViewMode.TABBED_GRID to stringResource(R.string.collections_editor_view_mode_tabs),
                        FolderViewMode.ROWS to stringResource(R.string.collections_editor_view_mode_rows),
                        FolderViewMode.FOLLOW_LAYOUT to stringResource(R.string.collections_editor_view_mode_follow)
                    )
                    viewModes.forEach { (mode, label) ->
                        val isSelected = uiState.viewMode == mode
                        MoviesButton(
                            onClick = { viewModel.setViewMode(mode) },
                            modifier = Modifier
                                .focusRequester(viewModeFocusRequesters.getValue(mode))
                                .semantics {
                                    role = Role.RadioButton
                                    selected = isSelected
                                }.tvControlHeight(),
                            style = if (isSelected) MoviesButtonStyle.Selected else MoviesButtonStyle.Secondary
                        ) {
                            Text(label)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(MoviesTheme.spacing.md))

            if (uiState.viewMode == FolderViewMode.TABBED_GRID) {
                    val showAllTabRowInteraction = remember { MutableInteractionSource() }
                    val showAllTabRowFocused by showAllTabRowInteraction.collectIsFocusedAsState()
                    MoviesCard(
                        onClick = { viewModel.setShowAllTab(!uiState.showAllTab) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .blockHorizontalFocus()
                            .semantics {
                                role = Role.Switch
                                toggleableState = if (uiState.showAllTab) ToggleableState.On else ToggleableState.Off
                            },
                        interactionSource = showAllTabRowInteraction
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.collections_editor_show_all_tab),
                                    style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Content)
                                )
                                Spacer(modifier = Modifier.height(MoviesTheme.spacing.xs))
                                Text(
                                    text = stringResource(R.string.collections_editor_show_all_tab_desc),
                                    style = MaterialTheme.typography.bodySmall.metaFloor(),
                                    color = LocalContentColor.current.copy(alpha = SECONDARY_CONTENT_ALPHA)
                                )
                            }
                            Spacer(modifier = Modifier.width(MoviesTheme.spacing.md))
                            MoviesSwitch(checked = uiState.showAllTab, enabled = true, focused = showAllTabRowFocused)
                        }
                    }
                    Spacer(modifier = Modifier.height(MoviesTheme.spacing.md))
            }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.collections_editor_folders),
                        style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Content),
                        color = MoviesTheme.colors.TextPrimary,
                    )
                    Text(
                        text = stringResource(
                            if (uiState.folders.size == 1) R.string.collection_editor_folder_count_one
                            else R.string.collection_editor_folder_count_other,
                            uiState.folders.size
                        ),
                        style = MaterialTheme.typography.bodySmall.metaFloor(),
                        color = MoviesTheme.colors.TextTertiary
                    )
                }
                Spacer(modifier = Modifier.height(MoviesTheme.spacing.md))
            }
        },
        footer = {
            Box(
                modifier = Modifier.padding(
                    start = MoviesTheme.spacing.sm,
                    end = MoviesTheme.spacing.sm,
                    top = if (uiState.folders.isEmpty()) MoviesTheme.spacing.xs else MoviesTheme.spacing.lg
                )
            ) {
                MoviesButton(
                    onClick = {
                        lastFocusedFolderTarget = "add_folder"
                        pendingFocusTarget = "add_folder"
                        viewModel.addFolder()
                    },
                    enabled = uiState.movingFolderKey == null,
                    modifier = Modifier
                        .focusRequester(addFolderFocusRequester)
                        .blockHorizontalFocus()
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(MoviesTheme.spacing.sm))
                    Text(stringResource(R.string.collections_editor_add_folder))
                }
            }
        },
    ) { folder, isMoving, onStartMove, onConfirmMove ->
        val editFocusRequester = folderFocusRequesters.getOrPut(folder.id) { FocusRequester() }
        val deleteFocusRequester = folderDeleteFocusRequesters.getOrPut(folder.id) { FocusRequester() }
        Box(modifier = Modifier.padding(start = MoviesTheme.spacing.sm, end = MoviesTheme.spacing.sm)) {
            FolderListItem(
                folder = folder,
                isMoving = isMoving,
                canMove = uiState.folders.size > 1,
                actionsEnabled = uiState.movingFolderKey == null || isMoving,
                editFocusRequester = editFocusRequester,
                deleteFocusRequester = deleteFocusRequester,
                onEdit = {
                    lastFocusedFolderTarget = "folder_edit:${folder.id}"
                    pendingFocusTarget = "folder_edit:${folder.id}"
                    viewModel.editFolder(folder.id)
                },
                onDelete = { folderToDelete = folder },
                onStartMove = onStartMove,
                onConfirmMove = onConfirmMove
            )
        }
    }
    SettingsVerticalScrollIndicators(
        state = foldersScrollState,
        suppressBackward = foldersMovingIndex == 0,
        suppressForward = foldersMovingIndex >= 0 &&
            foldersMovingIndex == uiState.folders.lastIndex,
        forceBackward = foldersMovingIndex > 0,
        forceForward = foldersMovingIndex in 0 until uiState.folders.lastIndex
    )
    }

    folderToDelete?.let { folder ->
        MoviesDialog(
            onDismiss = {
                folderToDelete = null
                pendingFocusTarget = "folder_delete:${folder.id}"
            },
            title = stringResource(R.string.collections_editor_delete_folder_title),
            subtitle = stringResource(R.string.collections_editor_delete_folder_subtitle, folder.title),
            suppressFirstKeyUp = false
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.md, Alignment.End)
            ) {
                MoviesButton(
                    onClick = {
                        folderToDelete = null
                        pendingFocusTarget = "folder_delete:${folder.id}"
                    },
                    modifier = Modifier
                        .focusRequester(deleteDialogFocusRequester)
                        .semantics { role = Role.Button }
                        .tvControlHeight()
                ) {
                    Text(stringResource(R.string.collections_cancel), style = buttonLabelStyle())
                }
                MoviesButton(
                    onClick = {
                        val currentIndex = uiState.folders.indexOfFirst { it.id == folder.id }
                        val nextTargetId = uiState.folders
                            .filterNot { it.id == folder.id }
                            .let { remaining ->
                                if (remaining.isEmpty()) null
                                else remaining[currentIndex.coerceIn(0, remaining.lastIndex)].id
                            }
                        viewModel.removeFolder(folder.id)
                        folderToDelete = null
                        val nextTarget = nextTargetId?.let { "folder_edit:$it" } ?: "add_folder"
                        lastFocusedFolderTarget = nextTarget
                        pendingFocusTarget = nextTarget
                    },
                    modifier = Modifier.semantics { role = Role.Button }.tvControlHeight(),
                    style = MoviesButtonStyle.Destructive
                ) {
                    Text(stringResource(R.string.cd_delete), style = buttonLabelStyle())
                }
            }
        }
    }

    if (showUnsavedCollectionDialog) {
        val canSave = uiState.canSaveCollection
        val dialogTitle = stringResource(
            if (canSave) R.string.collections_editor_unsaved_title
            else R.string.collections_editor_unsaved_incomplete_title
        )
        val dialogSubtitle = stringResource(
            when {
                canSave -> R.string.collections_editor_unsaved_subtitle
                uiState.title.isBlank() && uiState.folders.isEmpty() ->
                    R.string.collections_editor_unsaved_missing_title_and_folder
                uiState.title.isBlank() -> R.string.collections_editor_unsaved_missing_title
                else -> R.string.collections_editor_unsaved_missing_folder
            }
        )
        UnsavedChangesDialog(
            title = dialogTitle,
            subtitle = dialogSubtitle,
            canSave = canSave,
            isSaving = uiState.isSaving,
            onSave = {
                showUnsavedCollectionDialog = false
                viewModel.save { onBack() }
            },
            onDiscard = {
                showUnsavedCollectionDialog = false
                onBack()
            },
            onDismiss = { showUnsavedCollectionDialog = false }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FolderListItem(
    folder: CollectionFolder,
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
        label = "folderCardColor",
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
                .padding(MoviesTheme.spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.title,
                    style = MaterialTheme.typography.titleSmall.bodyFloor().copy(
                        textDirection = folder.title.contentTextDirection()
                    ),
                    color = MoviesTheme.colors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val shapeLabel = stringResource(
                    when (folder.tileShape) {
                        com.kien.movies.domain.model.PosterShape.POSTER -> R.string.collections_editor_shape_poster
                        com.kien.movies.domain.model.PosterShape.LANDSCAPE -> R.string.collections_editor_shape_wide
                        com.kien.movies.domain.model.PosterShape.SQUARE -> R.string.collections_editor_shape_square
                    }
                )
                Text(
                    text = "$shapeLabel - ${stringResource(R.string.collections_editor_source_count, folder.sources.size)}",
                    style = MaterialTheme.typography.bodySmall.metaFloor(),
                    color = MoviesTheme.colors.TextTertiary
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm),
                modifier = Modifier.trapHorizontalFocus()
            ) {
                MoviesButton(
                    onClick = if (isMoving) onConfirmMove else onStartMove,
                    enabled = isMoving || (canMove && actionsEnabled),
                    modifier = Modifier.semantics {
                        role = Role.Button
                    }.tvControlHeight(),
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
