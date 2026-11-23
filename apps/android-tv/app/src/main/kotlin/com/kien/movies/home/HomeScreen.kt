package com.kien.movies.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kien.movies.design.RailSize
import com.kien.movies.design.Spacing
import com.kien.movies.shell.PlaceholderScreen
import com.kien.movies.shell.Router

/**
 * Home tab root (tvOS HomeView.swift). Stub: one line per row; selecting a catalog row opens
 * Details of its first item. Slices A4 and A5 replace the body; the signature stays.
 */
@Composable
fun HomeScreen(router: Router) {
    val viewModel = viewModel { HomeViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        HomeScreenState.Loading -> PlaceholderScreen(title = "Home", detail = "Loading")
        is HomeScreenState.Error -> PlaceholderScreen(title = "Home", detail = current.message)
        is HomeScreenState.Rows -> RowList(current.rows, router)
    }
}

@Composable
private fun RowList(rows: List<HomeRowStub>, router: Router) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag(HOME_ROWS_TAG),
        contentPadding = PaddingValues(
            start = RailSize.contentStart,
            end = Spacing.screenHorizontal,
            top = Spacing.screenVertical,
            bottom = Spacing.screenVertical
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.textGap)
    ) {
        item(key = "title", contentType = "title") {
            Text(text = "Home", style = MaterialTheme.typography.headlineSmall)
        }
        items(rows.withIndex().toList(), key = { (index, _) -> index }, contentType = { "row" }) { (_, row) ->
            ListItem(
                selected = false,
                onClick = { row.firstItem?.let(router::open) },
                headlineContent = { Text(row.title) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

const val HOME_ROWS_TAG = "home-rows"
