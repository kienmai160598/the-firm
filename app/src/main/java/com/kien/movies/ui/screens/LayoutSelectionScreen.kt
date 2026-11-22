@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.screens

import com.kien.movies.ui.theme.ExpressiveColors
import com.kien.movies.ui.components.expressiveButtonBorder
import com.kien.movies.ui.components.expressiveButtonColors
import com.kien.movies.ui.components.expressiveButtonScale
import com.kien.movies.ui.components.expressiveButtonShape
import com.kien.movies.ui.theme.MoviesTheme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
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
import com.kien.movies.ui.theme.isExpressiveBuild
import com.kien.movies.ui.theme.metaFloor
import com.kien.movies.ui.theme.tvControlHeight

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
            Button(
                onClick = {
                    viewModel.onEvent(LayoutSettingsEvent.SelectLayout(selectedLayout))
                    onContinue()
                },
                modifier = Modifier
                    .width(200.dp)
                    .height(MoviesTheme.spacing.xxxl)
                    .focusRequester(continueFocusRequester)
                    .tvControlHeight(),
                shape = expressiveButtonShape(ButtonDefaults.shape(
                    shape = RoundedCornerShape(MoviesTheme.spacing.xl)
                )),
                colors = expressiveButtonColors(ButtonDefaults.colors(
                    containerColor = MoviesTheme.colors.BackgroundCard,
                    focusedContainerColor = MoviesTheme.colors.FocusBackground
                )),
                border = expressiveButtonBorder(ButtonDefaults.border(
                    focusedBorder = Border(
                        border = MoviesTheme.focusRing.border(MoviesTheme.spacing.xxs),
                        shape = RoundedCornerShape(MoviesTheme.spacing.xl)
                    )
                )),
                scale = expressiveButtonScale(ButtonDefaults.scale())
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.layout_selection_continue),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isExpressiveBuild) Color.Unspecified else Color.White
                    )
                }
            }
        }
    }
}

/** The chosen layout rests tonal on the expressive build, like a selected chip. */
@Composable
private fun layoutOptionContainer(selected: Boolean): Color =
    if (isExpressiveBuild && selected) {
        MoviesTheme.colors.FocusBackground
    } else {
        MoviesTheme.colors.BackgroundCard
    }

/**
 * A focused layout card on the box build brightens its frosted fill and lifts instead of drawing
 * a ring; its animated preview stays on a dark ground, so the fill does not turn solid white.
 */
@Composable
private fun layoutOptionFocusedContainer(selected: Boolean): Color =
    if (isExpressiveBuild) ExpressiveColors.frostedFocused else layoutOptionContainer(selected)

@Composable
private fun LayoutOptionCard(
    layout: HomeLayout,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        onClick = onSelect,
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused },
        colors = CardDefaults.colors(
            containerColor = layoutOptionContainer(isSelected),
            focusedContainerColor = layoutOptionFocusedContainer(isSelected)
        ),
        border = CardDefaults.border(
            border = Border.None,
            focusedBorder = if (isExpressiveBuild) {
                Border.None
            } else {
                Border(
                    border = MoviesTheme.focusRing.border(MoviesTheme.spacing.xxs),
                    shape = RoundedCornerShape(MoviesTheme.radii.xl)
                )
            }
        ),
        shape = CardDefaults.shape(RoundedCornerShape(MoviesTheme.radii.xl)),
        scale = CardDefaults.scale(focusedScale = if (isExpressiveBuild) 1.04f else 1.03f)
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
