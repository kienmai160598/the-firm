package com.kien.movies.ui.components

import com.kien.movies.ui.components.expressive.M3eShapes
import com.kien.movies.ui.theme.expressiveFocusScale
import com.kien.movies.ui.util.contentTextDirection

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.FilterChipDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.SelectableChipScale
import androidx.tv.material3.Text

enum class SourceChipStatus {
    LOADING,
    SUCCESS,
    ERROR
}

data class SourceChipItem(
    val name: String,
    val status: SourceChipStatus
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SourceStatusFilterChip(
    name: String,
    isSelected: Boolean,
    status: SourceChipStatus = SourceChipStatus.SUCCESS,
    isSelectable: Boolean = true,
    onClick: () -> Unit,
    onFocusSelect: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val isError = status == SourceChipStatus.ERROR
    val isLoading = status == SourceChipStatus.LOADING
    val shakeOffsetPx = remember { Animatable(0f) }
    val alphaPulse = remember { Animatable(1f) }

    LaunchedEffect(isError) {
        shakeOffsetPx.snapTo(0f)
        alphaPulse.snapTo(1f)
        if (!isError) return@LaunchedEffect

        playErrorNudge(alphaPulse, shakeOffsetPx)
    }

    val textColor = filterChipContentColor(isFocused, isSelected, isError)

    FilterChip(
        selected = isSelected,
        onClick = { if (isSelectable) onClick() },
        modifier = modifier
            .expressiveFocusScale(isFocused)
            .graphicsLayer {
                alpha = alphaPulse.value
                translationX = shakeOffsetPx.value
            }
            .onFocusChanged {
                val nowFocused = it.isFocused
                isFocused = nowFocused
                if (nowFocused && !isSelected && isSelectable) {
                    onFocusSelect?.invoke()
                }
            },
        leadingIcon = selectedCheckIcon(isSelected && !isError, textColor),
        colors = filterChipColors(isFocused, isSelected, isError, textColor),
        border = borderlessFilterChip(),
        shape = FilterChipDefaults.shape(shape = M3eShapes.capsule),
        scale = SelectableChipScale.None
    ) {
        Text(
            text = name,
            modifier = Modifier.shimmer(isLoading),
            style = MaterialTheme.typography.labelLarge.copy(
                textDirection = name.contentTextDirection()
            ),
            color = textColor
        )
    }
}
