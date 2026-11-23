@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.screens.settings

import androidx.annotation.RawRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.domain.model.SettingsUiStyle
import com.kien.movies.ui.components.FocusMarqueeText
import com.kien.movies.ui.components.MoviesDialog
import com.kien.movies.ui.components.expressive.M3eShapes
import com.kien.movies.ui.components.expressive.MoviesButton
import com.kien.movies.ui.components.expressive.MoviesButtonStyle
import com.kien.movies.ui.components.expressive.MoviesCard
import com.kien.movies.ui.components.expressive.moviesCardBorder
import com.kien.movies.ui.screens.detail.requestFocusAfterFrames
import com.kien.movies.ui.theme.ExpressiveColors
import com.kien.movies.ui.theme.ExpressiveFocus
import com.kien.movies.ui.theme.MoviesComponents
import com.kien.movies.ui.theme.MoviesTheme
import com.kien.movies.ui.theme.tvControlHeight
import com.kien.movies.ui.util.rememberRawSvgPainter

internal val SettingsSecondaryCardRadius = MoviesComponents.tokens.settings.secondaryCardRadius
internal val SettingsRailItemHeight = MoviesComponents.tokens.settings.railItemHeight

internal val SettingsZenRowShape = RoundedCornerShape(12.dp)
internal val SettingsHorizonRowShape = RoundedCornerShape(10.dp)
internal val SettingsHorizonGroupShape = RoundedCornerShape(16.dp)

@Composable
@androidx.compose.runtime.ReadOnlyComposable
internal fun settingsGroupShape(): RoundedCornerShape = if (isFlatSettingsStyle()) {
    SettingsHorizonGroupShape
} else {
    RoundedCornerShape(SettingsSecondaryCardRadius)
}

/**
 * Marks a row that holds several options side by side. Without it, moving into the row uses a
 * geometric search and lands on whichever option sits nearest the full width row above, which is
 * the middle one. With it, coming back to the row takes the option last used, and anything else
 * takes [fallbackOption], which callers point at the first option.
 *
 * The fallback has to be named. Left at the default it hands the choice back to the same geometric
 * search, which puts focus in the middle again.
 */
internal fun Modifier.settingsOptionRow(fallbackOption: FocusRequester): Modifier = this
    .focusRestorer(fallbackOption)
    .focusGroup()

@Composable
@androidx.compose.runtime.ReadOnlyComposable
internal fun isFlatSettingsStyle(): Boolean = MoviesTheme.settingsUiStyle != SettingsUiStyle.CLASSIC

@Composable
@androidx.compose.runtime.ReadOnlyComposable
internal fun settingsHeaderInset(): Dp = when (MoviesTheme.settingsUiStyle) {
    SettingsUiStyle.CLASSIC -> 0.dp
    SettingsUiStyle.ZEN -> MoviesTheme.spacing.md
    SettingsUiStyle.HORIZON -> MoviesTheme.spacing.xs
}

@Composable
@androidx.compose.runtime.ReadOnlyComposable
internal fun settingsRowShape(): RoundedCornerShape = when (MoviesTheme.settingsUiStyle) {
    SettingsUiStyle.CLASSIC -> settingsRowCornerShape()
    SettingsUiStyle.ZEN -> SettingsZenRowShape
    SettingsUiStyle.HORIZON -> SettingsHorizonRowShape
}

internal object SettingsTestTags {
    const val ROW_ICON = "settings_row_icon"
}

@Composable
private fun SettingsRowLeadingIcon(icon: ImageVector, enabled: Boolean) {
    val contentAlpha = if (enabled) 1f else 0.4f
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = settingsRowText(contentAlpha),
        modifier = Modifier
            .size(MoviesTheme.spacing.xl)
            .testTag(SettingsTestTags.ROW_ICON)
    )
}

internal data class SettingsPickerOption<T>(
    val value: T,
    val title: String,
    val description: String? = null,
    val trailing: String? = null,
    val titleFontFamily: FontFamily? = null
)

@Composable
internal fun SettingsStandaloneScaffold(
    title: String,
    subtitle: String,
    classicContainer: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MoviesTheme.spacing.xxl, vertical = MoviesTheme.spacing.xl)
    ) {
        SettingsWorkspaceSurface(
            modifier = Modifier
                .fillMaxSize(),
            classicContainer = classicContainer
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.lg)
            ) {
                content()
            }
        }
    }
}

@Composable
internal fun SettingsWorkspaceSurface(
    modifier: Modifier = Modifier,
    classicContainer: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    TenFootTypography {
        if (!isFlatSettingsStyle() && classicContainer) {
            Box(modifier = modifier.padding(20.dp), content = content)
        } else {
            Box(
                modifier = modifier
                    .padding(
                        horizontal = MoviesTheme.spacing.md,
                        vertical = MoviesTheme.spacing.sm
                    ),
                content = content
            )
        }
    }
}

@Composable
internal fun SettingsRailButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onFocused: () -> Unit = {},
    icon: ImageVector? = null,
    rawIconRes: Int? = null,
    suppressSelectedVisual: Boolean = false
) {
    if (!isFlatSettingsStyle()) {
        SettingsExpressiveRailButton(
            title, isSelected, onClick, modifier, focusRequester, onFocused, icon, rawIconRes,
            suppressSelectedVisual
        )
        return
    }
    var isFocused by remember { mutableStateOf(false) }
    val appliedModifier = if (focusRequester != null) {
        modifier.focusRequester(focusRequester)
    } else {
        modifier
    }
    val showSelected = isSelected && !suppressSelectedVisual

    MoviesCard(
        onClick = onClick,
        modifier = appliedModifier
            .padding(top = MoviesTheme.spacing.xxs, bottom = MoviesTheme.spacing.xxs)
            .fillMaxWidth()
            .heightIn(min = SettingsRailItemHeight)
            .onFocusChanged { state ->
                val nowFocused = state.isFocused
                if (isFocused != nowFocused) {
                    isFocused = nowFocused
                    if (nowFocused) onFocused()
                }
            }
            .semantics {
                contentDescription = title
                role = Role.Tab
                this.selected = isSelected
            },
        selected = showSelected
    ) {
        val emphasis = if (isSelected || isFocused) 1f else SETTINGS_SECONDARY_TEXT_ALPHA
        val content = LocalContentColor.current.copy(alpha = emphasis)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SettingsRailItemHeight),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(16.dp)
                        .clip(M3eShapes.capsule)
                        .background(if (showSelected) content else Color.Transparent)
                )
                Spacer(modifier = Modifier.width(MoviesTheme.spacing.md))
                if (rawIconRes != null) {
                    Image(
                        painter = rememberRawSvgPainter(rawIconRes),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(content)
                    )
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = content,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (rawIconRes != null || icon != null) {
                    Spacer(modifier = Modifier.width(10.dp))
                }

                FocusMarqueeText(
                    text = title,
                    focused = isFocused,
                    modifier = Modifier.clearAndSetSemantics {},
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight =
                            if (isSelected || isFocused) FontWeight.SemiBold else FontWeight.Medium
                    ),
                    color = content
                )
            }
        }
    }
}

@Composable
internal fun SettingsTopBarTab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onFocused: () -> Unit = {},
    icon: ImageVector? = null,
    rawIconRes: Int? = null,
    onFocusedTabPositioned: ((LayoutCoordinates) -> Unit)? = null,
    suppressSelectedVisual: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    var tabCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val glideIndicator = onFocusedTabPositioned != null
    val appliedModifier = if (focusRequester != null) {
        modifier.focusRequester(focusRequester)
    } else {
        modifier
    }
    val showSelected = isSelected && !suppressSelectedVisual
    val contentColor = ExpressiveFocus.contentColor(isFocused, showSelected)

    Card(
        onClick = onClick,
        modifier = appliedModifier
            .semantics {
                role = Role.Tab
                this.selected = isSelected
            }
            .onGloballyPositioned { coordinates ->
                tabCoordinates = coordinates
                if (isFocused) onFocusedTabPositioned?.invoke(coordinates)
            }
            .onFocusChanged { state ->
                val nowFocused = state.isFocused
                if (isFocused != nowFocused) {
                    isFocused = nowFocused
                    if (nowFocused) {
                        onFocused()
                        tabCoordinates?.let { onFocusedTabPositioned?.invoke(it) }
                    }
                }
            },
        colors = CardDefaults.colors(
            containerColor = if (showSelected) ExpressiveColors.frostedStrong else Color.Transparent,
            focusedContainerColor = if (glideIndicator) Color.Transparent else ExpressiveColors.focus
        ),
        border = moviesCardBorder(),
        shape = CardDefaults.shape(M3eShapes.capsule),
        scale = CardDefaults.scale(focusedScale = 1f, pressedScale = 1f)
    ) {
        Row(
            modifier = Modifier
                .tvControlHeight()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (rawIconRes != null) {
                Image(
                    painter = rememberRawSvgPainter(rawIconRes),
                    contentDescription = null,
                    modifier = Modifier.size(buttonIconSize(16.dp)),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(contentColor)
                )
                Spacer(modifier = Modifier.width(MoviesTheme.spacing.sm))
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(buttonIconSize(16.dp))
                )
                Spacer(modifier = Modifier.width(MoviesTheme.spacing.sm))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (showSelected ||
                        isFocused
                    ) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Medium
                    }
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun SettingsDetailHeader(title: String, subtitle: String, modifier: Modifier = Modifier) {
    when (MoviesTheme.settingsUiStyle) {
        SettingsUiStyle.ZEN -> Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = settingsHeaderInset(), bottom = MoviesTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MoviesTheme.colors.TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MoviesTheme.colors.TextTertiary
            )
        }

        SettingsUiStyle.HORIZON -> Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = settingsHeaderInset(), bottom = MoviesTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.xs)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MoviesTheme.colors.TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MoviesTheme.colors.TextSecondary
            )
        }

        SettingsUiStyle.CLASSIC -> Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MoviesTheme.colors.TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MoviesTheme.colors.TextSecondary
            )
        }
    }
}

@Composable
internal fun SettingsGroupCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    when (MoviesTheme.settingsUiStyle) {
        SettingsUiStyle.ZEN, SettingsUiStyle.HORIZON -> Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = MoviesTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm)
        ) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.2.sp,
                    color = MoviesTheme.colors.TextTertiary,
                    modifier = Modifier.padding(start = MoviesTheme.spacing.lg)
                )
            }
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MoviesTheme.colors.TextTertiary.copy(alpha = 0.8f),
                    modifier = Modifier.padding(start = MoviesTheme.spacing.lg)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(settingsGroupShape())
                    .background(MoviesTheme.colors.BackgroundCard.copy(alpha = 0.55f))
                    .padding(MoviesTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.xxs)
            ) {
                content()
            }
        }

        SettingsUiStyle.CLASSIC -> SettingsExpressiveGroup(modifier, title, subtitle, content)
    }
}

@Composable
internal fun SettingsToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
    enabled: Boolean = true,
    titleTrailingIcon: ImageVector? = null,
    titleTrailingIconTint: Color = MoviesTheme.colors.TextPrimary,
    expandSubtitleOnFocus: Boolean = false
) {
    val contentAlpha = if (enabled) 1f else 0.4f
    var isFocused by remember { mutableStateOf(false) }

    MoviesCard(
        onClick = {
            if (enabled) onToggle()
        },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 62.dp)
            .focusProperties { canFocus = enabled }
            .semantics {
                if (!enabled) disabled()
                role = Role.Switch
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
            }
            .onFocusChanged { state ->
                val nowFocused = state.isFocused
                if (isFocused != nowFocused) {
                    isFocused = nowFocused
                    if (nowFocused) onFocused()
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = MoviesTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = settingsRowTitleStyle(),
                        color = settingsRowText(contentAlpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (titleTrailingIcon != null) {
                        Spacer(modifier = Modifier.width(MoviesTheme.spacing.sm))
                        Icon(
                            imageVector = titleTrailingIcon,
                            contentDescription = null,
                            tint = if (isFocused) {
                                settingsRowText(contentAlpha)
                            } else {
                                titleTrailingIconTint.copy(alpha = contentAlpha)
                            },
                            modifier = Modifier.size(MoviesTheme.spacing.lg)
                        )
                    }
                }
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(MoviesTheme.spacing.xxs))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = settingsRowText(contentAlpha, SETTINGS_SECONDARY_TEXT_ALPHA),
                        maxLines = if (expandSubtitleOnFocus && isFocused) 8 else 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(MoviesTheme.spacing.md))
            SettingsTogglePill(
                checked = checked,
                enabled = enabled,
                focused = isFocused
            )
        }
    }
}

@Composable
internal fun SettingsActionRow(
    title: String,
    subtitle: String?,
    value: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
    enabled: Boolean = true,
    trailingIcon: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
    titleTrailingIcon: ImageVector? = null,
    titleTrailingIconTint: Color = MoviesTheme.colors.TextPrimary,
    leadingIcon: ImageVector? = null,
    @RawRes leadingRawIconRes: Int? = null,
    leadingArtworkSize: Dp = MoviesTheme.spacing.xl,
    valueColor: Color = MoviesTheme.colors.TextSecondary
) {
    val contentAlpha = if (enabled) 1f else 0.4f
    var isFocused by remember { mutableStateOf(false) }

    MoviesCard(
        onClick = { if (enabled) onClick() },
        modifier = modifier
            .padding(top = MoviesTheme.spacing.xxs, bottom = MoviesTheme.spacing.xxs)
            .fillMaxWidth()
            .focusProperties { canFocus = enabled }
            .semantics {
                if (!enabled) disabled()
                role = Role.Button
            }
            .onFocusChanged { state ->
                val nowFocused = state.isFocused
                if (isFocused != nowFocused) {
                    isFocused = nowFocused
                    if (nowFocused) onFocused()
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 62.dp)
                .padding(horizontal = 18.dp, vertical = MoviesTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingRawIconRes != null) {
                Image(
                    painter = rememberRawSvgPainter(leadingRawIconRes, leadingArtworkSize),
                    contentDescription = null,
                    modifier = Modifier
                        .size(leadingArtworkSize)
                        .alpha(contentAlpha),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(MoviesTheme.spacing.lg))
            } else if (leadingIcon != null) {
                SettingsRowLeadingIcon(icon = leadingIcon, enabled = enabled)
                Spacer(modifier = Modifier.width(MoviesTheme.spacing.lg))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = settingsRowTitleStyle(),
                        color = settingsRowText(contentAlpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (titleTrailingIcon != null) {
                        Spacer(modifier = Modifier.width(MoviesTheme.spacing.sm))
                        Icon(
                            imageVector = titleTrailingIcon,
                            contentDescription = null,
                            tint = if (isFocused) {
                                settingsRowText(contentAlpha)
                            } else {
                                titleTrailingIconTint.copy(alpha = contentAlpha)
                            },
                            modifier = Modifier.size(MoviesTheme.spacing.lg)
                        )
                    }
                }
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(MoviesTheme.spacing.xxs))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = settingsRowText(contentAlpha, SETTINGS_SECONDARY_TEXT_ALPHA),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!value.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    color = settingsRowValueColor(valueColor, contentAlpha, isFocused),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 260.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = settingsRowText(contentAlpha, SETTINGS_TERTIARY_TEXT_ALPHA),
                modifier = Modifier.size(buttonIconSize(18.dp))
            )
        }
    }
}

@Composable
internal fun <T> SettingsSingleChoiceDialog(
    title: String,
    options: List<SettingsPickerOption<T>>,
    selectedValue: T,
    onOptionSelected: (T) -> Unit,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    width: Dp = 420.dp,
    maxHeight: Dp = 320.dp
) {
    val focusRequester = remember { FocusRequester() }
    val focusedIndex = options.indexOfFirst { it.value == selectedValue }
        .let { if (it >= 0) it else 0 }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = focusedIndex)

    LaunchedEffect(focusedIndex) {
        focusRequester.requestFocusAfterFrames()
    }

    MoviesDialog(
        onDismiss = onDismiss,
        title = title,
        subtitle = subtitle,
        width = width,
        suppressFirstKeyUp = false
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm),
                contentPadding = PaddingValues(vertical = MoviesTheme.spacing.xs)
            ) {
                itemsIndexed(
                    items = options,
                    key = { index, option -> "$index-${option.value}" }
                ) { index, option ->
                    val isSelected = option.value == selectedValue
                    MoviesCard(
                        onClick = { onOptionSelected(option.value) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (index ==
                                    focusedIndex
                                ) {
                                    Modifier.focusRequester(focusRequester)
                                } else {
                                    Modifier
                                }
                            )
                            .semantics {
                                role = Role.RadioButton
                                this.selected = isSelected
                            },
                        selected = isSelected
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(MoviesTheme.spacing.lg),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontFamily = option.titleFontFamily
                                )
                                if (!option.description.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(MoviesTheme.spacing.xs))
                                    Text(
                                        text = option.description,
                                        color = settingsOptionSupportingColor(),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            if (!option.trailing.isNullOrBlank()) {
                                Spacer(modifier = Modifier.width(MoviesTheme.spacing.md))
                                Text(
                                    text = option.trailing,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = settingsOptionSupportingColor()
                                )
                            }
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(MoviesTheme.spacing.md))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(buttonIconSize(20.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun <T> SettingsMultiChoiceDialog(
    title: String,
    options: List<SettingsPickerOption<T>>,
    selectedValues: List<T>,
    onValuesSelected: (List<T>) -> Unit,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    width: Dp = 520.dp,
    maxHeight: Dp = 420.dp,
    maxSelections: Int? = null
) {
    val focusRequester = remember { FocusRequester() }
    val selected =
        remember(selectedValues) { mutableStateListOf<T>().also { it.addAll(selectedValues) } }
    val firstSelectedIndex = options.indexOfFirst { option ->
        selectedValues.contains(option.value)
    }
        .let { if (it >= 0) it else 0 }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = firstSelectedIndex)

    LaunchedEffect(firstSelectedIndex) {
        focusRequester.requestFocusAfterFrames()
    }

    MoviesDialog(
        onDismiss = onDismiss,
        title = title,
        subtitle = subtitle,
        width = width,
        suppressFirstKeyUp = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm),
                    contentPadding = PaddingValues(vertical = MoviesTheme.spacing.xs)
                ) {
                    itemsIndexed(
                        items = options,
                        key = { index, option -> "$index-${option.value}" }
                    ) { index, option ->
                        val isSelected = selected.contains(option.value)
                        MoviesCard(
                            onClick = {
                                if (isSelected) {
                                    selected.remove(option.value)
                                } else if (maxSelections == null || selected.size < maxSelections) {
                                    selected.add(option.value)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (index ==
                                        firstSelectedIndex
                                    ) {
                                        Modifier.focusRequester(focusRequester)
                                    } else {
                                        Modifier
                                    }
                                )
                                .semantics {
                                    role = Role.Checkbox
                                    toggleableState =
                                        if (isSelected) ToggleableState.On else ToggleableState.Off
                                },
                            selected = isSelected
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(MoviesTheme.spacing.lg),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontFamily = option.titleFontFamily
                                    )
                                    if (!option.description.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(MoviesTheme.spacing.xs))
                                        Text(
                                            text = option.description,
                                            color = settingsOptionSupportingColor(),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(MoviesTheme.spacing.md))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(buttonIconSize(20.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }
            SettingsDialogActionRow {
                SettingsDialogActionButton(
                    text = stringResource(R.string.action_clear),
                    onClick = { selected.clear() }
                )
                SettingsDialogActionButton(
                    text = stringResource(R.string.action_save),
                    onClick = {
                        onValuesSelected(options.map { it.value }.filter { selected.contains(it) })
                    },
                    primary = true
                )
            }
        }
    }
}

@Composable
internal fun SettingsDialogActionRow(
    horizontalAlignment: Alignment.Horizontal = Alignment.End,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm, horizontalAlignment),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
internal fun SettingsDialogActionButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    MoviesButton(
        onClick = onClick,
        modifier = modifier,
        style = if (primary) MoviesButtonStyle.Primary else MoviesButtonStyle.Secondary,
        enabled = enabled
    ) {
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun SettingsChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
    multiSelect: Boolean = false
) {
    SettingsExpressiveChoiceChip(label, selected, onClick, modifier, onFocused, multiSelect)
}

@Composable
internal fun SettingsTogglePill(checked: Boolean, enabled: Boolean, focused: Boolean = false) {
    SettingsExpressiveSwitch(checked, enabled, focused)
}
