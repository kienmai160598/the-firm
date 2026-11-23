package com.kien.movies.streams

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvControls
import com.kien.movies.design.TvGlass
import com.kien.movies.design.TvShapes
import com.kien.movies.design.TvType
import com.kien.movies.design.glassEdge
import com.kien.movies.player.StreamNotice
import com.nuvio.tvos.TvosStream

/**
 * What is known about a stream, and why it did not play (tvOS StreamDetailSheet.swift, a sheet):
 * the notice when there is one, the name, the source fields and, without a notice, which sources
 * this app plays (S4, P4). A dialog window: it takes Back itself, Close holds focus.
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
                .widthIn(max = SHEET_MAX_WIDTH)
                .heightIn(max = SHEET_MAX_HEIGHT)
                .background(DialogPanelFill, TvShapes.panel)
                .glassEdge(TvShapes.panel)
                .padding(SHEET_PADDING)
                .testTag(STREAM_SHEET_TAG),
            verticalArrangement = Arrangement.spacedBy(SHEET_GAP)
        ) {
            sheet.notice?.let { notice -> StreamNoticeView(notice) }
            Text(text = sheet.stream.name, style = TvType.headline, maxLines = 2, overflow = TextOverflow.Ellipsis)
            // Only the fields scroll: Close is the one focusable, so the notice and the name above
            // must never scroll out of reach.
            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(SHEET_GAP)
            ) {
                StreamFieldGrid(fields(sheet.stream))
                if (sheet.notice == null) {
                    Text(text = playbackNote(sheet.stream), style = TvType.caption2, color = TvColors.textSecondary)
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

@Composable
private fun StreamNoticeView(notice: StreamNotice) {
    Row(horizontalArrangement = Arrangement.spacedBy(NOTICE_GAP), verticalAlignment = Alignment.Top) {
        Icon(
            painter = painterResource(R.drawable.ic_streams_warning),
            contentDescription = null,
            tint = NOTICE_YELLOW,
            modifier = Modifier.size(NOTICE_SYMBOL)
        )
        Column(verticalArrangement = Arrangement.spacedBy(NOTICE_TEXT_GAP)) {
            Text(text = notice.title, style = TvType.headline)
            Text(text = notice.message, style = TvType.caption1, color = TvColors.textSecondary)
        }
    }
}

@Composable
private fun StreamFieldGrid(fields: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(FIELD_ROW_GAP)) {
        fields.forEach { (label, value) ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FIELD_COLUMN_GAP)) {
                Box(modifier = Modifier.width(FIELD_LABEL_WIDTH), contentAlignment = Alignment.TopEnd) {
                    Text(
                        text = label,
                        style = TvType.caption2,
                        color = TvColors.textSecondary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.padding(top = FIELD_LABEL_BASELINE)
                    )
                }
                Text(text = value, style = TvType.caption1, maxLines = FIELD_MAX_LINES, overflow = TextOverflow.Ellipsis)
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

// tvOS literals, pt / 2.
private val SHEET_MAX_WIDTH = 650.dp // frame(maxWidth: 1300)
private val SHEET_MAX_HEIGHT = 486.dp // the screen less the 27 dp overscan at top and bottom
private val SHEET_PADDING = 32.dp // padding(64)
private val SHEET_GAP = 18.dp // spacing 36
private val CLOSE_TOP = 4.dp // padding(.top, 8)
private val NOTICE_TEXT_GAP = 8.dp
private val NOTICE_GAP = 14.dp // HStack spacing 28
private val NOTICE_SYMBOL = 26.dp // 52 pt symbol
private val FIELD_ROW_GAP = 10.dp // verticalSpacing 20
private val FIELD_COLUMN_GAP = 20.dp // horizontalSpacing 40
private val FIELD_LABEL_WIDTH = 90.dp // widest label, "External URL", at 12 sp
private val FIELD_LABEL_BASELINE = 1.dp // first text baseline of 12 sp against 13 sp
