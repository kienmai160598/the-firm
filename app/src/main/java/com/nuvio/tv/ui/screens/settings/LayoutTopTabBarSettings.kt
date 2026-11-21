package com.nuvio.tv.ui.screens.settings

import androidx.compose.runtime.Composable
import com.nuvio.tv.core.build.BoxBuild

// The top tab bar replaces the sidebar, so its section goes and the Discover location row it
// held moves to Home content.
internal val discoverLocationInHomeContent: Boolean = BoxBuild.topTabBar

internal fun List<LayoutSection>.withoutSidebarForTopTabBar(
    topTabBar: Boolean = BoxBuild.topTabBar
): List<LayoutSection> = if (topTabBar) this - LayoutSection.SIDEBAR else this

@Composable
internal fun LayoutDiscoverLocationRow(
    uiState: LayoutSettingsUiState,
    onEvent: (LayoutSettingsEvent) -> Unit
) {
    DiscoverLocationRow(
        selectedLocation = uiState.discoverLocation,
        rememberedLocation = uiState.lastNonOffDiscoverLocation,
        onLocationSelected = { location -> onEvent(LayoutSettingsEvent.SetDiscoverLocation(location)) }
    )
}
