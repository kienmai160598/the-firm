package com.kien.movies.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.tv.material3.LocalTextStyle
import com.kien.movies.ui.theme.bodyFloor

/** A label inside a tv-material Button or Card: its text style, at least 16sp. */
@Composable
@ReadOnlyComposable
internal fun buttonLabelStyle(): TextStyle = LocalTextStyle.current.bodyFloor()
