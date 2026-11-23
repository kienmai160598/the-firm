@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.core.player.SubtitleFonts
import com.kien.movies.ui.components.MoviesDialog
import com.kien.movies.ui.components.expressive.MoviesCard
import com.kien.movies.ui.screens.addon.QrCodeOverlay
import com.kien.movies.ui.screens.detail.requestFocusAfterFrames
import com.kien.movies.ui.screens.player.rememberSubtitleFontFamily
import com.kien.movies.ui.screens.player.subtitleFontLabel
import com.kien.movies.ui.theme.MoviesTheme

@Composable
internal fun SubtitleFontPickerDialog(
    selectedFontId: String,
    onFontSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: SubtitleFontsViewModel = hiltViewModel()
) {
    val fonts by viewModel.fonts.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val fontIds = SubtitleFonts.builtIn + fonts.map { it.id }
    val focusedIndex = fontIds.indexOf(selectedFontId).coerceAtLeast(0)
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = focusedIndex)
    val select: (String) -> Unit = { fontId ->
        onFontSelected(fontId)
        onDismiss()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocusAfterFrames()
    }

    MoviesDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.sub_font),
        subtitle = stringResource(R.string.sub_font_import_desc),
        width = 560.dp,
        suppressFirstKeyUp = false
    ) {
        if (selectedFontId !in fontIds) {
            Text(
                text = stringResource(R.string.sub_font_missing),
                style = MaterialTheme.typography.bodySmall,
                color = MoviesTheme.colors.Warning
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 340.dp),
            verticalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm),
            contentPadding = PaddingValues(vertical = MoviesTheme.spacing.xs)
        ) {
            items(SubtitleFonts.builtIn, key = { it }) { fontId ->
                SubtitleFontOption(
                    fontId = fontId,
                    label = subtitleFontLabel(fontId, fonts),
                    selected = fontId == selectedFontId,
                    onClick = { select(fontId) },
                    modifier = if (fontIds.indexOf(fontId) ==
                        focusedIndex
                    ) {
                        Modifier.focusRequester(focusRequester)
                    } else {
                        Modifier
                    }
                )
            }
            items(fonts, key = { it.id }) { font ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm)
                ) {
                    SubtitleFontOption(
                        fontId = font.id,
                        label = font.name,
                        selected = font.id == selectedFontId,
                        onClick = { select(font.id) },
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (fontIds.indexOf(font.id) ==
                                    focusedIndex
                                ) {
                                    Modifier.focusRequester(focusRequester)
                                } else {
                                    Modifier
                                }
                            )
                    )
                    SubtitleFontActionCard(
                        icon = Icons.Outlined.Delete,
                        label = stringResource(R.string.sub_font_remove, font.name),
                        onClick = { viewModel.remove(font, selectedFontId) }
                    )
                }
            }
            item(key = "import") {
                SubtitleFontActionCard(
                    icon = Icons.Default.QrCode2,
                    label = stringResource(R.string.sub_font_import),
                    showLabel = true,
                    onClick = viewModel::startImport,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (importState.isActive) {
        Dialog(
            onDismissRequest = viewModel::stopImport,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            QrCodeOverlay(
                qrBitmap = importState.qrCodeBitmap,
                serverUrl = importState.serverUrl,
                instruction = importState.error ?: stringResource(R.string.sub_font_import_qr),
                onClose = viewModel::stopImport
            )
        }
    }
}

@Composable
private fun SubtitleFontOption(
    fontId: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MoviesCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            },
        selected = selected
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MoviesTheme.spacing.lg, vertical = MoviesTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Aa",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = rememberSubtitleFontFamily(fontId),
                modifier = Modifier.width(52.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Spacer(modifier = Modifier.width(MoviesTheme.spacing.md))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SubtitleFontActionCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false
) {
    MoviesCard(
        onClick = onClick,
        modifier = modifier.semantics {
            role = Role.Button
            contentDescription = label
        }
    ) {
        Row(
            modifier = Modifier.padding(MoviesTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            }
            if (showLabel) {
                Spacer(modifier = Modifier.width(MoviesTheme.spacing.md))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
