package com.kien.movies.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import com.kien.movies.addons.AddonDialog
import com.kien.movies.addons.AddonDialogButton
import com.kien.movies.addons.phone.QrCodeImage
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvType
import com.nuvio.tvos.TvosTraktPhase
import com.nuvio.tvos.TvosTraktState

/**
 * Trakt sign in (tvOS AccountView.swift), laid out like the Settings pages: the code and a QR code
 * of the address on the left while the user approves it on a phone, the account actions on the
 * right.
 */
@Composable
fun AccountScreen() {
    val model = viewModel { TraktAccountViewModel() }
    val state by model.state.collectAsStateWithLifecycle()
    var confirmsSignOut by remember { mutableStateOf(false) }
    val waiting = state.phase == TvosTraktPhase.WAITING_FOR_APPROVAL

    SettingsSplit(
        title = SettingsPage.Account.title,
        preview = {
            SettingsPreview(caption = if (waiting) WAITING_CAPTION else SettingsPage.Account.caption) {
                if (waiting) CodeArt(state) else SettingsSymbolArt(SettingsPage.Account.icon)
            }
        },
        list = {
            Column(verticalArrangement = Arrangement.spacedBy(SettingsMetrics.rowGap)) {
                Text(text = "Trakt", style = TvType.caption1, color = TvColors.textSecondary, modifier = Modifier.padding(start = INSET))
                Rows(state, model, onSignOut = { confirmsSignOut = true })
                footer(state)?.let { footer ->
                    Text(text = footer, style = TvType.caption1, color = TvColors.textSecondary, modifier = Modifier.padding(horizontal = INSET))
                }
            }
        }
    )
    if (confirmsSignOut) {
        AddonDialog(title = "Sign out of Trakt?", onDismiss = { confirmsSignOut = false }) {
            AddonDialogButton(label = "Sign Out", onClick = {
                confirmsSignOut = false
                model.signOut()
            })
            AddonDialogButton(label = "Cancel", onClick = { confirmsSignOut = false }, focusFirst = true)
        }
    }
}

@Composable
private fun Rows(state: TvosTraktState, model: TraktAccountViewModel, onSignOut: () -> Unit) {
    when (state.phase) {
        TvosTraktPhase.SIGNED_IN -> {
            SettingsLinkLabel(title = "Account", value = state.summary, onClick = {}, trailingIcon = null)
            SettingsLinkLabel(title = "Sign Out", onClick = onSignOut, trailingIcon = null, modifier = Modifier.testTag(TRAKT_SIGN_OUT_TAG))
        }

        TvosTraktPhase.WAITING_FOR_APPROVAL -> {
            SettingsLinkLabel(title = "Code", value = state.userCode, onClick = {}, trailingIcon = null, modifier = Modifier.testTag(TRAKT_CODE_TAG))
            SettingsLinkLabel(title = "Cancel", onClick = model::cancel, trailingIcon = null)
        }

        TvosTraktPhase.STARTING -> SettingsLinkLabel(title = "Getting a Code", value = "...", onClick = {}, trailingIcon = null)

        // Without credentials the row stays focusable, so the page holds focus; it says why and does nothing.
        TvosTraktPhase.NOT_CONFIGURED -> SettingsLinkLabel(
            title = SIGN_IN,
            value = NOT_AVAILABLE,
            onClick = {},
            trailingIcon = null,
            modifier = Modifier.testTag(TRAKT_SIGN_IN_TAG)
        )

        TvosTraktPhase.SIGNED_OUT -> SettingsLinkLabel(
            title = SIGN_IN,
            onClick = model::signIn,
            trailingIcon = null,
            modifier = Modifier.testTag(TRAKT_SIGN_IN_TAG)
        )
    }
}

/** The address and code to approve, with a QR code of the address. */
@Composable
private fun CodeArt(state: TvosTraktState) {
    val url = state.verificationUrl.orEmpty()
    ArtPlatter {
        QrCodeImage(content = url, minSide = QR_SIDE)
        Spacer(modifier = Modifier.height(ART_GAP))
        Text(text = url.removePrefix("https://"), style = TvType.caption1, color = TvColors.textPrimary)
        Text(text = state.userCode.orEmpty(), style = CODE_STYLE, color = TvColors.textPrimary)
    }
}

private fun footer(state: TvosTraktState): String? = if (state.phase == TvosTraktPhase.NOT_CONFIGURED) NOT_CONFIGURED_FOOTER else state.failureText

const val TRAKT_SIGN_IN_TAG = "trakt-sign-in"
const val TRAKT_SIGN_OUT_TAG = "trakt-sign-out"
const val TRAKT_CODE_TAG = "trakt-code"

private const val SIGN_IN = "Sign In to Trakt"
private const val WAITING_CAPTION = "Scan the code or open the address on a phone, then type the code there."
private val INSET = 16.dp
private val QR_SIDE = 110.dp
private val ART_GAP = 12.dp
private val CODE_STYLE = TvType.pageTitle.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 30.sp)
