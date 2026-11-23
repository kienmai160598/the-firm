package com.kien.movies.addons

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kien.movies.design.RailSize
import com.kien.movies.shell.PlaceholderScreen

/** Addons tab root (tvOS AddonsView.swift). Stub until slice A12; the signature stays. */
@Composable
fun AddonsScreen() {
    viewModel { AddonsViewModel() }
    PlaceholderScreen(title = "Addons", detail = "Coming in A12", modifier = Modifier.padding(start = RailSize.contentStart))
}
