@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.screens

import com.kien.movies.ui.components.expressive.MoviesButton
import com.kien.movies.ui.components.expressive.MoviesButtonStyle
import com.kien.movies.ui.components.expressive.MoviesCard
import com.kien.movies.ui.components.expressive.MoviesCardFocus
import com.kien.movies.ui.theme.MoviesTheme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kien.movies.domain.model.HomeLayout
import com.kien.movies.ui.components.ClassicLayoutPreview
import com.kien.movies.ui.components.GridLayoutPreview
import com.kien.movies.ui.components.ModernLayoutPreview
import com.kien.movies.ui.screens.settings.LayoutSettingsEvent
import com.kien.movies.ui.screens.settings.LayoutSettingsViewModel
import androidx.compose.ui.res.stringResource
import com.kien.movies.R
import com.kien.movies.ui.theme.metaFloor

@Composable
fun LayoutSelectionScreen(
    viewModel: LayoutSettingsViewModel = hiltViewModel(),
    onContinue: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedLayout by remember { mutableStateOf(HomeLayout.MODERN) }
    val continueFocusRequester = remember { FocusRequester() }

    LaunchedEffect(uiState.selectedLayout) {
        selectedLayout = uiState.selectedLayout
    }

    LaunchedEffect(Unit) {
        continueFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 80.dp, vertical = MoviesTheme.spacing.xxxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = stringResource(R.string.layout_selection_welcome),
                style = MaterialTheme.typography.headlineLarge,
                color = MoviesTheme.colors.TextPrimary
            )

            Spacer(modifier = Modifier.height(MoviesTheme.spacing.sm))

            Text(
                text = stringResource(R.string.layout_selection_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MoviesTheme.colors.TextSecondary
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Layout cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.xxl, Alignment.CenterHorizontally)
            ) {
                LayoutOptionCard(
                    layout = HomeLayout.MODERN,
                    isSelected = selectedLayout == HomeLayout.MODERN,
                    onSelect = { selectedLayout = HomeLayout.MODERN },
                    modifier = Modifier.weight(1f)
                )

                LayoutOptionCard(
                    layout = HomeLayout.GRID,
                    isSelected = selectedLayout == HomeLayout.GRID,
                    onSelect = { selectedLayout = HomeLayout.GRID },
                    modifier = Modifier.weight(1f)
                )

                LayoutOptionCard(
                    layout = HomeLayout.CLASSIC,
                    isSelected = selectedLayout == HomeLayout.CLASSIC,
                    onSelect = { selectedLayout = HomeLayout.CLASSIC },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(MoviesTheme.spacing.xl))

            // Continue button
            MoviesButton(
                onClick = {
                    viewModel.onEvent(LayoutSettingsEvent.SelectLayout(selectedLayout))
                    onContinue()
                },
                modifier = Modifier
                    .width(200.dp)
                    .height(MoviesTheme.spacing.xxxl)
                    .focusRequester(continueFocusRequester),
                style = MoviesButtonStyle.Primary
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.layout_selection_continue),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun LayoutOptionCard(
    layout: HomeLayout,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    MoviesCard(
        onClick = onSelect,
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused },
        selected = isSelected,
        focus = MoviesCardFocus.Frosted
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MoviesTheme.spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    when (layout) {
                        HomeLayout.CLASSIC -> ClassicLayoutPreview(
                            modifier = Modifier.fillMaxSize()
                        )
                        HomeLayout.GRID -> GridLayoutPreview(
                            modifier = Modifier.fillMaxSize()
                        )
                        HomeLayout.MODERN -> ModernLayoutPreview(
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(MoviesTheme.spacing.lg))

                Text(
                    text = when (layout) {
                        HomeLayout.CLASSIC -> stringResource(R.string.layout_classic)
                        HomeLayout.GRID -> stringResource(R.string.layout_grid)
                        HomeLayout.MODERN -> stringResource(R.string.layout_modern)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isSelected || isFocused) MoviesTheme.colors.TextPrimary else MoviesTheme.colors.TextSecondary
                )

            Spacer(modifier = Modifier.height(MoviesTheme.spacing.xs))

            Text(
                text = when (layout) {
                    HomeLayout.CLASSIC -> stringResource(R.string.layout_classic_desc)
                    HomeLayout.GRID -> stringResource(R.string.layout_grid_desc)
                    HomeLayout.MODERN -> stringResource(R.string.layout_modern_desc)
                },
                style = MaterialTheme.typography.bodySmall.metaFloor(),
                color = MoviesTheme.colors.TextTertiary
            )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.cd_selected),
                    tint = MoviesTheme.colors.FocusRing,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(MoviesTheme.spacing.sm)
                        .size(MoviesTheme.spacing.xl)
                )
            }
        }
    }
}
