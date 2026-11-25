package com.kien.movies.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.tv.material3.ClickableSurfaceColors
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ListItemColors
import androidx.tv.material3.ListItemDefaults
import com.kien.movies.design.TvColors

/**
 * A row of Settings and its pages, as the tvOS system `List` draws it: an opaque dark grey platter
 * with no glass edge, pure white with dark content when focused.
 */
object SettingsRowColors {
    @Composable
    @ReadOnlyComposable
    fun surface(): ClickableSurfaceColors = ClickableSurfaceDefaults.colors(
        containerColor = TvColors.settingsRow,
        contentColor = TvColors.textPrimary,
        focusedContainerColor = TvColors.settingsFocusedRow,
        focusedContentColor = TvColors.focusedContent,
        pressedContainerColor = TvColors.settingsFocusedRow,
        pressedContentColor = TvColors.focusedContent
    )

    @Composable
    @ReadOnlyComposable
    fun listItem(): ListItemColors = ListItemDefaults.colors(
        containerColor = TvColors.settingsRow,
        contentColor = TvColors.textPrimary,
        focusedContainerColor = TvColors.settingsFocusedRow,
        focusedContentColor = TvColors.focusedContent,
        pressedContainerColor = TvColors.settingsFocusedRow,
        pressedContentColor = TvColors.focusedContent
    )
}
