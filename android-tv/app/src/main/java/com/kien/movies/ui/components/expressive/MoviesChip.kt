@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.components.expressive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.FilterChipDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.SelectableChipScale
import androidx.tv.material3.Text
import com.kien.movies.ui.components.borderlessFilterChip
import com.kien.movies.ui.components.filterChipColors
import com.kien.movies.ui.components.filterChipContentColor
import com.kien.movies.ui.components.selectedCheckIcon
import com.kien.movies.ui.theme.expressiveFocusScale
import com.kien.movies.ui.theme.tvControlHeight
import com.kien.movies.ui.util.contentTextDirection

/**
 * The app's choice or filter chip: a frosted capsule, brighter with a check and a bold label
 * while [selected], solid white with black content and a small lift on focus. [onFocused] runs
 * each time it gains focus, for chips that select on focus.
 */
@Composable
internal fun MoviesChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showCheck: Boolean = true,
    isError: Boolean = false,
    onFocused: () -> Unit = {}
) {
    var focused by remember { mutableStateOf(false) }
    val content = filterChipContentColor(focused, selected, isError)

    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .expressiveFocusScale(focused)
            .tvControlHeight()
            .onFocusChanged { state ->
                if (focused != state.isFocused) {
                    focused = state.isFocused
                    if (state.isFocused) onFocused()
                }
            },
        leadingIcon = selectedCheckIcon(selected && showCheck && !isError, content),
        colors = filterChipColors(focused, selected, isError, content),
        border = borderlessFilterChip(),
        shape = FilterChipDefaults.shape(shape = M3eShapes.capsule),
        scale = SelectableChipScale.None
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                textDirection = label.contentTextDirection(),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = content
        )
    }
}
