package com.nuvio.tv.ui.navigation.toptabbar

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nuvio.tv.ui.components.ProfileAvatarCircle
import com.nuvio.tv.ui.theme.NuvioTheme

internal class TopTabProfile(
    val name: String,
    val colorHex: String,
    val avatarImageUrl: String?,
    val onClick: () -> Unit
)

/** The active profile's avatar; selecting it opens the profile picker. */
@Composable
internal fun TopTabProfileButton(profile: TopTabProfile, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val ring = if (focused) NuvioTheme.colors.Secondary else Color.Transparent

    Box(
        modifier = modifier
            .size(TopTabBarHeight)
            .topTabFocusScale(focused)
            .border(2.dp, ring, CircleShape)
            .onFocusChanged { focused = it.isFocused }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = profile.onClick
            )
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center
    ) {
        ProfileAvatarCircle(
            name = profile.name,
            colorHex = profile.colorHex,
            size = TopTabHeight,
            avatarImageUrl = profile.avatarImageUrl,
            imageCrossfade = false
        )
    }
}
