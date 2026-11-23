package com.kien.movies.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.ui.components.expressive.MoviesCard
import com.kien.movies.ui.theme.MoviesComponents
import com.kien.movies.ui.theme.MoviesTheme

private val NavItemIconShape = RoundedCornerShape(MoviesComponents.tokens.sidebar.panelRadius / 3)

data class SidebarItem(val route: String, val label: String, val icon: ImageVector)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SidebarNavigation(
    items: List<SidebarItem>,
    selectedRoute: String?,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    focusRequester: FocusRequester,
    onFocusChange: (Boolean) -> Unit,
    onNavigate: (String) -> Unit
) {
    val sidebarWidth = MoviesTheme.sizes.sidebar.expandedWidth
    val sidebarAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        label = "sidebarAlpha"
    )

    Column(
        modifier = Modifier
            .width(sidebarWidth)
            .fillMaxHeight()
            .graphicsLayer { alpha = sidebarAlpha }
            .background(MoviesTheme.colors.BackgroundElevated)
            .padding(vertical = MoviesTheme.spacing.xl, horizontal = MoviesTheme.spacing.lg)
            .onFocusChanged { state ->
                onFocusChange(state.hasFocus)
                onExpandedChange(state.hasFocus)
            },
        verticalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.md)
    ) {
        Text(
            text = stringResource(R.string.app_name).uppercase(),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MoviesTheme.colors.Primary
        )

        Spacer(modifier = Modifier.height(MoviesTheme.spacing.md))

        items.forEach { item ->
            SidebarNavItem(
                item = item,
                isSelected = item.route == selectedRoute,
                focusRequester = if (item.route == selectedRoute) focusRequester else null,
                onNavigate = onNavigate
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SidebarNavItem(
    item: SidebarItem,
    isSelected: Boolean,
    focusRequester: FocusRequester?,
    onNavigate: (String) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    MoviesCard(
        onClick = { onNavigate(item.route) },
        modifier = Modifier
            .fillMaxWidth()
            .height(MoviesTheme.sizes.settings.railItemHeight)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { state ->
                isFocused = state.hasFocus
            },
        selected = isSelected
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(MoviesTheme.sizes.settings.railItemHeight)
                .padding(horizontal = MoviesTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(MoviesTheme.sizes.icons.xl - MoviesTheme.spacing.xs)
                    .clip(NavItemIconShape)
                    .background(MoviesTheme.colors.SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = MoviesTheme.colors.TextPrimary,
                    modifier = Modifier.size(MoviesTheme.sizes.icons.sm)
                )
            }

            Text(
                text = item.label,
                style = MaterialTheme.typography.titleMedium,
                color = LocalContentColor.current.copy(
                    alpha = if (isFocused ||
                        isSelected
                    ) {
                        1f
                    } else {
                        0.64f
                    }
                )
            )
        }
    }
}
