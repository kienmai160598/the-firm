package com.kien.movies.ui.screens.library

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kien.movies.domain.model.LibraryListTab
import com.kien.movies.domain.model.LibraryListPrivacy
import kotlinx.coroutines.delay
import com.kien.movies.domain.model.localizedTitle
import com.kien.movies.ui.components.MoviesDialog
import com.kien.movies.ui.components.expressive.MoviesButton
import com.kien.movies.ui.components.expressive.MoviesButtonStyle
import com.kien.movies.ui.theme.MoviesTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import com.kien.movies.R
import com.kien.movies.core.tracking.TrackingListManagementCapabilities
import com.kien.movies.ui.screens.ambient.AmbientDialogEffect
import com.kien.movies.ui.screens.detail.buttonLabelStyle
import com.kien.movies.ui.screens.detail.tvButtonPadding

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun ManageListsDialog(
    tabs: List<LibraryListTab>,
    capabilities: TrackingListManagementCapabilities,
    selectedKey: String?,
    errorMessage: String?,
    pending: Boolean,
    onSelect: (String) -> Unit,
    onCreate: () -> Unit,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val personalTabs = remember(tabs) { tabs.filter { it.type == LibraryListTab.Type.PERSONAL } }
    val firstFocusRequester = remember { FocusRequester() }

    LaunchedEffect(personalTabs.size, pending) {
        if (pending) return@LaunchedEffect
        val target = firstFocusRequester
        val focused = runCatching { target.requestFocus() }.isSuccess
        if (!focused) {
            delay(16)
            runCatching { target.requestFocus() }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        AmbientDialogEffect(onDismiss)
        Box(
            modifier = Modifier
                .width(620.dp)
                .background(MoviesTheme.colors.BackgroundElevated, RoundedCornerShape(MoviesTheme.radii.xl))
                .padding(MoviesTheme.spacing.xl)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = stringResource(R.string.library_manage_lists),
                    style = MaterialTheme.typography.titleLarge,
                    color = MoviesTheme.colors.TextPrimary
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MoviesTheme.colors.Error
                    )
                }

                if (personalTabs.isEmpty()) {
                    Text(
                        text = stringResource(R.string.library_no_lists),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MoviesTheme.extendedColors.textSecondary
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(personalTabs, key = { it.key }) { tab ->
                            val selected = tab.key == selectedKey
                            MoviesButton(
                                onClick = { onSelect(tab.key) },
                                enabled = !pending,
                                modifier = (if (tab.key == personalTabs.firstOrNull()?.key) {
                                    Modifier
                                        .fillMaxWidth()
                                        .focusRequester(firstFocusRequester)
                                } else {
                                    Modifier.fillMaxWidth()
                                }).semantics {
                                    role = Role.Tab
                                    this.selected = selected
                                },
                                style = selectedButtonStyle(selected),
                                contentPadding = tvButtonPadding()
                            ) {
                                Text(
                                    text = tab.localizedTitle(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = buttonLabelStyle()
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MoviesButton(
                        onClick = onCreate,
                        modifier = if (personalTabs.isEmpty()) Modifier.focusRequester(firstFocusRequester) else Modifier,
                        enabled = !pending,
                        contentPadding = tvButtonPadding()
                    ) {
                        Text(
                            stringResource(R.string.library_list_create),
                            style = buttonLabelStyle()
                        )
                    }
                    MoviesButton(
                        onClick = onEdit,
                        enabled = !pending && selectedKey != null,
                        contentPadding = tvButtonPadding()
                    ) {
                        Text(
                            stringResource(R.string.library_list_edit),
                            style = buttonLabelStyle()
                        )
                    }
                    if (capabilities.supportsReordering) {
                        MoviesButton(
                            onClick = onMoveUp,
                            enabled = !pending && selectedKey != null,
                            contentPadding = tvButtonPadding()
                        ) {
                            Text(
                                stringResource(R.string.library_list_move_up),
                                style = buttonLabelStyle()
                            )
                        }
                        MoviesButton(
                            onClick = onMoveDown,
                            enabled = !pending && selectedKey != null,
                            contentPadding = tvButtonPadding()
                        ) {
                            Text(
                                stringResource(R.string.library_list_move_down),
                                style = buttonLabelStyle()
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MoviesButton(
                        onClick = onDelete,
                        enabled = !pending && selectedKey != null,
                        style = MoviesButtonStyle.Destructive,
                        contentPadding = tvButtonPadding()
                    ) {
                        Text(
                            stringResource(R.string.library_list_delete),
                            style = buttonLabelStyle()
                        )
                    }
                    MoviesButton(
                        onClick = onDismiss,
                        enabled = !pending,
                        contentPadding = tvButtonPadding()
                    ) {
                        Text(
                            stringResource(R.string.library_list_close),
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
internal fun ListEditorDialog(
    state: LibraryListEditorState,
    capabilities: TrackingListManagementCapabilities,
    pending: Boolean,
    onNameChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onPrivacyChanged: (LibraryListPrivacy) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val nameFocusRequester = remember { FocusRequester() }
    val descriptionFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var nameEditing by remember { mutableStateOf(false) }
    var descriptionEditing by remember { mutableStateOf(false) }

    fun isSelectKey(keyCode: Int): Boolean {
        return keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
            keyCode == AndroidKeyEvent.KEYCODE_ENTER ||
            keyCode == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER
    }

    LaunchedEffect(Unit) {
        nameFocusRequester.requestFocus()
    }

    MoviesDialog(
        onDismiss = onCancel,
        title = if (state.mode == LibraryListEditorState.Mode.CREATE) stringResource(R.string.library_list_create_dialog_title) else stringResource(R.string.library_list_edit_dialog_title),
        width = 560.dp
    ) {
        androidx.compose.material3.OutlinedTextField(
            value = state.name,
            onValueChange = onNameChanged,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(nameFocusRequester)
                .onFocusChanged {
                    if (!it.isFocused) {
                        nameEditing = false
                    }
                }
                .onPreviewKeyEvent { event ->
                    val native = event.nativeKeyEvent
                    if (native.action == AndroidKeyEvent.ACTION_DOWN && isSelectKey(native.keyCode)) {
                        nameEditing = true
                        descriptionEditing = false
                        keyboardController?.show()
                    }
                    false
                },
            enabled = !pending,
            readOnly = !nameEditing,
            singleLine = true,
            maxLines = 1,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    nameEditing = false
                    keyboardController?.hide()
                }
            ),
            label = { androidx.compose.material3.Text(stringResource(R.string.library_list_name_label)) },
            textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                textDirection = TextDirection.Content
            ),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedTextColor = MoviesTheme.colors.TextPrimary,
                unfocusedTextColor = MoviesTheme.colors.TextPrimary,
                focusedContainerColor = MoviesTheme.colors.BackgroundCard,
                unfocusedContainerColor = MoviesTheme.colors.BackgroundCard,
                focusedBorderColor = MoviesTheme.colors.FocusRing,
                unfocusedBorderColor = MoviesTheme.colors.Border,
                focusedLabelColor = MoviesTheme.colors.TextSecondary,
                unfocusedLabelColor = MoviesTheme.colors.TextTertiary,
                cursorColor = MoviesTheme.colors.FocusRing,
            )
        )

        if (capabilities.supportsDescription) {
            androidx.compose.material3.OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(descriptionFocusRequester)
                    .onFocusChanged {
                        if (!it.isFocused) {
                            descriptionEditing = false
                        }
                    }
                    .onPreviewKeyEvent { event ->
                        val native = event.nativeKeyEvent
                        if (native.action == AndroidKeyEvent.ACTION_DOWN && isSelectKey(native.keyCode)) {
                            descriptionEditing = true
                            nameEditing = false
                            keyboardController?.show()
                        }
                        false
                    },
                enabled = !pending,
                readOnly = !descriptionEditing,
                minLines = 3,
                maxLines = 5,
                label = { androidx.compose.material3.Text(stringResource(R.string.library_list_description_label)) },
                textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                    textDirection = TextDirection.Content
                ),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MoviesTheme.colors.TextPrimary,
                    unfocusedTextColor = MoviesTheme.colors.TextPrimary,
                    focusedContainerColor = MoviesTheme.colors.BackgroundCard,
                    unfocusedContainerColor = MoviesTheme.colors.BackgroundCard,
                    focusedBorderColor = MoviesTheme.colors.FocusRing,
                    unfocusedBorderColor = MoviesTheme.colors.Border,
                    focusedLabelColor = MoviesTheme.colors.TextSecondary,
                    unfocusedLabelColor = MoviesTheme.colors.TextTertiary,
                    cursorColor = MoviesTheme.colors.FocusRing,
                )
            )
        }

        Text(
            text = stringResource(R.string.library_list_privacy),
            style = MaterialTheme.typography.bodyMedium,
            color = MoviesTheme.extendedColors.textSecondary
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(capabilities.privacyOptions, key = { it.name }) { privacy ->
                val selected = privacy == state.privacy
                MoviesButton(
                    onClick = { onPrivacyChanged(privacy) },
                    enabled = !pending,
                    modifier = Modifier.semantics {
                        role = Role.RadioButton
                        // `selected` is also a local val here, so compare explicitly
                        // instead of reading the write-only semantics property.
                        this.selected = privacy == state.privacy
                    },
                    style = selectedButtonStyle(selected),
                    contentPadding = tvButtonPadding()
                ) {
                    Text(
                        privacy.apiValue.replaceFirstChar { it.uppercase() },
                        style = buttonLabelStyle()
                    )
                }
            }
        }

        MoviesButton(
            onClick = onSave,
            enabled = !pending,
            modifier = Modifier.fillMaxWidth(),
            style = MoviesButtonStyle.Primary,
            contentPadding = tvButtonPadding()
        ) {
            Text(
                if (pending) {
                    stringResource(R.string.action_saving)
                } else {
                    stringResource(R.string.action_save)
                },
                style = buttonLabelStyle()
            )
        }
    }
}

internal fun selectedButtonStyle(selected: Boolean): MoviesButtonStyle =
    if (selected) MoviesButtonStyle.Selected else MoviesButtonStyle.Secondary
