@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.screens.addon

import com.kien.movies.ui.theme.MoviesTheme

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.kien.movies.ui.components.LoadingIndicator
import com.kien.movies.ui.components.TvReorderList
import com.kien.movies.ui.screens.settings.SettingsVerticalScrollIndicators
import androidx.compose.ui.res.stringResource
import com.kien.movies.R
import kotlinx.coroutines.delay
import com.kien.movies.ui.components.expressive.M3eShapes
import com.kien.movies.ui.components.expressive.MoviesButtonStyle
import com.kien.movies.ui.components.expressive.MoviesSurface
import com.kien.movies.ui.components.expressive.moviesButtonColors
import com.kien.movies.ui.components.expressive.moviesButtonScale
import com.kien.movies.ui.components.expressive.moviesButtonShape
import com.kien.movies.ui.screens.settings.TenFootTypography
import com.kien.movies.ui.theme.tvControlHeight
import com.kien.movies.ui.theme.ExpressiveMotion

@Composable
fun CatalogOrderScreen(
    viewModel: CatalogOrderViewModel = hiltViewModel(),
    onBackPress: () -> Unit
) {
    TenFootTypography {
        CatalogOrderScreenBody(viewModel = viewModel, onBackPress = onBackPress)
    }
}

@Composable
private fun CatalogOrderScreenBody(
    viewModel: CatalogOrderViewModel,
    onBackPress: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pinToMoveNotice = stringResource(R.string.catalog_order_pin_to_move_toast)
    var pinnedNoticeMessage by remember { mutableStateOf<String?>(null) }
    var pinnedNoticeNonce by remember { mutableIntStateOf(0) }

    LaunchedEffect(pinnedNoticeNonce) {
        if (pinnedNoticeMessage != null) {
            delay(3_000)
            pinnedNoticeMessage = null
        }
    }

    BackHandler(enabled = uiState.movingKey == null) {
        onBackPress()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MoviesTheme.colors.Background),
    ) {
        when {
            uiState.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = MoviesTheme.spacing.xxxl, vertical = MoviesTheme.spacing.xl),
                ) {
                    CatalogOrderHeader(
                        followAddonsOrder = uiState.followAddonsOrder,
                        movingKey = uiState.movingKey,
                        onToggleFollowAddons = viewModel::toggleFollowAddonsOrder,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        LoadingIndicator()
                    }
                }
            }

            uiState.items.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = MoviesTheme.spacing.xxxl, vertical = MoviesTheme.spacing.xl),
                ) {
                    CatalogOrderHeader(
                        followAddonsOrder = uiState.followAddonsOrder,
                        movingKey = uiState.movingKey,
                        onToggleFollowAddons = viewModel::toggleFollowAddonsOrder,
                    )
                    Text(
                        text = stringResource(R.string.catalog_order_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MoviesTheme.colors.TextSecondary,
                    )
                }
            }

            else -> {
                val reorderScrollState = rememberScrollState()
                val movingItem = uiState.movingKey?.let { movingKey ->
                    uiState.items.firstOrNull { it.key == movingKey }
                }
                Box(modifier = Modifier.fillMaxSize()) {
                TvReorderList(
                    items = uiState.items,
                    key = { it.key },
                    canMove = { it.canMove && uiState.movingKey == null },
                    movingKey = uiState.movingKey,
                    onStartMove = viewModel::enterMoveMode,
                    onMoveBy = viewModel::moveDuringDrag,
                    onConfirmMove = { viewModel.confirmMove() },
                    onCancelMove = { viewModel.cancelMove() },
                    modifier = Modifier.fillMaxSize(),
                    scrollState = reorderScrollState,
                    contentPadding = PaddingValues(
                        start = MoviesTheme.spacing.xxxl,
                        end = MoviesTheme.spacing.xxxl,
                        top = MoviesTheme.spacing.xl,
                        bottom = MoviesTheme.spacing.xxxl,
                    ),
                    header = {
                        CatalogOrderHeader(
                            followAddonsOrder = uiState.followAddonsOrder,
                            movingKey = uiState.movingKey,
                            onToggleFollowAddons = viewModel::toggleFollowAddonsOrder,
                        )
                    },
                    useHeaderAwareBringIntoView = true,
                    scrollToTopOnEdgeItem = true,
                ) { item, isMoving, onStartMove, onConfirmMove ->
                    CatalogOrderCard(
                        item = item,
                        isMoving = isMoving,
                        onStartMove = onStartMove,
                        onConfirmMove = onConfirmMove,
                        onPinnedMoveAttempt = {
                            pinnedNoticeMessage = pinToMoveNotice
                            pinnedNoticeNonce++
                        },
                        onToggleEnabled = { viewModel.toggleCatalogEnabled(item.disableKey) },
                        actionsEnabled = uiState.movingKey == null || isMoving,
                    )
                }
                SettingsVerticalScrollIndicators(
                    state = reorderScrollState,
                    suppressBackward = movingItem != null && !movingItem.canMoveUp,
                    suppressForward = movingItem != null && !movingItem.canMoveDown,
                    forceBackward = movingItem?.canMoveUp == true,
                    forceForward = movingItem?.canMoveDown == true
                )
                }
            }
        }

        AnimatedVisibility(
            visible = pinnedNoticeMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = MoviesTheme.spacing.xxxl, vertical = MoviesTheme.spacing.xl),
        ) {
            val visibleMessage = pinnedNoticeMessage ?: return@AnimatedVisibility
            Surface(
                shape = M3eShapes.panel,
                colors = SurfaceDefaults.colors(containerColor = MoviesTheme.colors.BackgroundElevated),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = MoviesTheme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.md),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MoviesTheme.colors.Secondary,
                    )
                    Text(
                        text = visibleMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MoviesTheme.colors.TextPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogOrderHeader(
    followAddonsOrder: Boolean,
    movingKey: String?,
    onToggleFollowAddons: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
    Text(
        text = stringResource(R.string.catalog_order_title),
        style = MaterialTheme.typography.headlineLarge,
        color = MoviesTheme.colors.TextPrimary,
    )
    Spacer(modifier = Modifier.height(MoviesTheme.spacing.sm))
    Text(
        text = stringResource(R.string.catalog_order_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MoviesTheme.colors.TextSecondary,
    )
    Spacer(modifier = Modifier.height(MoviesTheme.spacing.lg))
    var isToggleFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.catalog_order_follow_addons),
                style = MaterialTheme.typography.titleMedium,
                color = MoviesTheme.colors.TextPrimary,
            )
            Text(
                text = stringResource(R.string.catalog_order_follow_addons_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MoviesTheme.colors.TextSecondary,
            )
        }
        MoviesSurface(
            onClick = {
                if (movingKey == null) {
                    onToggleFollowAddons(!followAddonsOrder)
                }
            },
            modifier = Modifier
                .semantics {
                    role = Role.Switch
                    toggleableState = if (followAddonsOrder) ToggleableState.On else ToggleableState.Off
                }
                .onFocusChanged { isToggleFocused = it.isFocused },
            shape = M3eShapes.capsule,
            resting = Color.Transparent
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                ListToggleSwitch(
                    checked = followAddonsOrder,
                    focused = isToggleFocused,
                    enabled = movingKey == null
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(MoviesTheme.spacing.lg))
    }
}

@Composable
private fun CatalogOrderCard(
    item: CatalogOrderItem,
    isMoving: Boolean,
    onStartMove: () -> Unit,
    onConfirmMove: () -> Unit,
    onPinnedMoveAttempt: () -> Unit,
    onToggleEnabled: () -> Unit,
    actionsEnabled: Boolean,
) {
    val cardColor by animateColorAsState(
        targetValue = if (isMoving) {
            MoviesTheme.colors.FocusBackground
        } else {
            MoviesTheme.colors.BackgroundCard
        },
        animationSpec = ExpressiveMotion.defaultEffects(),
        label = "catalogOrderCardColor",
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = M3eShapes.panel,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.md),
            ) {
                Column {
                    Text(
                        text = "${item.catalogName} - ${item.typeLabel.toDisplayTypeLabel()}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (item.isDisabled) MoviesTheme.colors.TextSecondary else MoviesTheme.colors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(MoviesTheme.spacing.xs))
                    val subtitle = if (item.isPinnedToTop) {
                        "${item.addonName} • ${stringResource(R.string.catalog_order_pinned_to_top)}"
                    } else {
                        item.addonName
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MoviesTheme.colors.TextSecondary
                    )
                    if (item.isDisabled) {
                        Spacer(modifier = Modifier.height(MoviesTheme.spacing.xs))
                        Text(
                            text = stringResource(R.string.catalog_order_disabled_on_home),
                            style = MaterialTheme.typography.bodySmall,
                            color = MoviesTheme.colors.Error
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val toggleLabel = stringResource(
                    if (item.isDisabled) R.string.catalog_order_enable else R.string.catalog_order_disable
                )
                var isToggleFocused by remember { mutableStateOf(false) }
                MoviesSurface(
                    onClick = {
                        if (actionsEnabled && !isMoving) onToggleEnabled()
                    },
                    modifier = Modifier
                        .semantics {
                            role = Role.Switch
                            contentDescription = toggleLabel
                            toggleableState = if (item.isDisabled) ToggleableState.Off else ToggleableState.On
                        }
                        .onFocusChanged { isToggleFocused = it.isFocused },
                    shape = M3eShapes.capsule,
                    resting = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ListToggleSwitch(checked = !item.isDisabled, focused = isToggleFocused)
                    }
                }
                if (item.isPinnedToTop) {
                    Button(
                        onClick = onPinnedMoveAttempt,
                        enabled = actionsEnabled,
                        modifier = Modifier.tvControlHeight(),
                        colors = moviesButtonColors(MoviesButtonStyle.Secondary),
                        border = reorderButtonBorder(),
                        shape = moviesButtonShape,
                        scale = moviesButtonScale
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = stringResource(R.string.catalog_order_pinned),
                        )
                    }
                } else {
                    Button(
                        onClick = if (isMoving) onConfirmMove else onStartMove,
                        enabled = isMoving || (item.canMove && actionsEnabled),
                        modifier = Modifier.tvControlHeight(),
                        colors = moviesButtonColors(
                            if (isMoving) MoviesButtonStyle.Selected else MoviesButtonStyle.Secondary
                        ),
                        border = reorderButtonBorder(),
                        shape = moviesButtonShape,
                        scale = moviesButtonScale
                    ) {
                        Icon(
                            imageVector = Icons.Default.Reorder,
                            contentDescription = stringResource(R.string.cd_reorder)
                        )
                    }
                }

            }
        }
    }
}

private fun String.toDisplayTypeLabel(): String {
    return replaceFirstChar { ch ->
        if (ch.isLowerCase()) ch.titlecase() else ch.toString()
    }
}
