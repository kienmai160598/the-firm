package com.kien.movies.ui.screens.settings

import androidx.compose.runtime.Composable
import com.kien.movies.ui.components.expressive.MoviesSwitch

@Composable
internal fun SettingsExpressiveSwitch(checked: Boolean, enabled: Boolean, focused: Boolean) =
    MoviesSwitch(checked = checked, enabled = enabled, focused = focused)
