package com.kien.movies.addons.phone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.kien.movies.addons.AddonDialog
import com.kien.movies.addons.AddonDialogButton
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvType
import com.nuvio.tvos.phone.AddonProposal

/**
 * The TV's answer to a phone's proposal (docs/android-tv-rebuild.md, 5b, step 3): each URL, then
 * Install (focused) or Cancel. Back is Cancel. Nothing is installed without this dialog.
 */
@Composable
fun PhoneAddonDialog(proposal: AddonProposal, onInstall: () -> Unit, onCancel: () -> Unit) {
    val title = if (proposal.urls.size == 1) "Install this addon?" else "Install these ${proposal.urls.size} addons?"
    AddonDialog(title = title, message = "Sent from a phone on your network.", onDismiss = onCancel, width = DIALOG_WIDTH) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(URL_GAP)) {
            proposal.urls.forEach { url ->
                Text(text = url, style = TvType.caption2, color = TvColors.textPrimary, maxLines = 1, overflow = TextOverflow.MiddleEllipsis)
            }
        }
        AddonDialogButton(label = INSTALL, onClick = onInstall, focusFirst = true)
        AddonDialogButton(label = CANCEL, onClick = onCancel)
    }
}

private const val INSTALL = "Install"
private const val CANCEL = "Cancel"

private val DIALOG_WIDTH = 420.dp
private val URL_GAP = 4.dp
