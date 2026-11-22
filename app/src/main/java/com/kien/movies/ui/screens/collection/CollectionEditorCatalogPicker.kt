package com.kien.movies.ui.screens.collection

import com.kien.movies.ui.components.expressiveOptionCardBorder
import com.kien.movies.ui.components.expressiveOptionCardColors
import com.kien.movies.ui.components.expressiveOptionCardShape
import com.kien.movies.ui.theme.MoviesTheme

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Switch
import androidx.tv.material3.SwitchDefaults
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.kien.movies.domain.model.AddonCatalogCollectionSource
import com.kien.movies.domain.model.catalogTypesMatch
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
import com.kien.movies.ui.screens.settings.SettingsVerticalScrollIndicators
import com.kien.movies.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import com.kien.movies.ui.theme.bodyFloor
import com.kien.movies.ui.theme.metaFloor
import com.kien.movies.ui.theme.screenTitleFloor

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CatalogPickerContent(
    catalogs: List<AvailableCatalog>,
    alreadyAdded: List<CollectionSource>,
    onToggle: (AvailableCatalog) -> Unit,
    onBack: () -> Unit
) {
    val firstCatalogFocusRequester = remember { FocusRequester() }
    val doneButtonFocusRequester = remember { FocusRequester() }
    val pickerListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        val target = if (catalogs.isNotEmpty()) firstCatalogFocusRequester else doneButtonFocusRequester
        if (!requestFocusWhenReady(target) && target !== doneButtonFocusRequester) {
            requestFocusWhenReady(doneButtonFocusRequester)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = MoviesTheme.spacing.xxxl, start = MoviesTheme.spacing.xxxl, end = MoviesTheme.spacing.xxxl)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.collections_editor_select_catalogs),
                style = MaterialTheme.typography.headlineMedium.screenTitleFloor(),
                color = MoviesTheme.colors.TextPrimary
            )
            MoviesButton(
                onClick = onBack,
                modifier = Modifier
                    .focusRequester(doneButtonFocusRequester)
                    .focusProperties { canFocus = pickerListState.firstVisibleItemIndex == 0 }
                    .blockHorizontalFocus()
            ) { Text(stringResource(R.string.collections_editor_done)) }
        }

        Spacer(modifier = Modifier.height(MoviesTheme.spacing.lg))

        Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = pickerListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = MoviesTheme.spacing.sm, end = MoviesTheme.spacing.sm, top = MoviesTheme.spacing.xs, bottom = MoviesTheme.spacing.xxxl),
            verticalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm)
        ) {
            itemsIndexed(
                items = catalogs,
                key = { index, c -> "${c.addonId}_${c.type}_${c.catalogId}_$index" }
            ) { index, catalog ->
                val isAdded = alreadyAdded.any {
                    it is AddonCatalogCollectionSource && it.addonId == catalog.addonId && catalogTypesMatch(it.type, catalog.type) && it.catalogId == catalog.catalogId
                }
                Card(
                    onClick = { onToggle(catalog) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .blockHorizontalFocus()
                        .then(if (index == 0) Modifier.focusRequester(firstCatalogFocusRequester) else Modifier)
                        .semantics {
                            role = Role.Checkbox
                            toggleableState = if (isAdded) ToggleableState.On else ToggleableState.Off
                        },
                    colors = expressiveOptionCardColors(CardDefaults.colors(
                        containerColor = if (isAdded) MoviesTheme.colors.Secondary.copy(alpha = 0.15f) else MoviesTheme.colors.BackgroundCard,
                        focusedContainerColor = MoviesTheme.colors.FocusBackground
                    ), selected = isAdded),
                    border = expressiveOptionCardBorder(CardDefaults.border(
                        border = if (isAdded) Border(
                            border = BorderStroke(MoviesTheme.spacing.hairline, MoviesTheme.colors.Secondary.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(MoviesTheme.radii.md)
                        ) else Border.None,
                        focusedBorder = Border(
                            border = MoviesTheme.focusRing.border(MoviesTheme.spacing.xxs),
                            shape = RoundedCornerShape(MoviesTheme.radii.md)
                        )
                    )),
                    shape = expressiveOptionCardShape(
                        CardDefaults.shape(RoundedCornerShape(MoviesTheme.radii.md))
                    ),
                    scale = CardDefaults.scale(focusedScale = 1.01f)
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
                                text = catalog.catalogName.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleSmall.bodyFloor(),
                                color = MoviesTheme.colors.TextPrimary
                            )
                            val supportingGenreText = when {
                                catalog.genreRequired -> stringResource(R.string.collections_editor_genre_required)
                                catalog.genreOptions.isNotEmpty() -> stringResource(R.string.collections_editor_genre_optional)
                                else -> null
                            }
                            Text(
                                text = listOfNotNull("${catalog.type} - ${catalog.addonName}", supportingGenreText).joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall.metaFloor(),
                                color = MoviesTheme.colors.TextTertiary
                            )
                        }
                        if (isAdded) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = MoviesTheme.colors.TextSecondary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MoviesTheme.colors.TextTertiary
                            )
                        }
                    }
                }
            }
        }
        SettingsVerticalScrollIndicators(state = pickerListState)
        }
    }
}
