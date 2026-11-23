package com.kien.movies

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import com.kien.movies.bridge.BridgeFlows
import com.nuvio.tvos.TvosHomeState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MoviesApp
        val home = homeScreenStates(app)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val state by home.collectAsStateWithLifecycle(HomeScreenState.Loading)
                    HomeTitles(state)
                }
            }
        }
    }
}

/** What the bare Home screen shows: a loading text, an error text or the row titles. */
sealed interface HomeScreenState {
    data object Loading : HomeScreenState

    data class Error(val message: String) : HomeScreenState

    data class Rows(val titles: List<String>) : HomeScreenState
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun homeScreenStates(app: MoviesApp): Flow<HomeScreenState> =
    app.runtimeState.flatMapLatest { runtime ->
        when (runtime) {
            RuntimeState.Starting -> emptyFlow()
            is RuntimeState.Failed -> flowOf(
                HomeScreenState.Error(runtime.message),
            )
            RuntimeState.Ready -> BridgeFlows.home().map(::homeScreenState)
        }
    }

internal fun homeScreenState(home: TvosHomeState): HomeScreenState {
    val titles = listOfNotNull(home.continueWatching?.title, home.upcoming?.title) +
        home.rows.map { row -> row.title }
    return when {
        titles.isNotEmpty() -> HomeScreenState.Rows(titles)
        home.errorMessage != null -> HomeScreenState.Error(home.errorMessage.orEmpty())
        else -> HomeScreenState.Loading
    }
}

@Composable
private fun HomeTitles(state: HomeScreenState) {
    when (state) {
        HomeScreenState.Loading -> CenteredText("Loading")
        is HomeScreenState.Error -> CenteredText(state.message)
        is HomeScreenState.Rows -> RowTitleList(state.titles)
    }
}

@Composable
private fun CenteredText(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.titleLarge)
    }
}

// Safe region: 48 dp from the sides, 27 dp from top and bottom (docs/android-tv-rebuild.md, 2).
@Composable
private fun RowTitleList(titles: List<String>) {
    val firstItem = remember { FocusRequester() }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 27.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(titles.withIndex().toList(), key = { (index, _) -> index }) { (index, title) ->
            if (index == 0) {
                // Runs once the first item is attached, so the request always finds its node.
                LaunchedEffect(Unit) { firstItem.requestFocus() }
            }
            ListItem(
                selected = false,
                onClick = {},
                headlineContent = { Text(title) },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (index == 0) Modifier.focusRequester(firstItem) else Modifier),
            )
        }
    }
}
