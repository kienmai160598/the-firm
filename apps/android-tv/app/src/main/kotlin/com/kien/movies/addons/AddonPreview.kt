package com.kien.movies.addons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.kien.movies.R
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvType
import com.kien.movies.settings.ArtPlatter
import com.kien.movies.settings.SettingsMetrics
import com.kien.movies.settings.SettingsPreview
import com.nuvio.tvos.TvosAddon

/**
 * The focused addon (tvOS AddonSummary in AddonPreview.swift): its logo on the art platter, then
 * why it failed or, unless the page lists it under About, up to five lines of its description.
 */
@Composable
fun AddonSummary(addon: TvosAddon, showsDescription: Boolean = true) {
    SettingsPreview(art = { ArtPlatter { AddonLogo(addon.logoUrl) } }) {
        val error = addon.errorMessage
        when {
            error != null -> Row(horizontalArrangement = Arrangement.spacedBy(ICON_GAP), verticalAlignment = Alignment.CenterVertically) {
                Icon(painter = painterResource(R.drawable.ic_addons_warning), contentDescription = null, tint = ERROR_RED, modifier = Modifier.size(ICON))
                AddonCaption(text = error, color = ERROR_RED, modifier = Modifier.weight(1f, fill = false))
            }

            showsDescription && addon.summary.isNotEmpty() -> AddonCaption(text = addon.summary, color = TvColors.textSecondary)
        }
    }
}

@Composable
private fun AddonCaption(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = TvType.caption1,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = SettingsMetrics.captionLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/** The logo inset as on tvOS, or the puzzle symbol while there is none or it has not loaded. */
@Composable
private fun AddonLogo(url: String?) {
    var loaded by remember(url) { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (!loaded) {
            Icon(painter = painterResource(R.drawable.ic_settings_addons), contentDescription = null, tint = TvColors.textSecondary, modifier = Modifier.size(ADDON_ART_SYMBOL))
        }
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                onSuccess = { loaded = true },
                modifier = Modifier.fillMaxSize().padding(LOGO_INSET)
            )
        }
    }
}

/** tvOS `.red` in dark mode, for an addon's error and a failed install. */
internal val ERROR_RED = Color(0xFFFF453A)

private val LOGO_INSET = 45.dp // tvOS 90 pt

/** tvOS 200 pt symbol on the art, scaled as Settings' 220 pt symbol is to 130 dp. */
internal val ADDON_ART_SYMBOL = 118.dp
private val ICON = 16.dp // tvOS Label symbol at the caption size
private val ICON_GAP = 8.dp // as STATUS_GAP, tvOS 16 pt
