@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.nuvio.tv.ui.theme.ExpressiveColors

// The box build's destructive roles: a dark red container for notices and resting delete
// buttons, Netflix red with white content when a delete button takes focus.
private val Error = ExpressiveColors.accentRed
private val OnError = Color.White
private val ErrorContainer = Color(0xFF4A0F12)
private val OnErrorContainer = Color(0xFFFFDAD6)

/** A dark red container, for error banners and notices on the expressive build. */
internal val ExpressiveErrorContainer: Color = ErrorContainer
internal val ExpressiveOnErrorContainer: Color = OnErrorContainer

/** A delete button rests in the dark red container and fills with Netflix red on focus. */
@Composable
internal fun destructiveButtonColors(): ButtonColors = ButtonDefaults.colors(
    containerColor = ErrorContainer,
    contentColor = OnErrorContainer,
    focusedContainerColor = Error,
    focusedContentColor = OnError
)
