package com.kien.movies.search

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kien.movies.design.RailSize
import com.kien.movies.shell.PlaceholderScreen
import com.kien.movies.shell.Router

/** Search tab root (tvOS SearchView.swift). Stub until slice A11; the signature stays. */
@Composable
fun SearchScreen(router: Router) {
    viewModel { SearchViewModel() }
    PlaceholderScreen(title = "Search", detail = "Coming in A11", modifier = Modifier.padding(start = RailSize.contentStart))
}
