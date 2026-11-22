package com.kien.movies.ui.navigation.toptabbar

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.kien.movies.ui.theme.ExpressiveMotion
import com.kien.movies.ui.theme.MoviesTheme

/**
 * One label of the top tab bar. It draws no container of its own: [TopTabIndicator] slides
 * under it. The weight stays the same in every state so the tab never changes width while the
 * indicator is measuring it.
 */
@Composable
internal fun TopTab(
    label: String,
    selected: Boolean,
    underIndicator: Boolean,
    barFocused: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    onPressChanged: (Boolean) -> Unit,
    onPlaced: (TopTabSpan) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    LaunchedEffect(pressed) { onPressChanged(pressed) }

    val content by animateColorAsState(
        targetValue = when {
            underIndicator && barFocused -> MoviesTheme.colors.OnSecondary
            selected || underIndicator -> MoviesTheme.colors.TextPrimary
            else -> MoviesTheme.colors.TextSecondary
        },
        animationSpec = ExpressiveMotion.defaultEffects(),
        label = "topTabContent"
    )

    Box(
        modifier = modifier
            .height(TopTabHeight)
            .onPlaced { coordinates ->
                onPlaced(
                    TopTabSpan(
                        left = coordinates.positionInParent().x,
                        width = coordinates.size.width.toFloat()
                    )
                )
            }
            .onFocusChanged { state -> onFocusChanged(state.isFocused) }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics {
                role = Role.Tab
                this.selected = selected
            }
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = content,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
