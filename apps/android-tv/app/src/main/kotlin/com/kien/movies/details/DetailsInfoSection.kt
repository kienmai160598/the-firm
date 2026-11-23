package com.kien.movies.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kien.movies.design.CardSize
import com.kien.movies.design.Palette
import com.kien.movies.design.Spacing
import com.kien.movies.design.TextStyles

/**
 * "Information": release, run time, rating, genres, director (tvOS DetailsInfoSection.swift). The
 * block is one focusable element so the D-pad can scroll down to it (AC D4); focus shows a flat
 * white 12% fill, no scale, as on tvOS. Select does nothing.
 */
@Composable
internal fun DetailsInfoSection(entries: List<InfoEntry>, modifier: Modifier = Modifier) {
    if (entries.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(SECTION_GAP_20PT)) {
        ShelfHeader(title = INFO_TITLE, modifier = Modifier.padding(horizontal = Spacing.screenHorizontal))
        // tvOS pulls the block out by its own padding so the text lines up with the header.
        Surface(
            onClick = {},
            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(CardSize.cornerRadius)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                contentColor = Palette.textPrimary,
                focusedContainerColor = FOCUSED_FILL,
                focusedContentColor = Palette.textPrimary,
                pressedContainerColor = FOCUSED_FILL,
                pressedContentColor = Palette.textPrimary
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            modifier = Modifier
                .padding(horizontal = Spacing.screenHorizontal - BLOCK_PADDING_28PT)
                .fillMaxWidth()
                .testTag(DETAILS_INFO_TAG)
        ) {
            Row(
                modifier = Modifier.padding(BLOCK_PADDING_28PT),
                horizontalArrangement = Arrangement.spacedBy(COLUMN_GAP_48PT),
                verticalAlignment = Alignment.Top
            ) {
                entries.forEach { entry ->
                    InfoColumn(entry, Modifier.weight(1f, fill = false).widthIn(max = COLUMN_WIDTH_400PT))
                }
            }
        }
    }
}

@Composable
private fun InfoColumn(entry: InfoEntry, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(TITLE_GAP_6PT)) {
        Text(text = entry.title, style = TITLE_STYLE, color = TITLE_COLOR, maxLines = 1)
        Text(text = entry.value, style = TextStyles.small, color = VALUE_COLOR, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

const val INFO_TITLE = "Information"
const val DETAILS_INFO_TAG = "details-info"

// tvOS literals, pt / 2.
private val SECTION_GAP_20PT = 10.dp
private val BLOCK_PADDING_28PT = 14.dp
private val COLUMN_GAP_48PT = 24.dp
private val COLUMN_WIDTH_400PT = 200.dp
private val TITLE_GAP_6PT = 3.dp
private val TITLE_STYLE = TextStyles.small.copy(fontWeight = FontWeight.SemiBold)
private val TITLE_COLOR = Color.White.copy(alpha = 0.5f)
private val VALUE_COLOR = Color.White.copy(alpha = 0.85f)
private val FOCUSED_FILL = Color.White.copy(alpha = 0.12f)
