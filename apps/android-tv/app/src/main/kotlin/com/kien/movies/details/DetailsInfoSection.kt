package com.kien.movies.details

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.kien.movies.design.TvGrid
import com.kien.movies.design.TvType

/**
 * "Information" as tvOS draws it (DetailsInfoSection.swift): the entries side by side, each column
 * 200 dp wide (tvOS `maxWidth` 400 pt fills to it), in a platter as wide as the page that is filled only while it has focus.
 * Focusable so the D-pad can scroll down to it (AC D4); Select does nothing.
 */
@Composable
internal fun DetailsInfoSection(entries: List<InfoEntry>, modifier: Modifier = Modifier) {
    if (entries.isEmpty()) return
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(INFO_TITLE_GAP)) {
        ShelfHeader(title = INFO_TITLE)
        // The platter reaches out by its padding so the text lines up with the title.
        Row(
            modifier = Modifier
                .padding(horizontal = TvGrid.sideMargin - INFO_PADDING)
                .fillMaxWidth()
                .background(if (isFocused) INFO_FOCUSED_FILL else Color.Transparent, INFO_SHAPE)
                .focusable(interactionSource = interaction)
                .testTag(DETAILS_INFO_TAG)
                .padding(INFO_PADDING),
            horizontalArrangement = Arrangement.spacedBy(INFO_COLUMN_GAP),
            verticalAlignment = Alignment.Top
        ) {
            entries.forEach { entry -> InfoColumn(entry) }
        }
    }
}

@Composable
private fun InfoColumn(entry: InfoEntry) {
    Column(modifier = Modifier.width(INFO_COLUMN_WIDTH), verticalArrangement = Arrangement.spacedBy(INFO_VALUE_GAP)) {
        Text(text = entry.title, style = INFO_LABEL_STYLE, color = INFO_LABEL_COLOR, maxLines = 1)
        Text(text = entry.value, style = INFO_VALUE_STYLE, color = INFO_VALUE_COLOR, maxLines = INFO_VALUE_LINES, overflow = TextOverflow.Ellipsis)
    }
}

const val INFO_TITLE = "Information"
const val DETAILS_INFO_TAG = "details-info"

private const val INFO_VALUE_LINES = 3

// tvOS (pt / 2): columns 400 pt, 48 pt apart, 28 pt inside a 24 pt corner platter, white
// 12% when focused; label 22 pt semibold white 50%, value 24 pt white 85%, 6 pt apart; the title
// 20 pt above the platter.
private val INFO_COLUMN_WIDTH = 200.dp
private val INFO_COLUMN_GAP = 24.dp
private val INFO_PADDING = 14.dp
private val INFO_SHAPE = RoundedCornerShape(12.dp)
private val INFO_FOCUSED_FILL = Color.White.copy(alpha = 0.12f)
private val INFO_TITLE_GAP = 10.dp
private val INFO_VALUE_GAP = 3.dp
private val INFO_LABEL_STYLE = TvType.body.copy(fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.SemiBold)
private val INFO_VALUE_STYLE = TvType.body.copy(fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.Normal)
private val INFO_LABEL_COLOR = Color.White.copy(alpha = 0.5f)
private val INFO_VALUE_COLOR = Color.White.copy(alpha = 0.85f)
