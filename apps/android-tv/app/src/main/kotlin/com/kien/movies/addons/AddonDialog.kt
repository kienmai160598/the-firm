package com.kien.movies.addons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.kien.movies.design.CardSize
import com.kien.movies.design.GlassStyle
import com.kien.movies.design.Palette
import com.kien.movies.design.Spacing
import com.kien.movies.design.TextStyles

/**
 * The confirmation dialogs of Addons (tvOS `confirmationDialog` and `alert`): a title, an optional
 * message and buttons, on one opaque panel. tv-material 1.1.0 has no dialog, so this is a Compose
 * `Dialog` window; it owns Back (dismiss) while it is open. The window dims what is behind it
 * once, as tvOS does.
 */
@Composable
fun AddonDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    width: Dp = DIALOG_WIDTH,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .width(width)
                .background(Palette.drawerPanel, RoundedCornerShape(CardSize.cornerRadius))
                .padding(DIALOG_PADDING),
            verticalArrangement = Arrangement.spacedBy(Spacing.textGapLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = TextStyles.shelfHeader, color = Palette.textPrimary, textAlign = TextAlign.Center)
            if (message != null) {
                Text(text = message, style = TextStyles.small, color = Palette.textSecondary, textAlign = TextAlign.Center)
            }
            content()
        }
    }
}

/** One full width action of an [AddonDialog]. [focusFirst] puts focus on it when the dialog opens. */
@Composable
fun AddonDialogButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, focusFirst: Boolean = false) {
    val requester = remember { FocusRequester() }
    Button(
        onClick = onClick,
        colors = GlassStyle.buttonColors(),
        scale = GlassStyle.buttonScale(),
        shape = GlassStyle.buttonShape(),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(requester)
            .testTag(dialogButtonTag(label))
    ) {
        Text(text = label, style = TextStyles.button, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
    if (focusFirst) {
        LaunchedEffect(Unit) {
            withFrameNanos {}
            requester.requestFocus()
        }
    }
}

/** Test tag of a dialog action, e.g. `addon-dialog-Move Up`. */
fun dialogButtonTag(label: String): String = "addon-dialog-$label"

private val DIALOG_WIDTH = 300.dp
private val DIALOG_PADDING = 24.dp
