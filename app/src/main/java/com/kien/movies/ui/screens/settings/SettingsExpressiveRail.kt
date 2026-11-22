@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import com.kien.movies.ui.components.FocusMarqueeText
import com.kien.movies.ui.theme.ExpressiveFocus
import com.kien.movies.ui.theme.NuvioTheme
import com.kien.movies.ui.theme.expressiveFocusScale
import com.kien.movies.ui.theme.isExpressiveBuild
import com.kien.movies.ui.util.rememberRawSvgPainter

// The rail is narrow beside the detail pane, so it grows less than the top tab bar's tabs.
private const val RAIL_FOCUS_SCALE = 1.04f
private val RailIconSize = buttonIconSize(18.dp)

/**
 * The Classic style's settings rail in the top tab bar's look: a focused category fills with the
 * accent, the chosen one rests tonal, and neither draws a ring. Focus and semantics match
 * [SettingsRailButton], which the screen's focus restoration relies on.
 */
@Composable
internal fun SettingsExpressiveRailButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    focusRequester: FocusRequester?,
    onFocused: () -> Unit,
    icon: ImageVector?,
    rawIconRes: Int?,
    suppressSelectedVisual: Boolean
) {
    var isFocused by remember { mutableStateOf(false) }
    val appliedModifier =
        if (focusRequester != null) modifier.focusRequester(focusRequester) else modifier
    val container = ExpressiveFocus.containerColor(isFocused, isSelected && !suppressSelectedVisual)
    val content = ExpressiveFocus.contentColor(isFocused, isSelected)

    Card(
        onClick = onClick,
        modifier = appliedModifier
            .padding(top = NuvioTheme.spacing.xxs, bottom = NuvioTheme.spacing.xxs)
            .fillMaxWidth()
            .heightIn(min = SettingsRailItemHeight)
            .expressiveFocusScale(isFocused, RAIL_FOCUS_SCALE)
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
        colors = CardDefaults.colors(containerColor = container, focusedContainerColor = container),
        border = CardDefaults.border(border = Border.None, focusedBorder = Border.None),
        shape = CardDefaults.shape(railShape(isFocused)),
        scale = CardDefaults.scale(focusedScale = 1f, pressedScale = 1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SettingsRailItemHeight)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RailIcon(icon = icon, rawIconRes = rawIconRes, tint = content)
            FocusMarqueeText(
                text = title,
                focused = isFocused,
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics {},
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight =
                        if (isSelected || isFocused) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = content
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(RailIconSize)
            )
        }
    }
}

/** A tvOS Settings row on the box build: a fixed 12dp corner; the morphing pill elsewhere. */
@Composable
private fun railShape(focused: Boolean): Shape =
    if (isExpressiveBuild) settingsRowCornerShape() else ExpressiveFocus.morphingPill(focused)

@Composable
private fun RailIcon(icon: ImageVector?, rawIconRes: Int?, tint: Color) {
    if (rawIconRes != null) {
        Image(
            painter = rememberRawSvgPainter(rawIconRes),
            contentDescription = null,
            modifier = Modifier.size(RailIconSize),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(tint)
        )
    } else if (icon != null) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(RailIconSize)
        )
    }
}
