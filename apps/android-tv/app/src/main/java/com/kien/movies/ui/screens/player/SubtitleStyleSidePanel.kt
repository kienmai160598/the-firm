@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.data.local.SubtitleStyleSettings
import com.kien.movies.ui.components.expressive.M3eShapes
import com.kien.movies.ui.components.expressive.MoviesCard
import com.kien.movies.ui.components.expressive.MoviesIconButton
import com.kien.movies.ui.theme.MoviesTheme
import com.kien.movies.ui.util.keepLtr
import kotlin.math.roundToInt

private val StyleCardWidth = 220.dp
private val StyleCardHeight = 102.dp
private val StyleCardGap = MoviesTheme.spacing.md
private val StyleGridWidth = (StyleCardWidth * 3) + (StyleCardGap * 2)

@Composable
internal fun SubtitleStyleSidePanel(
    subtitleStyle: SubtitleStyleSettings,
    onEvent: (PlayerEvent) -> Unit,
    modifier: Modifier = Modifier,
    isStyleDisabledByLibass: Boolean = false,
    isEditingHdrStyle: Boolean = false
) {
    val firstItemFocusRequester = remember { FocusRequester() }
    val dispatchStyleEvent: (PlayerEvent) -> Unit = { event ->
        if (!isStyleDisabledByLibass) {
            onEvent(event)
        }
    }
    val contentModifier = if (isStyleDisabledByLibass) Modifier.alpha(0.35f) else Modifier

    LaunchedEffect(Unit) {
        if (!isStyleDisabledByLibass) {
            kotlinx.coroutines.delay(100)
            try {
                firstItemFocusRequester.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    Column(
        modifier = modifier
            .width(760.dp)
            .height(330.dp)
            .clip(RoundedCornerShape(bottomStart = PanelCorner, bottomEnd = PanelCorner))
            .background(MoviesTheme.colors.Surface)
            .then(
                if (isStyleDisabledByLibass) {
                    Modifier.focusProperties {
                        canFocus = false
                    }
                } else {
                    Modifier
                }
            )
            .padding(
                start = MoviesTheme.spacing.lg,
                end = MoviesTheme.spacing.lg,
                top = 22.dp,
                bottom = 10.dp
            )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Row(
                modifier = Modifier.width(StyleGridWidth),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.subtitle_style_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(contentModifier),
            horizontalArrangement = Arrangement.spacedBy(
                StyleCardGap,
                Alignment.CenterHorizontally
            ),
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm)) {
                SubtitleStyleSection(
                    title = stringResource(R.string.subtitle_style_font_size),
                    modifier = Modifier
                        .width(StyleCardWidth)
                        .height(StyleCardHeight)
                ) {
                    SubtitleStyleSettingRow {
                        SubtitleStyleStepperButton(
                            icon = Icons.Default.Remove,
                            contentDescription = stringResource(R.string.cd_decrease),
                            onClick = {
                                onEvent(PlayerEvent.OnSetSubtitleSize(subtitleStyle.size - 10))
                            },
                            modifier = Modifier.focusRequester(firstItemFocusRequester)
                        )
                        SubtitleStyleValueDisplay(text = "${subtitleStyle.size}%")
                        SubtitleStyleStepperButton(
                            icon = Icons.Default.Add,
                            contentDescription = stringResource(R.string.cd_increase),
                            onClick = {
                                onEvent(PlayerEvent.OnSetSubtitleSize(subtitleStyle.size + 10))
                            }
                        )
                    }
                }
                SubtitleStyleSection(
                    title = stringResource(R.string.subtitle_style_bold),
                    modifier = Modifier
                        .width(StyleCardWidth)
                        .height(StyleCardHeight)
                ) {
                    SubtitleStyleSettingRow(
                        label = stringResource(R.string.subtitle_style_weight)
                    ) {
                        SubtitleStyleToggleButton(
                            isEnabled = subtitleStyle.bold,
                            onClick = {
                                onEvent(PlayerEvent.OnSetSubtitleBold(!subtitleStyle.bold))
                            }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm)) {
                SubtitleStyleSection(
                    title = if (isEditingHdrStyle) {
                        stringResource(R.string.subtitle_style_text_color_hdr)
                    } else {
                        stringResource(R.string.subtitle_style_text_color)
                    },
                    centerContent = false,
                    modifier = Modifier
                        .width(StyleCardWidth)
                        .height(140.dp)
                ) {
                    val currentAlphaPercent = (
                        Color(
                            subtitleStyle.textColor
                        ).alpha * 100f
                        ).roundToInt().coerceIn(0, 100)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm)) {
                            SubtitleTextColorPresets.forEach { color ->
                                SubtitleStyleColorChip(
                                    color = color,
                                    isSelected =
                                        Color(subtitleStyle.textColor).copy(alpha = 1f).toArgb() ==
                                            color.copy(alpha = 1f).toArgb(),
                                    onClick = {
                                        val currentAlpha = Color(subtitleStyle.textColor).alpha
                                        onEvent(
                                            PlayerEvent.OnSetSubtitleTextColor(
                                                color.copy(alpha = currentAlpha).toArgb()
                                            )
                                        )
                                    }
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SubtitleStyleStepperButton(
                                icon = Icons.Default.Remove,
                                contentDescription = stringResource(R.string.cd_decrease),
                                onClick = {
                                    val newAlpha =
                                        (currentAlphaPercent - 10).coerceAtLeast(0) / 100f
                                    onEvent(
                                        PlayerEvent.OnSetSubtitleTextColor(
                                            Color(
                                                subtitleStyle.textColor
                                            ).copy(alpha = newAlpha).toArgb()
                                        )
                                    )
                                }
                            )
                            SubtitleStyleValueDisplay(text = "$currentAlphaPercent%")
                            SubtitleStyleStepperButton(
                                icon = Icons.Default.Add,
                                contentDescription = stringResource(R.string.cd_increase),
                                onClick = {
                                    val newAlpha =
                                        (currentAlphaPercent + 10).coerceAtMost(100) / 100f
                                    onEvent(
                                        PlayerEvent.OnSetSubtitleTextColor(
                                            Color(
                                                subtitleStyle.textColor
                                            ).copy(alpha = newAlpha).toArgb()
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
                if (isEditingHdrStyle) {
                    SubtitleStyleSection(
                        title = stringResource(R.string.subtitle_style_brightness_hdr),
                        modifier = Modifier
                            .width(StyleCardWidth)
                            .height(StyleCardHeight)
                    ) {
                        SubtitleStyleSettingRow {
                            SubtitleStyleStepperButton(
                                icon = Icons.Default.Remove,
                                onClick = {
                                    onEvent(
                                        PlayerEvent.OnSetSubtitleHdrBrightness(
                                            subtitleStyle.hdrBrightness - 10
                                        )
                                    )
                                }
                            )
                            SubtitleStyleValueDisplay(text = "${subtitleStyle.hdrBrightness}%")
                            SubtitleStyleStepperButton(
                                icon = Icons.Default.Add,
                                onClick = {
                                    onEvent(
                                        PlayerEvent.OnSetSubtitleHdrBrightness(
                                            subtitleStyle.hdrBrightness + 10
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
                SubtitleStyleSection(
                    title = stringResource(R.string.subtitle_style_outline),
                    centerContent = false,
                    modifier = Modifier
                        .width(StyleCardWidth)
                        .height(StyleCardHeight)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SubtitleStyleToggleButton(
                                isEnabled = subtitleStyle.outlineEnabled,
                                onClick = {
                                    onEvent(
                                        PlayerEvent.OnSetSubtitleOutlineEnabled(
                                            !subtitleStyle.outlineEnabled
                                        )
                                    )
                                }
                            )
                            Text(
                                text = stringResource(R.string.subtitle_style_color),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm)) {
                            SubtitleOutlineColorPresets.forEach { color ->
                                SubtitleStyleColorChip(
                                    color = color.copy(
                                        alpha = if (subtitleStyle.outlineEnabled) 1f else 0.35f
                                    ),
                                    isSelected = subtitleStyle.outlineColor == color.toArgb(),
                                    onClick = {
                                        if (!subtitleStyle.outlineEnabled) {
                                            onEvent(PlayerEvent.OnSetSubtitleOutlineEnabled(true))
                                        }
                                        onEvent(
                                            PlayerEvent.OnSetSubtitleOutlineColor(color.toArgb())
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm)) {
                SubtitleStyleSection(
                    title = stringResource(R.string.subtitle_style_bottom_offset),
                    modifier = Modifier
                        .width(StyleCardWidth)
                        .height(StyleCardHeight)
                ) {
                    SubtitleStyleSettingRow {
                        SubtitleStyleStepperButton(
                            icon = Icons.Default.Remove,
                            contentDescription = stringResource(R.string.cd_decrease),
                            onClick = {
                                onEvent(
                                    PlayerEvent.OnSetSubtitleVerticalOffset(
                                        subtitleStyle.verticalOffset - 5
                                    )
                                )
                            }
                        )
                        SubtitleStyleValueDisplay(text = subtitleStyle.verticalOffset.toString())
                        SubtitleStyleStepperButton(
                            icon = Icons.Default.Add,
                            contentDescription = stringResource(R.string.cd_increase),
                            onClick = {
                                onEvent(
                                    PlayerEvent.OnSetSubtitleVerticalOffset(
                                        subtitleStyle.verticalOffset + 5
                                    )
                                )
                            }
                        )
                    }
                }
                SubtitleStyleSection(
                    title = stringResource(R.string.subtitle_style_defaults),
                    modifier = Modifier
                        .width(StyleCardWidth)
                        .height(StyleCardHeight)
                ) {
                    MoviesCard(
                        onClick = { onEvent(PlayerEvent.OnResetSubtitleDefaults) },
                        shape = M3eShapes.capsule
                    ) {
                        Text(
                            text = stringResource(R.string.subtitle_style_reset),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(
                                horizontal = MoviesTheme.spacing.lg,
                                vertical = 10.dp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubtitleStyleSection(
    title: String,
    centerContent: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(MoviesTheme.radii.lg))
            .background(MoviesTheme.colors.BackgroundCard)
            .padding(MoviesTheme.spacing.md)
    ) {
        Column(
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(if (centerContent) Alignment.CenterStart else Alignment.TopStart)
                .padding(top = 14.dp),
            contentAlignment = if (centerContent) Alignment.CenterStart else Alignment.TopStart
        ) {
            content()
        }
    }
}

@Composable
private fun SubtitleStyleSettingRow(label: String? = null, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}

@Composable
private fun SubtitleStyleStepperButton(
    icon: ImageVector,
    contentDescription: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MoviesIconButton(
        onClick = onClick,
        modifier = modifier.size(MoviesTheme.spacing.xxl)
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(MoviesTheme.spacing.lg)
        )
    }
}

@Composable
private fun SubtitleStyleValueDisplay(text: String) {
    Box(
        modifier = Modifier
            .widthIn(min = 52.dp)
            .clip(PlayerSurfaceStyle.pillShape())
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.keepLtr(),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun SubtitleStyleColorChip(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    val isLight = (color.red + color.green + color.blue) / 3f > 0.5f
    var isFocused by remember { mutableStateOf(false) }
    val swatchLabel = stringResource(R.string.cd_color_swatch, panelSwatchColorLabel(color))

    val borderModifier = when {
        isFocused -> Modifier.border(
            MoviesTheme.focusRing.border(MoviesTheme.spacing.xxs),
            CircleShape
        )

        isSelected -> Modifier.border(MoviesTheme.spacing.xxs, Color.White, CircleShape)

        else -> Modifier
    }

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(30.dp)
            .then(borderModifier)
            .clearAndSetSemantics {
                contentDescription = swatchLabel
                role = Role.RadioButton
                this.selected = isSelected
            }
            .onFocusChanged { isFocused = it.isFocused },
        colors = IconButtonDefaults.colors(
            containerColor = color,
            focusedContainerColor = color,
            contentColor = if (isLight) Color.Black else Color.White,
            focusedContentColor = if (isLight) Color.Black else Color.White
        ),
        shape = IconButtonDefaults.shape(shape = CircleShape)
    ) {
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun SubtitleStyleToggleButton(isEnabled: Boolean, onClick: () -> Unit) {
    MoviesCard(
        onClick = onClick,
        modifier = Modifier.semantics {
            role = Role.Switch
            toggleableState = if (isEnabled) ToggleableState.On else ToggleableState.Off
        },
        selected = isEnabled,
        shape = M3eShapes.capsule
    ) {
        Text(
            text = if (isEnabled) {
                stringResource(R.string.subtitle_style_on)
            } else {
                stringResource(R.string.subtitle_style_off)
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (isEnabled) {
                LocalContentColor.current
            } else {
                LocalContentColor.current.copy(alpha = TOGGLE_OFF_CONTENT_ALPHA)
            },
            modifier = Modifier.padding(horizontal = MoviesTheme.spacing.md, vertical = 6.dp)
        )
    }
}

private const val TOGGLE_OFF_CONTENT_ALPHA = 0.55f

private val PanelCorner = 20.dp

/** Accessible name for a colour swatch: its own RGB value, as that is the only colour
 *  description available on this panel (there are no per-colour name resources). */
private fun panelSwatchColorLabel(color: Color): String {
    val rgb = color.toArgb() and 0xFFFFFF
    return "#" + rgb.toString(16).padStart(6, '0').uppercase()
}
