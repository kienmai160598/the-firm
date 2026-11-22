package com.kien.movies.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import com.kien.movies.ui.components.expressive.MoviesChip

/**
 * The Classic style's choice chip: the kit's [MoviesChip], announced as a radio button, or as a
 * checkbox when the group takes several choices ([multiSelect]).
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
    MoviesChip(
        label = label,
        selected = selected,
        onClick = onClick,
        modifier = modifier.semantics { choiceSemantics(selected, multiSelect) },
        onFocused = onFocused
    )
}

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
