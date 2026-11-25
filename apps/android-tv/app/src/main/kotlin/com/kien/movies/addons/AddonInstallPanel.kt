package com.kien.movies.addons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.addons.phone.AddonQrSetup
import com.kien.movies.addons.phone.QrCodeImage
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvType
import com.kien.movies.settings.ArtPlatter
import com.kien.movies.settings.SettingsMetrics
import com.kien.movies.settings.SettingsPreview
import com.kien.movies.streams.StreamsSpinner
import com.nuvio.tvos.phone.AddonPairingLimits

/**
 * The preview while Add Addon has focus (tvOS AddonInstallPanel.swift): the QR code a phone
 * scans to send a manifest URL, what to do, "No addons" when the list is empty, and how the last
 * install went.
 */
@Composable
fun AddonInstallPanel(setup: AddonQrSetup, status: AddonInstallStatus?, hasAddons: Boolean) {
    SettingsPreview(art = { QrArt(setup) }) {
        if (!hasAddons) Text(text = EMPTY_TITLE, style = TvType.headline, color = TvColors.textPrimary)
        Text(text = caption(setup), style = TvType.caption1, color = TvColors.textSecondary, textAlign = TextAlign.Center)
        StatusLine(status)
    }
}

@Composable
private fun QrArt(setup: AddonQrSetup) {
    if (setup is AddonQrSetup.Running) {
        Box(modifier = Modifier.fillMaxSize().background(Color.White, ART_SHAPE), contentAlignment = Alignment.Center) {
            QrCodeImage(content = setup.server.address, maxSide = SettingsMetrics.artSide - QR_INSET * 2, modifier = Modifier.testTag(QR_TAG))
        }
    } else {
        ArtPlatter {
            Icon(painter = painterResource(R.drawable.ic_addons_qr), contentDescription = null, tint = TvColors.textSecondary, modifier = Modifier.size(ADDON_ART_SYMBOL))
        }
    }
}

private fun caption(setup: AddonQrSetup): String = when (setup) {
    AddonQrSetup.Starting, is AddonQrSetup.Running -> SCAN
    AddonQrSetup.NoNetwork -> NO_NETWORK
    AddonQrSetup.NoFreePort -> NO_FREE_PORT
    AddonQrSetup.Expired -> EXPIRED
}

@Composable
private fun StatusLine(status: AddonInstallStatus?) {
    when (status) {
        AddonInstallStatus.Installing -> Row(horizontalArrangement = Arrangement.spacedBy(STATUS_GAP), verticalAlignment = Alignment.CenterVertically) {
            StreamsSpinner(size = STATUS_ICON)
            Text(text = "Installing", style = TvType.caption1, color = TvColors.textPrimary)
        }

        is AddonInstallStatus.Installed -> StatusLabel(R.drawable.ic_addons_check, "Installed ${status.name}", TvColors.textSecondary)

        is AddonInstallStatus.Failed -> {
            StatusLabel(R.drawable.ic_addons_warning, INSTALL_FAILED, ERROR_RED)
            Text(text = status.message, style = TvType.caption2, color = TvColors.textSecondary, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }

        null -> Unit
    }
}

@Composable
private fun StatusLabel(icon: Int, text: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(STATUS_GAP), verticalAlignment = Alignment.CenterVertically) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(STATUS_ICON))
        Text(text = text, style = TvType.caption1, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

const val EMPTY_TITLE = "No addons"
const val INSTALL_FAILED = "Couldn't add this addon"
const val QR_TAG = "addon-qr"

private const val SCAN = "Scan with your phone's camera, then paste the manifest URL on the page that opens. Or select Add Addon to type it."
const val NO_NETWORK = "Connect the TV to your home network to add from a phone, or select Add Addon to type a manifest URL."
private val NO_FREE_PORT = "Ports ${AddonPairingLimits.FIRST_PORT} to ${AddonPairingLimits.LAST_PORT} are all in use, so a phone cannot reach the TV. Select Add Addon to type a manifest URL."
private const val EXPIRED = "The code stopped after 10 minutes without a phone. Open Addons again for a new one, or select Add Addon to type a manifest URL."

private val ART_SHAPE = RoundedCornerShape(SettingsMetrics.artRadius)

// tvOS pads the code 56 pt in its white square; the code's own 4 module quiet zone is white too,
// so 14 dp here leaves the same white margin.
private val QR_INSET = 14.dp
private val STATUS_ICON = 16.dp // tvOS Label symbol at the caption size
private val STATUS_GAP = 8.dp // tvOS HStack spacing 16 pt
