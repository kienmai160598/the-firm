package com.kien.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Text
import com.kien.movies.ui.components.AutoResizeText
import com.kien.movies.ui.components.BrandWordmark
import com.kien.movies.ui.components.ProfileAvatarCircle
import com.kien.movies.ui.components.expressive.M3eShapes
import com.kien.movies.ui.components.expressive.MoviesSurface
import com.kien.movies.ui.theme.MoviesComponents
import com.kien.movies.ui.theme.MoviesStrokes
import com.kien.movies.ui.theme.MoviesTheme
import com.kien.movies.ui.util.contentTextDirection
import com.kien.movies.ui.util.rememberRawSvgPainter
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

private val SidebarLeadingVisualSize = MoviesComponents.tokens.sidebar.leadingVisual
private val SidebarContentGap = MoviesComponents.tokens.sidebar.contentGap
private val SidebarProfileContentGap =
    MoviesComponents.tokens.sidebar.contentGap + MoviesTheme.spacing.xs

@Composable
internal fun ModernSidebarBlurPanel(
    drawerItems: List<DrawerItem>,
    selectedDrawerRoute: String?,
    keepSidebarFocusDuringCollapse: Boolean,
    sidebarLabelAlpha: Float,
    sidebarIconScale: Float,
    sidebarExpandProgress: Float,
    isSidebarExpanded: Boolean,
    sidebarCollapsePending: Boolean,
    blurEnabled: Boolean,
    sidebarHazeState: HazeState,
    panelShape: RoundedCornerShape,
    drawerItemFocusRequesters: Map<String, FocusRequester>,
    onDrawerItemFocused: (Int) -> Unit,
    onDrawerItemClick: (String) -> Unit,
    activeProfileName: String,
    activeProfileColorHex: String,
    activeProfileAvatarImageUrl: String?,
    showProfileSelector: Boolean,
    onSwitchProfile: () -> Unit
) {
    val delayedBlurProgress =
        ((sidebarExpandProgress - 0.34f) / 0.66f).coerceIn(0f, 1f)
    val showPanelBlur = blurEnabled &&
        isSidebarExpanded &&
        !sidebarCollapsePending &&
        delayedBlurProgress > 0f
    val expandedPanelBlurModifier = if (showPanelBlur) {
        Modifier.hazeEffect(state = sidebarHazeState) {
            blurRadius = MoviesTheme.effects.blurPanel * delayedBlurProgress
            noiseFactor = 0.04f * delayedBlurProgress
            inputScale = HazeInputScale.Fixed(0.66f)
        }
    } else {
        Modifier
    }
    val colors = MoviesTheme.colors
    val bgElevated = colors.BackgroundElevated
    val bgCard = colors.BackgroundCard
    val borderBase = colors.Border
    val isAmoledBlack = bgElevated == Color.Black
    val panelBackgroundBrush = remember(blurEnabled, isAmoledBlack, bgElevated, bgCard) {
        val baseColor = if (isAmoledBlack) Color.Black else Color(0xFF161618)
        val alpha = when {
            blurEnabled -> 0.65f
            isAmoledBlack -> 1f
            else -> 0.97f
        }
        Brush.verticalGradient(
            listOf(
                baseColor.copy(alpha = alpha),
                baseColor.copy(alpha = alpha)
            )
        )
    }
    val panelBorderColor = remember(isAmoledBlack, blurEnabled, borderBase) {
        if (!blurEnabled && isAmoledBlack) borderBase.copy(alpha = 0.9f) else Color.Transparent
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .graphicsLayer {
                val p = sidebarExpandProgress
                alpha = p
                val s = 0.97f + (0.03f * p)
                scaleX = s
                scaleY = s
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .clip(panelShape)
            .then(expandedPanelBlurModifier)
            .background(brush = panelBackgroundBrush, shape = panelShape)
            .then(
                if (panelBorderColor != Color.Transparent) {
                    Modifier.border(
                        width = MoviesStrokes.tokens.hairline,
                        color = panelBorderColor,
                        shape = panelShape
                    )
                } else {
                    Modifier
                }
            )
            .padding(
                horizontal = MoviesTheme.spacing.md,
                vertical =
                    MoviesTheme.spacing.lg - MoviesTheme.spacing.xxs
            )
    ) {
        if (showProfileSelector && activeProfileName.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = MoviesTheme.spacing.md),
                contentAlignment = Alignment.Center
            ) {
                SidebarProfileItem(
                    profileName = activeProfileName,
                    profileColorHex = activeProfileColorHex,
                    profileAvatarImageUrl = activeProfileAvatarImageUrl,
                    focusEnabled = keepSidebarFocusDuringCollapse,
                    labelAlpha = sidebarLabelAlpha,
                    onFocusChanged = { focused ->
                        if (focused) onDrawerItemFocused(drawerItems.size)
                    },
                    onClick = onSwitchProfile,
                    modifier = Modifier.fillMaxWidth(0.92f)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = MoviesTheme.spacing.md),
                contentAlignment = Alignment.Center
            ) {
                BrandWordmark(
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(36.dp),
                    alpha = sidebarLabelAlpha
                )
            }
        }

        Spacer(modifier = Modifier.height(MoviesTheme.spacing.lg))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.offset(y = (-12).dp),
                verticalArrangement = Arrangement.spacedBy(
                    MoviesTheme.spacing.sm - MoviesTheme.spacing.xxs
                )
            ) {
                drawerItems.forEachIndexed { index, item ->
                    key(item.route) {
                        SidebarNavigationItem(
                            label = item.label,
                            iconRes = item.iconRes,
                            icon = item.icon,
                            selected = selectedDrawerRoute == item.route,
                            focusEnabled = keepSidebarFocusDuringCollapse,
                            labelAlpha = sidebarLabelAlpha,
                            iconScale = sidebarIconScale,
                            onFocusChanged = {
                                if (it) {
                                    onDrawerItemFocused(index)
                                }
                            },
                            onClick = { onDrawerItemClick(item.route) },
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .focusRequester(drawerItemFocusRequesters.getValue(item.route))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarNavigationItem(
    label: String,
    iconRes: Int?,
    icon: ImageVector?,
    selected: Boolean,
    focusEnabled: Boolean,
    labelAlpha: Float,
    iconScale: Float,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    MoviesSurface(
        onClick = onClick,
        modifier = modifier
            .onFocusChanged {
                onFocusChanged(it.hasFocus)
            }
            .focusProperties { canFocus = focusEnabled },
        selected = selected,
        shape = M3eShapes.capsule,
        resting = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .semantics {
                    contentDescription = label
                    role = Role.Tab
                    this.selected = selected
                }
                .fillMaxWidth()
                .padding(
                    horizontal = MoviesTheme.spacing.lg - MoviesTheme.spacing.xxs,
                    vertical =
                        MoviesTheme.spacing.sm + MoviesTheme.spacing.xxs
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(SidebarLeadingVisualSize)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
                contentAlignment = Alignment.Center
            ) {
                val iconModifier = Modifier.size(MoviesComponents.tokens.sidebar.iconSize)
                when {
                    icon != null -> Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = LocalContentColor.current,
                        modifier = iconModifier
                    )

                    iconRes != null -> Icon(
                        painter = rememberRawSvgPainter(iconRes),
                        contentDescription = null,
                        tint = LocalContentColor.current,
                        modifier = iconModifier
                    )
                }
            }
            Spacer(modifier = Modifier.width(SidebarContentGap))

            AutoResizeText(
                text = label,
                color = LocalContentColor.current,
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer { alpha = labelAlpha }
            )
        }
    }
}

@Composable
private fun SidebarProfileItem(
    profileName: String,
    profileColorHex: String,
    profileAvatarImageUrl: String?,
    focusEnabled: Boolean,
    labelAlpha: Float,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    MoviesSurface(
        onClick = onClick,
        modifier = modifier
            .onFocusChanged {
                onFocusChanged(it.hasFocus)
            }
            .focusProperties { canFocus = focusEnabled },
        shape = M3eShapes.capsule,
        resting = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .semantics {
                    contentDescription = profileName
                    role = Role.Button
                }
                .fillMaxWidth()
                .padding(
                    horizontal = MoviesTheme.spacing.lg - MoviesTheme.spacing.xxs,
                    vertical =
                        MoviesTheme.spacing.sm + MoviesTheme.spacing.xxs
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(SidebarLeadingVisualSize),
                contentAlignment = Alignment.Center
            ) {
                ProfileAvatarCircle(
                    name = profileName,
                    colorHex = profileColorHex,
                    size = SidebarLeadingVisualSize,
                    avatarImageUrl = profileAvatarImageUrl,
                    imageCrossfade = false
                )
            }
            Spacer(modifier = Modifier.width(SidebarProfileContentGap))
            AutoResizeText(
                text = profileName,
                color = LocalContentColor.current,
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer { alpha = labelAlpha },
                style = androidx.tv.material3.MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    textDirection = profileName.contentTextDirection()
                )
            )
        }
    }
}
