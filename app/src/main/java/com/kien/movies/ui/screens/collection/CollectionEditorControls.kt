package com.kien.movies.ui.screens.collection

import androidx.tv.material3.LocalContentColor
import com.kien.movies.ui.components.expressive.MoviesSurface
import com.kien.movies.ui.components.expressive.MoviesButtonStyle
import com.kien.movies.ui.components.expressive.MoviesButton
import androidx.activity.compose.BackHandler
import com.kien.movies.ui.components.buttonLabelStyle
import com.kien.movies.ui.theme.MoviesTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.FocusDirection
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
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Switch
import androidx.tv.material3.SwitchDefaults
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
import com.kien.movies.domain.model.TraktCollectionSource
import com.kien.movies.ui.components.LoadingIndicator
import com.kien.movies.ui.components.MoviesDialog
import com.kien.movies.R
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.TextStyle
import com.kien.movies.ui.theme.tvControlHeight

private const val PLACEHOLDER_ALPHA = 0.4f
internal const val SECONDARY_CONTENT_ALPHA = 0.64f

internal suspend fun requestFocusWhenReady(
    requester: FocusRequester,
    maxFrames: Int = 8
): Boolean {
    repeat(maxFrames) {
        val focused = runCatching { requester.requestFocus() }.getOrDefault(false)
        if (focused) return true
        withFrameNanos { }
    }
    return false
}

internal fun Modifier.trapHorizontalFocus(): Modifier = this
    .focusProperties {
        onExit = {
            if (requestedFocusDirection == FocusDirection.Left ||
                requestedFocusDirection == FocusDirection.Right
            ) {
                cancelFocusChange()
            }
        }
    }
    .focusGroup()

internal fun Modifier.blockHorizontalFocus(): Modifier = this
    .focusProperties {
        left = FocusRequester.Cancel
        right = FocusRequester.Cancel
    }

internal fun Modifier.focusEntryTarget(
    target: FocusRequester
): Modifier = this
    .focusProperties {
        onEnter = {
            if (requestedFocusDirection == FocusDirection.Up ||
                requestedFocusDirection == FocusDirection.Down
            ) {
                runCatching { target.requestFocus() }
            }
        }
        onExit = {
            if (requestedFocusDirection == FocusDirection.Left ||
                requestedFocusDirection == FocusDirection.Right
            ) {
                cancelFocusChange()
            }
        }
    }
    .focusGroup()

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MoviesTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    focusRequester: FocusRequester? = null
) {
    var isEditing by remember { mutableStateOf(false) }
    val textFieldFocusRequester = remember { FocusRequester() }
    val surfaceFocusRequester = focusRequester ?: remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isEditing) {
        if (isEditing) {
            requestFocusWhenReady(textFieldFocusRequester)
        }
    }

    BackHandler(enabled = isEditing) {
        isEditing = false
        keyboardController?.hide()
        runCatching { surfaceFocusRequester.requestFocus() }
    }

    MoviesSurface(
        onClick = { isEditing = true },
        modifier = modifier.focusRequester(surfaceFocusRequester),
        scale = 1f
    ) {
        Box(modifier = Modifier.padding(MoviesTheme.spacing.md)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusProperties { canFocus = isEditing }
                    .focusRequester(textFieldFocusRequester)
                    .onFocusChanged {
                        if (!it.isFocused && isEditing) {
                            isEditing = false
                            keyboardController?.hide()
                        }
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        isEditing = false
                        keyboardController?.hide()
                        runCatching { surfaceFocusRequester.requestFocus() }
                    }
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = LocalContentColor.current,
                                            textDirection = TextDirection.Content
                ),
                cursorBrush = SolidColor(if (isEditing) MoviesTheme.colors.Primary else Color.Transparent),
                decorationBox = { innerTextField ->
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalContentColor.current.copy(alpha = PLACEHOLDER_ALPHA)
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun UnsavedChangesDialog(
    title: String,
    subtitle: String,
    canSave: Boolean,
    isSaving: Boolean = false,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    val primaryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        requestFocusWhenReady(primaryFocusRequester, maxFrames = 10)
    }

    MoviesDialog(
        onDismiss = { if (!isSaving) onDismiss() },
        title = title,
        subtitle = subtitle,
        width = 560.dp,
        suppressFirstKeyUp = false
    ) {
        BackHandler(enabled = !isSaving) { onDismiss() }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.md, Alignment.End)
        ) {
            MoviesButton(
                onClick = onDiscard,
                enabled = !isSaving,
                modifier = (if (canSave) Modifier else Modifier.focusRequester(primaryFocusRequester))
                    .semantics { role = Role.Button }
                    .tvControlHeight()
            ) {
                Text(stringResource(R.string.collections_cancel), style = buttonLabelStyle())
            }
            if (canSave) {
                MoviesButton(
                    onClick = onSave,
                    enabled = !isSaving,
                    style = MoviesButtonStyle.Primary,
                    modifier = Modifier
                        .focusRequester(primaryFocusRequester)
                        .semantics { role = Role.Button }
                        .tvControlHeight()
                ) {
                    Text(
                        stringResource(R.string.collections_editor_save),
                        style = buttonLabelStyle()
                    )
                }
            }
        }
    }
}

fun collectionSourceKeys(sources: List<CollectionSource>): List<String> {
    val seen = HashMap<String, Int>()
    return sources.map { source ->
        val key = collectionSourceKey(source)
        val occurrence = seen.getOrDefault(key, 0)
        seen[key] = occurrence + 1
        if (occurrence == 0) key else "$key#$occurrence"
    }
}

fun collectionSourceKey(source: CollectionSource): String {
    return when (source) {
        is AddonCatalogCollectionSource -> "addon_${source.addonId}_${source.type}_${source.catalogId}_${source.genre.orEmpty()}"
        is TmdbCollectionSource -> "tmdb_${source.sourceType}_${source.tmdbId}_${source.mediaType}_${source.sortBy}_${source.title}_${source.filters.hashCode()}"
        is TraktCollectionSource -> "trakt_${source.traktListId}_${source.mediaType}_${source.sortBy}_${source.sortHow}_${source.title}"
    }
}

@Composable
fun tmdbSourceSubtitle(source: TmdbCollectionSource): String {
    val media = when (source.mediaType) {
        TmdbCollectionMediaType.MOVIE -> stringResource(R.string.type_movies)
        TmdbCollectionMediaType.TV -> stringResource(R.string.type_series_plural)
    }
    val sort = when (source.sortBy) {
        TmdbCollectionSort.ORIGINAL.value -> stringResource(R.string.collections_editor_sort_original)
        TmdbCollectionSort.POPULAR_DESC.value -> stringResource(R.string.tmdb_entity_rail_popular)
        TmdbCollectionSort.VOTE_AVERAGE_DESC.value -> stringResource(R.string.tmdb_entity_rail_top_rated)
        TmdbCollectionSort.VOTE_COUNT_DESC.value -> stringResource(R.string.tmdb_entity_rail_most_voted)
        TmdbCollectionSort.RELEASE_DATE_DESC.value,
        TmdbCollectionSort.FIRST_AIR_DATE_DESC.value -> stringResource(R.string.tmdb_entity_rail_recent)
        else -> source.sortBy
    }
    return when (source.sourceType) {
        TmdbCollectionSourceType.LIST -> stringResource(R.string.collections_editor_tmdb_default_list)
        TmdbCollectionSourceType.COLLECTION -> stringResource(R.string.collections_editor_tmdb_movie_collection)
        TmdbCollectionSourceType.COMPANY -> listOf(stringResource(R.string.collections_editor_tmdb_mode_production), media, sort).joinToString(" • ")
        TmdbCollectionSourceType.NETWORK -> listOf(stringResource(R.string.collections_editor_tmdb_mode_network), stringResource(R.string.type_series_plural), sort).joinToString(" • ")
        TmdbCollectionSourceType.PERSON -> listOf(stringResource(R.string.collections_editor_tmdb_person_credits), media, sort).joinToString(" • ")
        TmdbCollectionSourceType.DIRECTOR -> listOf(stringResource(R.string.collections_editor_tmdb_director_credits), media, sort).joinToString(" • ")
        TmdbCollectionSourceType.DISCOVER -> listOf(stringResource(R.string.collections_editor_tmdb_default_discover), media, sort).joinToString(" • ")
    }
}

@Composable
fun traktSourceSubtitle(source: TraktCollectionSource): String {
    val media = when (source.mediaType) {
        TmdbCollectionMediaType.MOVIE -> stringResource(R.string.type_movies)
        TmdbCollectionMediaType.TV -> stringResource(R.string.type_series_plural)
    }
    val sort = when (source.sortBy) {
        "rank" -> stringResource(R.string.collections_editor_sort_list_order)
        "added" -> stringResource(R.string.collections_editor_sort_recently_added)
        "title" -> stringResource(R.string.collections_editor_sort_title)
        "released" -> stringResource(R.string.collections_editor_sort_released)
        "popularity" -> stringResource(R.string.tmdb_entity_rail_popular)
        "votes" -> stringResource(R.string.collections_editor_sort_votes)
        else -> source.sortBy
    }
    val direction = if (source.sortHow == "desc") {
        stringResource(R.string.collections_editor_direction_desc_short)
    } else {
        stringResource(R.string.collections_editor_direction_asc_short)
    }
    return listOf(stringResource(R.string.collections_editor_trakt_list), media, "$sort $direction").joinToString(" • ")
}
