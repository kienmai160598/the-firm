package com.kien.movies.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvGrid
import com.kien.movies.design.TvType

/**
 * The Settings app split (tvOS SettingsSplit in AddonPreview.swift): the list against the right
 * safe edge and the preview centred in the space left of it. A pushed page shows its title on
 * top; a tab root leaves the same room empty, so rows and art sit at one height on both.
 */
@Composable
fun SettingsSplit(title: String?, preview: @Composable () -> Unit, list: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(top = SettingsMetrics.top)) {
        Box(modifier = Modifier.fillMaxWidth().height(SettingsMetrics.titleRoom), contentAlignment = Alignment.TopCenter) {
            if (title != null) Text(text = title, style = TvType.pageTitle, color = TvColors.textSecondary, maxLines = 1)
        }
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = TvGrid.sideMargin),
            horizontalArrangement = Arrangement.spacedBy(SettingsMetrics.gap)
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(top = SettingsMetrics.artTop), contentAlignment = Alignment.TopCenter) {
                preview()
            }
            Box(modifier = Modifier.width(SettingsMetrics.listWidth).fillMaxHeight().focusGroup()) {
                list()
            }
        }
    }
}

/** The preview's art and caption, stacked and centred like Settings. */
@Composable
fun SettingsPreview(caption: String, art: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(SettingsMetrics.artToCaption)) {
        Box(modifier = Modifier.size(SettingsMetrics.artSide)) { art() }
        Text(
            text = caption,
            style = TvType.caption1,
            color = TvColors.textSecondary,
            textAlign = TextAlign.Center,
            maxLines = SettingsMetrics.captionLines,
            modifier = Modifier.width(SettingsMetrics.captionWidth)
        )
    }
}

/** A page's icon as the preview art, like the device picture of the Settings app. */
@Composable
fun SettingsSymbolArt(@DrawableRes icon: Int) {
    ArtPlatter {
        Icon(painter = painterResource(icon), contentDescription = null, tint = TvColors.textSecondary, modifier = Modifier.size(SettingsMetrics.symbol))
    }
}

/** The rounded platter the preview art sits on. */
@Composable
fun ArtPlatter(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(TvColors.settingsArt, RoundedCornerShape(SettingsMetrics.artRadius)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content
    )
}

/**
 * The Settings app split measured on an Apple TV 4K screenshot and halved (dp = pt / 2): rows start
 * 89 dp down, the art 30 dp below them, centred in the space between the side margin and the list.
 */
object SettingsMetrics {
    val top = 27.dp
    val titleRoom = 62.dp
    val listWidth = 390.dp
    val gap = 45.dp
    val artTop = 30.dp
    val artSide = 270.dp
    val artRadius = 60.dp
    val artToCaption = 20.dp
    val captionWidth = 360.dp
    const val captionLines = 5
    val symbol = 130.dp
    val rowHeight = 33.dp
    val rowGap = 7.dp
}
