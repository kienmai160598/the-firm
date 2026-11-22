package com.kien.movies.ui.screens.settings

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kien.movies.ui.theme.isExpressiveBuild

// Google Sans Flex at its real Medium weight sets "Watch party" and "Appearance" wider than
// upstream's 220dp rail fits next to the icon and chevron.
internal val SettingsRailWidth: Dp = if (isExpressiveBuild) 256.dp else 220.dp
