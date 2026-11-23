package com.kien.movies.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kien.movies.bridge.BridgeFlows
import com.kien.movies.shell.AppRoute
import com.nuvio.tvos.TvosContinueWatchingItem
import com.nuvio.tvos.TvosContinueWatchingRow
import com.nuvio.tvos.TvosHome
import com.nuvio.tvos.TvosHomeState
import com.nuvio.tvos.TvosItem
import com.nuvio.tvos.TvosRow
import com.nuvio.tvos.TvosStreamTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn

/**
 * Home's state (tvOS HomeModel.swift). The bridge sends the whole Home as one object; it is mapped
 * to one immutable state per row, and a row that did not change keeps its previous instance, so
 * an update of one row (Continue Watching after playback, a catalog that finished loading) does
 * not recompose the others.
 */
class HomeViewModel(
    homeStates: Flow<TvosHomeState> = BridgeFlows.home(),
    private val removeCard: (key: String) -> Unit = TvosHome::removeFromContinueWatching
) : ViewModel() {
    val state: StateFlow<HomeUiState> = homeStates
        .scan(HomeUiState()) { previous, home -> homeUiState(home, previous) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    /** Removes a Continue Watching or Upcoming card; the bridge stores it and sends a new Home. */
    fun removeFromContinueWatching(key: String) = removeCard(key)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** Everything Home draws. The default is the first frame: loading, nothing yet. */
@Immutable
data class HomeUiState(
    val hero: List<HeroItem> = emptyList(),
    val watchingRows: List<WatchingRowState> = emptyList(),
    val catalogRows: List<CatalogRowState> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val hasContent: Boolean get() = watchingRows.isNotEmpty() || catalogRows.isNotEmpty()

    /** What the hero slot shows (tvOS HomeView and HomeHero `body`). */
    val heroMode: HeroMode
        get() = when {
            hero.isNotEmpty() -> HeroMode.Featured
            isLoading -> HeroMode.Loading
            !hasContent -> HeroMode.Status
            else -> HeroMode.None
        }
}

enum class HeroMode {
    /** Up to six items of the first row, with their backdrops. */
    Featured,

    /** A placeholder of the same layout, whose button holds focus until the rows arrive. */
    Loading,

    /** Loaded, nothing to show: the error or "No catalogs yet" (tvOS HomeStatusView). */
    Status,

    /** Rows loaded but the first row has no backdrop: no hero, as on tvOS. */
    None
}

/** One page of the hero. */
@Immutable
data class HeroItem(
    val metaId: String,
    val type: String,
    val name: String,
    val backdropUrl: String,
    val logoUrl: String?,
    val overview: String?,
    val facts: List<String>,
    val imdbRating: String?
) {
    val route: AppRoute.Details get() = AppRoute.Details(type = type, id = metaId)
}

/** One catalog shelf. [key] is unique in the Home list. */
@Immutable
data class CatalogRowState(val key: String, val title: String, val items: List<PosterItem>)

/** One poster; [genre] is the first genre. */
@Immutable
data class PosterItem(val metaId: String, val type: String, val name: String, val posterUrl: String?, val genre: String?) {
    val route: AppRoute.Details get() = AppRoute.Details(type = type, id = metaId)

    /** "TV Show · Comedy", shown in the focused poster's band. */
    val caption: String get() = listOfNotNull(kindLabel(type), genre).joinToString(FACT_SEPARATOR)
}

/** "TV Show" for a series, "Movie" otherwise (tvOS HeroDetails `facts`). */
internal fun kindLabel(type: String): String = if (type == "series") "TV Show" else "Movie"

private const val FACT_SEPARATOR = " · "

/** Continue Watching or Upcoming. [key] is unique in the Home list. */
@Immutable
data class WatchingRowState(val key: String, val title: String, val cards: List<WatchingCard>)

/** A Continue Watching or Upcoming card; [details] are the subtitle and the progress label. */
@Immutable
data class WatchingCard(
    val key: String,
    val title: String,
    val details: List<String>,
    val progressLabel: String?,
    val imageUrl: String?,
    val progressFraction: Float,
    val isNextUp: Boolean,
    val target: TvosStreamTarget
) {
    /** Resumes without asking, as tvOS does (AC H4). */
    val route: AppRoute.Streams get() = AppRoute.Streams(target = target, resumesDirectly = true)
}

internal const val HERO_MAX_ITEMS = 6
internal const val CONTINUE_WATCHING_KEY = "watching:continue"
internal const val UPCOMING_KEY = "watching:upcoming"
private const val CATALOG_KEY_PREFIX = "catalog:"

/** Maps the bridge's Home, keeping the row instances of [previous] that did not change. */
internal fun homeUiState(home: TvosHomeState, previous: HomeUiState = HomeUiState()): HomeUiState {
    val catalogs = home.rows.distinctBy(TvosRow::key).map(::catalogRow)
    val watching = listOfNotNull(
        home.continueWatching?.let { row -> watchingRow(CONTINUE_WATCHING_KEY, row) },
        home.upcoming?.let { row -> watchingRow(UPCOMING_KEY, row) }
    )
    return HomeUiState(
        hero = heroItems(home.rows.firstOrNull()).reusing(previous.hero, HeroItem::metaId),
        watchingRows = watching.reusing(previous.watchingRows, WatchingRowState::key),
        catalogRows = catalogs.reusing(previous.catalogRows, CatalogRowState::key),
        isLoading = home.isLoading,
        errorMessage = home.errorMessage
    )
}

/** Up to six items of the first row that have a backdrop (AC H1). */
private fun heroItems(firstRow: TvosRow?): List<HeroItem> = firstRow?.items.orEmpty()
    .distinctBy(TvosItem::metaId)
    .mapNotNull { item -> item.backdropUrl?.let { backdrop -> heroItem(item, backdrop) } }
    .take(HERO_MAX_ITEMS)

private fun heroItem(item: TvosItem, backdropUrl: String) = HeroItem(
    metaId = item.metaId,
    type = item.type,
    name = item.name,
    backdropUrl = backdropUrl,
    logoUrl = item.logoUrl,
    overview = item.overview,
    facts = heroFacts(item),
    imdbRating = item.imdbRating
)

/** Kind, release and up to two genres (tvOS HeroDetails `facts`). */
private fun heroFacts(item: TvosItem): List<String> {
    val kind = kindLabel(item.type)
    val genres = item.genres.take(2).joinToString(", ")
    return listOfNotNull(kind, item.releaseInfo?.takeIf(String::isNotBlank), genres.ifEmpty { null })
}

// A catalog can list a title twice; a lazy row needs unique keys.
private fun catalogRow(row: TvosRow) = CatalogRowState(
    key = CATALOG_KEY_PREFIX + row.key,
    title = row.title,
    items = row.items.distinctBy(TvosItem::metaId).map(::posterItem)
)

private fun posterItem(item: TvosItem) = PosterItem(
    metaId = item.metaId,
    type = item.type,
    name = item.name,
    posterUrl = item.posterUrl,
    genre = item.genres.firstOrNull()
)

private fun watchingRow(key: String, row: TvosContinueWatchingRow) = WatchingRowState(
    key = key,
    title = row.title,
    cards = row.items.distinctBy(TvosContinueWatchingItem::key).map(::watchingCard)
)

private fun watchingCard(item: TvosContinueWatchingItem) = WatchingCard(
    key = item.key,
    title = item.title,
    details = listOfNotNull(item.subtitle, item.progressLabel).filter(String::isNotEmpty),
    progressLabel = item.progressLabel,
    imageUrl = item.imageUrl,
    progressFraction = item.progressFraction,
    isNextUp = item.isNextUp,
    target = item.target
)

/** This list, with each element that equals its keyed match in [previous] replaced by that instance. */
private fun <T : Any> List<T>.reusing(previous: List<T>, key: (T) -> String): List<T> {
    if (this == previous) return previous
    val old = previous.associateBy(key)
    return map { element -> old[key(element)]?.takeIf { match -> match == element } ?: element }
}
