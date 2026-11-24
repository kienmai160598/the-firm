package com.kien.movies.streams

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvControls
import com.kien.movies.design.TvGlass
import com.kien.movies.design.TvSheet
import com.kien.movies.design.TvType
import com.kien.movies.design.glassEdge
import com.kien.movies.player.StreamNotice
import com.nuvio.tvos.TvosStream

/**
 * What is known about a stream, and why it did not play (tvOS StreamDetailSheet.swift, a sheet):
 * the notice when there is one, the name on one line, the source fields and, without a notice,
 * which sources this app plays (S4, P4). A centred panel [TvSheet.width] wide whose corner is
 * concentric with the Close capsule ([TvSheet.shape]). A dialog window: it takes Back itself,
 * Close holds focus.
 */
@Composable
fun StreamDetailSheet(sheet: StreamSheet, onClose: () -> Unit) {
    val closeFocus = remember { FocusRequester() }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        LaunchedEffect(sheet) {
            withFrameNanos {}
            closeFocus.requestFocus()
        }
        Column(
            modifier = Modifier
                .width(TvSheet.width)
                .heightIn(max = SHEET_MAX_HEIGHT)
                .background(TvColors.dialogPanel, TvSheet.shape)
                .glassEdge(TvSheet.shape)
                .padding(TvSheet.padding)
                .testTag(STREAM_SHEET_TAG),
            verticalArrangement = Arrangement.spacedBy(SHEET_GAP)
        ) {
            sheet.notice?.let { notice -> StreamNoticeView(notice) }
            Text(text = oneLineName(sheet.stream.name), style = NAME_STYLE, maxLines = 2, overflow = TextOverflow.Ellipsis)
            // Only the fields scroll: Close is the one focusable, so the notice and the name above
            // must never scroll out of reach.
            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(SHEET_GAP)
            ) {
                StreamFieldGrid(fields(sheet.stream))
                if (sheet.notice == null) {
                    Text(text = playbackNote(sheet.stream), style = TvType.caption1, color = TvColors.textSecondary)
                }
            }
            Button(
                onClick = onClose,
                colors = TvGlass.buttonColors(),
                scale = TvGlass.buttonScale(),
                shape = TvGlass.pillShape(),
                border = TvGlass.buttonBorder(),
                contentPadding = TvGlass.pillPadding,
                modifier = Modifier
                    .padding(top = CLOSE_TOP)
                    .height(TvControls.pillHeight)
                    .focusRequester(closeFocus)
                    .testTag(STREAM_SHEET_CLOSE_TAG)
            ) {
                Text(text = "Close", style = TvType.bodyEmphasized)
            }
        }
    }
}

/** The yellow triangle and the title on one line, the message under both. */
@Composable
private fun StreamNoticeView(notice: StreamNotice) {
    Column(verticalArrangement = Arrangement.spacedBy(NOTICE_TEXT_GAP)) {
        Row(horizontalArrangement = Arrangement.spacedBy(NOTICE_GAP), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_streams_warning),
                contentDescription = null,
                tint = NOTICE_YELLOW,
                modifier = Modifier.size(NOTICE_SYMBOL)
            )
            Text(text = notice.title, style = NOTICE_TITLE_STYLE)
        }
        Text(text = notice.message, style = TvType.body, color = TvColors.textSecondary)
    }
}

/**
 * Two columns as the tvOS Grid lays them out: the labels left aligned in a column as wide as the
 * widest label, the values beside them, each row aligned on its first baseline.
 */
@Composable
private fun StreamFieldGrid(fields: List<Pair<String, String>>) {
    Layout(
        content = {
            fields.forEach { (label, _) -> Text(text = label, style = TvType.caption1, color = TvColors.textSecondary, maxLines = 1) }
            fields.forEach { (_, value) -> Text(text = value, style = TvType.body, maxLines = FIELD_MAX_LINES, overflow = TextOverflow.Ellipsis) }
        }
    ) { measurables, constraints ->
        val labels = measurables.take(fields.size).map { label -> label.measure(Constraints(maxWidth = constraints.maxWidth / 3)) }
        val labelWidth = labels.maxOfOrNull(Placeable::width) ?: 0
        val valueX = labelWidth + FIELD_COLUMN_GAP.roundToPx()
        val valueConstraints = Constraints(maxWidth = (constraints.maxWidth - valueX).coerceAtLeast(0))
        val values = measurables.drop(fields.size).map { value -> value.measure(valueConstraints) }
        val rowGap = FIELD_ROW_GAP.roundToPx()
        val labelTops = IntArray(fields.size)
        val valueTops = IntArray(fields.size)
        var top = 0
        for (index in fields.indices) {
            if (index > 0) top += rowGap
            val labelBaseline = labels[index][FirstBaseline]
            val valueBaseline = values[index][FirstBaseline]
            val baseline = maxOf(labelBaseline, valueBaseline)
            labelTops[index] = top + baseline - labelBaseline
            valueTops[index] = top + baseline - valueBaseline
            top = maxOf(labelTops[index] + labels[index].height, valueTops[index] + values[index].height)
        }
        layout(constraints.maxWidth, top.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            for (index in fields.indices) {
                labels[index].place(0, labelTops[index])
                values[index].place(valueX, valueTops[index])
            }
        }
    }
}

private fun fields(stream: TvosStream): List<Pair<String, String>> = listOf(
    "Source" to stream.kind,
    "Details" to (stream.detail ?: stream.title),
    "URL" to stream.url,
    "External URL" to stream.externalUrl,
    "Info hash" to stream.infoHash,
    "File index" to stream.fileIndex,
    "File name" to stream.filename,
    "Size" to stream.sizeLabel
).mapNotNull { (label, value) -> value?.let { label to it } }

private fun playbackNote(stream: TvosStream): String = if (stream.infoHash != null) TORRENT_NOTE else UNSUPPORTED_NOTE

// tvOS says "Movies on Apple TV"; the Android copy names no platform. Until A10 a torrent press
// shows a notice instead, so the torrent note only shows once A10 starts torrents.
private const val TORRENT_NOTE =
    "This is a torrent. Movies streams it with its built-in torrent engine " +
        "and plays MP4, MKV, WebM and most other video files."
private const val UNSUPPORTED_NOTE =
    "Movies cannot play this source yet. It plays direct HTTP(S) links such as MP4, MKV and HLS."

const val STREAM_SHEET_TAG = "stream-sheet"
const val STREAM_SHEET_CLOSE_TAG = "stream-sheet-close"

private const val FIELD_MAX_LINES = 4
private val NOTICE_YELLOW = Color(0xFFFFCC00) // the multicolour triangle symbol
private val NAME_STYLE = TvType.headline.copy(fontSize = 24.sp, lineHeight = 29.sp) // 48 pt bold
private val NOTICE_TITLE_STYLE = TvType.headline.copy(fontWeight = FontWeight.SemiBold) // 38 pt semibold

// tvOS literals, pt / 2.
private val SHEET_MAX_HEIGHT = 486.dp // the screen less the 27 dp overscan at top and bottom
private val SHEET_GAP = 18.dp // spacing 36
private val CLOSE_TOP = 4.dp // padding(.top, 8)
private val NOTICE_TEXT_GAP = 6.dp // spacing 12
private val NOTICE_GAP = 8.dp // HStack spacing 16
private val NOTICE_SYMBOL = 20.dp // the 38 pt symbol
private val FIELD_ROW_GAP = 10.dp // verticalSpacing 20
private val FIELD_COLUMN_GAP = 20.dp // horizontalSpacing 40
