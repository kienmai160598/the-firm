package com.kien.movies.ui.components

import com.kien.movies.ui.theme.bodyFloor
import com.kien.movies.ui.theme.tvControlHeight
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.Role
import androidx.tv.material3.Text
import com.kien.movies.ui.theme.ExpressiveFocus
import com.kien.movies.ui.theme.MoviesTheme
import com.kien.movies.ui.theme.expressiveFocusScale
import com.kien.movies.ui.theme.isExpressiveBuild

/** A text button that answers focus the tvOS way: frosted capsule at rest, white and lifted on focus. */
@Composable
internal fun ExpressivePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val container = ExpressiveFocus.containerColor(
        focused = focused,
        resting = if (isExpressiveBuild) {
            ExpressiveFocus.frostedContainer
        } else {
            MoviesTheme.colors.BackgroundCard
        }
    )
    val content = ExpressiveFocus.contentColor(
        focused = focused,
        resting = MoviesTheme.colors.TextPrimary
    )

    Box(
        modifier = modifier
            .expressiveFocusScale(focused)
            .background(container, ExpressiveFocus.morphingPill(focused))
            .onFocusChanged { focused = it.isFocused }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .tvControlHeight()
            .padding(
                horizontal = MoviesTheme.spacing.xl,
                vertical = MoviesTheme.spacing.md
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = MoviesTheme.textStyles.button.bodyFloor(), color = content)
    }
}
