package com.nuvio.tv.ui.navigation.toptabbar

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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.nuvio.tv.ui.components.ProfileAvatarCircle
import com.nuvio.tv.ui.theme.focusLift

private const val AVATAR_FOCUS_SCALE = 1.12f

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

    Box(
        modifier = modifier
            .size(TopTabBarHeight)
            .focusLift(focused = focused, shape = CircleShape, scale = AVATAR_FOCUS_SCALE)
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
