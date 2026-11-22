@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.screens.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kien.movies.ui.theme.ExpressiveFocus
import com.kien.movies.ui.theme.MoviesTheme
import com.kien.movies.ui.theme.expressiveFocusScale
import com.kien.movies.ui.theme.isExpressiveBuild
import com.kien.movies.ui.theme.tvControlHeight
import com.kien.movies.ui.util.contentTextDirection

/**
 * The Classic style's choice chip as an M3 Expressive pill: a chosen chip rests tonal with a bold
 * label, a focused one fills with the accent, grows and squares its corners a little.
 */
@Composable
internal fun SettingsExpressiveChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    onFocused: () -> Unit,
    multiSelect: Boolean
) {
    var isFocused by remember { mutableStateOf(false) }
    val container = ExpressiveFocus.containerColor(isFocused, selected, chipRestingFill())
    val content = ExpressiveFocus.contentColor(isFocused, selected)

    Card(
        onClick = onClick,
        modifier = modifier
            .expressiveFocusScale(isFocused)
            .tvControlHeight()
            .onFocusChanged { state ->
                val nowFocused = state.isFocused
                if (isFocused != nowFocused) {
                    isFocused = nowFocused
                    if (nowFocused) onFocused()
                }
            }
            .semantics { choiceSemantics(selected, multiSelect) },
        colors = CardDefaults.colors(containerColor = container, focusedContainerColor = container),
        border = CardDefaults.border(border = Border.None, focusedBorder = Border.None),
        shape = CardDefaults.shape(chipShape(isFocused)),
        scale = CardDefaults.scale(focusedScale = 1f, pressedScale = 1f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                textDirection = label.contentTextDirection(),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = content,
            modifier = Modifier.padding(horizontal = MoviesTheme.spacing.lg, vertical = 10.dp)
        )
    }
}

@Composable
private fun chipRestingFill(): Color =
    if (isExpressiveBuild) SettingsRowRestFill else MoviesTheme.colors.Background

@Composable
private fun chipShape(focused: Boolean): Shape =
    settingsCapsuleOr(ExpressiveFocus.morphingPill(focused))

private fun SemanticsPropertyReceiver.choiceSemantics(
    selected: Boolean,
    multiSelect: Boolean
) {
    if (multiSelect) {
        role = Role.Checkbox
        toggleableState = if (selected) ToggleableState.On else ToggleableState.Off
    } else {
        role = Role.RadioButton
        this.selected = selected
    }
}
