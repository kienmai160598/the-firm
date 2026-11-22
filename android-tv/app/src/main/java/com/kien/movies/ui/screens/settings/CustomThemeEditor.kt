package com.kien.movies.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.domain.model.CustomThemeColors
import com.kien.movies.domain.model.formatHexColor
import com.kien.movies.ui.components.MoviesDialog
import com.kien.movies.ui.components.expressive.MoviesButton
import com.kien.movies.ui.components.expressive.MoviesButtonStyle
import com.kien.movies.ui.components.expressive.MoviesCard
import com.kien.movies.ui.screens.detail.requestFocusAfterFrames
import com.kien.movies.ui.theme.LocalMoviesFocusRingStyle
import com.kien.movies.ui.theme.MoviesTheme
import com.kien.movies.ui.theme.ThemeColors
import com.kien.movies.ui.theme.accentBrush
import com.kien.movies.ui.theme.createFocusRingStyle
import com.kien.movies.ui.theme.toColorPalette
import com.kien.movies.ui.theme.tvControlHeight

@Composable
internal fun CustomThemeDialog(
    initialColors: CustomThemeColors,
    allowGradient: Boolean,
    onSave: (CustomThemeColors) -> Unit,
    onDismiss: () -> Unit
) {
    var gradientEnabled by remember(allowGradient) { mutableStateOf(allowGradient && !initialColors.isSolid) }
    val editorFocusRing = remember { createFocusRingStyle(ThemeColors.White) }
    CompositionLocalProvider(LocalMoviesFocusRingStyle provides editorFocusRing) {
        MoviesDialog(
            onDismiss = onDismiss,
            title = stringResource(
                if (gradientEnabled) R.string.custom_theme_title else R.string.custom_theme_solid_title
            ),
            subtitle = stringResource(
                if (gradientEnabled) R.string.custom_theme_subtitle else R.string.custom_theme_solid_subtitle
            ),
            width = 840.dp,
            usePlatformDefaultWidth = false,
            contentPadding = 20.dp,
            contentSpacing = 12.dp
        ) {
            CustomThemeEditor(
                initialColors = initialColors,
                gradientEnabled = gradientEnabled,
                onSave = onSave,
                onDismiss = onDismiss,
                onGradientChanged = if (allowGradient) { { gradientEnabled = it } } else null
            )
        }
    }
}

@Composable
internal fun CustomThemeEditor(
    initialColors: CustomThemeColors,
    gradientEnabled: Boolean,
    onSave: (CustomThemeColors) -> Unit,
    onDismiss: () -> Unit,
    onGradientChanged: ((Boolean) -> Unit)? = null
) {
    var draftColors by remember { mutableStateOf(initialColors) }
    val colors = if (gradientEnabled) draftColors else CustomThemeColors.solid(draftColors.second)
    var selectedIndex by remember(gradientEnabled) { mutableStateOf(0) }
    var showHexEditor by remember(gradientEnabled) { mutableStateOf(false) }
    var restoreHexFocus by remember(gradientEnabled) { mutableStateOf(false) }
    val firstColorFocusRequester = remember { FocusRequester() }
    val firstSwatchFocusRequester = remember { FocusRequester() }
    val modeFocusRequester = remember { FocusRequester() }
    val hexFocusRequester = remember { FocusRequester() }
    val cancelFocusRequester = remember { FocusRequester() }
    val selectedColor = colors.colors[selectedIndex]

    fun updateColor(color: Int) {
        draftColors = draftColors.withColor(if (gradientEnabled) selectedIndex else 1, color)
    }

    LaunchedEffect(Unit) {
        val focusRequester = when {
            onGradientChanged != null -> modeFocusRequester
            gradientEnabled -> firstColorFocusRequester
            else -> firstSwatchFocusRequester
        }
        focusRequester.requestFocusAfterFrames()
    }
    LaunchedEffect(showHexEditor) {
        if (!showHexEditor && restoreHexFocus) {
            restoreHexFocus = false
            hexFocusRequester.requestFocusAfterFrames()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm)) {
        if (onGradientChanged != null) {
            Row(
                modifier = Modifier.selectableGroup().settingsOptionRow(modeFocusRequester),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(true, false).forEach { gradient ->
                    val selected = gradientEnabled == gradient
                    SettingsChoiceChip(
                        label = stringResource(
                            if (gradient) R.string.custom_theme_mode_gradient else R.string.custom_theme_mode_solid
                        ),
                        selected = selected,
                        onClick = { onGradientChanged(gradient) },
                        modifier = Modifier
                            .then(if (selected) Modifier.focusRequester(modeFocusRequester) else Modifier)
                            .semantics {
                                role = Role.RadioButton
                                this.selected = selected
                            }
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
        ) {
            Column(
                modifier = Modifier.weight(1.25f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (gradientEnabled) {
                    Row(
                        modifier = Modifier.settingsOptionRow(firstColorFocusRequester),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colors.colors.forEachIndexed { index, color ->
                            ThemeColorSlot(
                                index = index,
                                color = color,
                                selected = index == selectedIndex,
                                onClick = { selectedIndex = index },
                                modifier = Modifier.weight(1f).then(
                                    if (index == 0) Modifier.focusRequester(firstColorFocusRequester) else Modifier
                                )
                            )
                        }
                    }
                }

                ThemeColorPicker(
                    color = selectedColor,
                    colorIndex = selectedIndex,
                    onColorChanged = ::updateColor,
                    firstSwatchFocusRequester = firstSwatchFocusRequester
                )

                MoviesCard(
                    onClick = { restoreHexFocus = true; showHexEditor = true },
                    modifier = Modifier.fillMaxWidth().focusRequester(hexFocusRequester)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            stringResource(R.string.custom_theme_enter_hex),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            formatHexColor(selectedColor),
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace)
                        )
                    }
                }
            }

            CustomThemeSample(colors, modifier = Modifier.weight(1f).fillMaxHeight())
        }

        Row(
            modifier = Modifier.fillMaxWidth().settingsOptionRow(cancelFocusRequester),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.custom_theme_remote_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MoviesTheme.colors.TextSecondary,
                modifier = Modifier.weight(1f)
            )
            MoviesButton(
                onClick = onDismiss,
                modifier = Modifier
                    .tvControlHeight()
                    .height(36.dp)
                    .focusRequester(cancelFocusRequester)
                    .focusProperties { up = hexFocusRequester }
            ) {
                Text(stringResource(R.string.action_cancel))
            }
            MoviesButton(
                onClick = { onSave(colors) },
                modifier = Modifier
                    .tvControlHeight()
                    .height(36.dp)
                    .focusProperties { up = hexFocusRequester },
                style = MoviesButtonStyle.Primary
            ) { Text(stringResource(R.string.custom_theme_save)) }
        }
    }

    if (showHexEditor) {
        HexColorDialog(
            initialColor = selectedColor,
            onConfirm = {
                updateColor(it)
                showHexEditor = false
            },
            onDismiss = { showHexEditor = false }
        )
    }
}

@Composable
private fun ThemeColorSlot(
    index: Int,
    color: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = stringResource(R.string.custom_theme_color_number, index + 1)
    MoviesCard(
        onClick = onClick,
        modifier = modifier.semantics {
            role = Role.RadioButton
            this.selected = selected
            contentDescription = "$label, ${formatHexColor(color)}"
        },
        selected = selected
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.size(22.dp).background(Color(color or 0xFF000000.toInt()), CircleShape))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun CustomThemeSample(colors: CustomThemeColors, modifier: Modifier = Modifier) {
    val palette = remember(colors) { colors.toColorPalette() }
    val brush = palette.accentBrush()
    val sampleShape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .background(palette.background, RoundedCornerShape(16.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            stringResource(R.string.custom_theme_preview),
            style = MaterialTheme.typography.labelMedium,
            color = MoviesTheme.colors.TextSecondary
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(BorderStroke(2.dp, brush), sampleShape)
                .background(palette.backgroundElevated, sampleShape)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.custom_theme_preview_ring),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}
