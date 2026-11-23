package com.kien.movies.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.kien.movies.R
import com.kien.movies.ui.components.expressive.MoviesChip
import com.kien.movies.ui.theme.MoviesTheme

enum class ProfileEditorTab {
    Avatar,
    Background
}

@Composable
fun ProfileEditorTabs(
    selectedTab: ProfileEditorTab,
    showBackgroundTab: Boolean,
    onTabSelected: (ProfileEditorTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm)
    ) {
        ProfileEditorTabItem(
            label = stringResource(R.string.profile_editor_tab_avatar),
            isSelected = selectedTab == ProfileEditorTab.Avatar,
            onClick = { onTabSelected(ProfileEditorTab.Avatar) }
        )
        if (showBackgroundTab) {
            ProfileEditorTabItem(
                label = stringResource(R.string.profile_editor_tab_background),
                isSelected = selectedTab == ProfileEditorTab.Background,
                onClick = { onTabSelected(ProfileEditorTab.Background) }
            )
        }
    }
}

@Composable
private fun ProfileEditorTabItem(label: String, isSelected: Boolean, onClick: () -> Unit) {
    MoviesChip(
        label = label,
        selected = isSelected,
        onClick = onClick,
        showCheck = false,
        modifier = Modifier.semantics {
            role = Role.Tab
            selected = isSelected
        }
    )
}
