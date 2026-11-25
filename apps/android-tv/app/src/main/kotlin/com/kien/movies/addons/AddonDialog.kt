package com.kien.movies.addons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvControls
import com.kien.movies.design.TvGlass
import com.kien.movies.design.TvShapes
import com.kien.movies.design.TvSheet
import com.kien.movies.design.TvType
import com.kien.movies.design.glassEdge

/**
 * The confirmation dialogs of Addons (tvOS `confirmationDialog` and `alert`): a title, an optional
 * message and capsule buttons, on one opaque panel with the glass edge. tv-material 1.1.0 has no dialog, so this is a Compose
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
                .background(TvColors.dialogPanel, DIALOG_SHAPE)
                .glassEdge(DIALOG_SHAPE)
                .padding(DIALOG_PADDING),
            verticalArrangement = Arrangement.spacedBy(DIALOG_GAP),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = TvType.headline, color = TvColors.textPrimary, textAlign = TextAlign.Center)
            if (message != null) {
                Text(text = message, style = TvType.caption2, color = TvColors.textSecondary, textAlign = TextAlign.Center)
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
        colors = TvGlass.buttonColors(),
        scale = TvGlass.buttonScale(),
        shape = TvGlass.pillShape(),
        border = TvGlass.buttonBorder(),
        contentPadding = TvGlass.pillPadding,
        modifier = modifier
            .fillMaxWidth()
            .height(TvControls.pillHeight)
            .focusRequester(requester)
            .testTag(dialogButtonTag(label))
    ) {
        Text(text = label, style = TvType.bodyEmphasized, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
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

// Concentric with the capsule buttons: 16 dp pill radius + 24 dp padding.
private val DIALOG_SHAPE = TvSheet.shapeFor(DIALOG_PADDING)
private val DIALOG_GAP = 12.dp
