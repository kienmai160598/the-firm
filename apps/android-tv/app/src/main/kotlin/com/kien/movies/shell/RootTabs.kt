package com.kien.movies.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.DrawerState
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.Icon
import androidx.tv.material3.ModalNavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.Text
import com.kien.movies.design.Palette
import com.kien.movies.design.RailSize
import com.kien.movies.design.Spacing

/**
 * The sidebar around the tab roots (tvOS RootTabs.swift `SidebarTabs`, decision 2):
 * tv-material's ModalNavigationDrawer, a rail of five icons when closed, icons and labels on an
 * opaque panel over one flat scrim when open. The drawer opens when focus enters it and closes
 * when focus leaves; entering always lands on the selected tab.
 *
 * Tab content draws under the closed rail: its backgrounds may run to the edge, its focusable
 * content starts at [RailSize.contentStart].
 */
@Composable
fun RootTabs(
    selected: RootTab,
    drawerState: DrawerState,
    selectedTabFocus: FocusRequester,
    onSelect: (RootTab) -> Unit,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimBrush = SolidColor(Color(0x80000000)),
        drawerContent = { value ->
            val panel = if (value == DrawerValue.Open) Modifier.background(Palette.drawerPanel) else Modifier
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .then(panel)
                    .padding(start = 36.dp, end = DRAWER_END_PADDING, top = Spacing.screenVertical, bottom = Spacing.screenVertical)
                    .selectableGroup()
                    .focusProperties { onEnter = { selectedTabFocus.requestFocus() } }
                    .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
            ) {
                RootTab.entries.forEach { tab ->
                    val isSelected = tab == selected
                    NavigationDrawerItem(
                        selected = isSelected,
                        onClick = { onSelect(tab) },
                        leadingContent = { Icon(painter = painterResource(tab.icon), contentDescription = null) },
                        modifier = Modifier
                            .testTag(tabTag(tab))
                            .then(if (isSelected) Modifier.focusRequester(selectedTabFocus) else Modifier)
                    ) {
                        Text(text = tab.title)
                    }
                }
            }
        },
        content = content
    )
}

/** Test tag of a sidebar item, e.g. `tab-addons`. */
fun tabTag(tab: RootTab): String = "tab-${tab.name.lowercase()}"

private val DRAWER_END_PADDING = 12.dp
