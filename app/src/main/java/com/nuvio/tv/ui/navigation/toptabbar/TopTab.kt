package com.nuvio.tv.ui.navigation.toptabbar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.nuvio.tv.ui.theme.ExpressiveMotion
import com.nuvio.tv.ui.theme.NuvioTheme

private const val FOCUSED_TAB_SCALE = 1.06f

@Composable
internal fun TopTab(
    label: String,
    selected: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val container by animateColorAsState(
        targetValue = when {
            focused -> NuvioTheme.colors.Secondary
            selected -> NuvioTheme.colors.FocusBackground
            else -> Color.Transparent
        },
        animationSpec = ExpressiveMotion.defaultEffects(),
        label = "topTabContainer"
    )
    val content by animateColorAsState(
        targetValue = when {
            focused -> NuvioTheme.colors.OnSecondary
            selected -> NuvioTheme.colors.TextPrimary
            else -> NuvioTheme.colors.TextSecondary
        },
        animationSpec = ExpressiveMotion.defaultEffects(),
        label = "topTabContent"
    )

    Box(
        modifier = modifier
            .height(TopTabHeight)
            .topTabFocusScale(focused)
            .background(container, CircleShape)
            .onFocusChanged { state ->
                if (state.isFocused && !focused) onFocused()
                focused = state.isFocused
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
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
            fontWeight = if (focused || selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Grows a tab or the profile button a little while it has focus. */
@Composable
internal fun Modifier.topTabFocusScale(focused: Boolean): Modifier {
    val scale = animateFloatAsState(
        targetValue = if (focused) FOCUSED_TAB_SCALE else 1f,
        animationSpec = ExpressiveMotion.expressiveFastSpatial(),
        label = "topTabFocusScale"
    )
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}
