package com.kien.movies.streams

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.design.TextStyles
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvType
import com.kien.movies.design.containerRadius

/**
 * The content of one stream button (tvOS StreamsRow.swift): resolution badge, name, up to four
 * capability badges and the file line, then size, seeders and delivery. The pressed row shows its
 * progress and only the tags that fit beside it, with no file line. Colours follow the button's
 * content colour, so a focused row reads black on white.
 *
 * The padding is the bordered button's own (30 x 15 pt) plus the row's 8 pt, so the button is
 * 134 pt tall around a 104 pt row, as measured on the tvOS capture.
 */
@Composable
fun StreamsRow(row: StreamRowItem, progress: StreamRowProgress?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ROW_PADDING_H, vertical = ROW_PADDING_V)
            .height(ROW_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(ROW_GAP),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StreamResolutionBadge(row.features.resolution)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TEXT_GAP)) {
            Text(
                text = row.oneLineName,
                style = ROW_TITLE,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val capabilities = row.features.capabilities.take(MAX_CAPABILITIES)
            if (progress != null) {
                StreamTagsThatFit(gap = BADGE_GAP, modifier = Modifier.height(DETAIL_LINE_HEIGHT)) {
                    capabilities.forEach { label -> StreamCapabilityBadge(label) }
                }
            } else {
                StreamDetailLine(capabilities = capabilities, fileLine = row.fileLine)
            }
        }
        AnimatedContent(
            targetState = progress,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "streamRowTrailing"
        ) { shown ->
            if (shown == null) {
                StreamFacts(size = row.features.size, seeders = row.features.seeders, isTorrent = row.isTorrent)
            } else {
                StreamRowProgressView(shown)
            }
        }
    }
}

@Composable
private fun StreamResolutionBadge(resolution: String?) {
    val content = LocalContentColor.current
    Box(
        modifier = Modifier
            .size(RESOLUTION_BADGE)
            .background(content.copy(alpha = BADGE_FILL_ALPHA), RESOLUTION_BADGE_SHAPE),
        contentAlignment = Alignment.Center
    ) {
        if (resolution != null) {
            Text(text = resolution, style = RESOLUTION_TEXT, maxLines = 1)
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_streams_no_resolution),
                contentDescription = null,
                tint = content.copy(alpha = SECONDARY_ALPHA),
                modifier = Modifier.size(RESOLUTION_SYMBOL)
            )
        }
    }
}

@Composable
private fun StreamDetailLine(capabilities: List<String>, fileLine: String?) {
    Row(
        modifier = Modifier.height(DETAIL_LINE_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(BADGE_GAP),
        verticalAlignment = Alignment.CenterVertically
    ) {
        capabilities.forEach { label -> StreamCapabilityBadge(label) }
        if (fileLine != null) {
            Text(
                text = fileLine,
                style = TvType.caption2,
                color = LocalContentColor.current.copy(alpha = SECONDARY_ALPHA),
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
                modifier = Modifier.padding(start = if (capabilities.isEmpty()) 0.dp else FILE_LINE_GAP)
            )
        }
    }
}

@Composable
private fun StreamCapabilityBadge(text: String) {
    val color = LocalContentColor.current.copy(alpha = SECONDARY_ALPHA)
    Text(
        text = text,
        style = TextStyles.badge,
        color = color,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .border(CAPABILITY_STROKE, color, CAPABILITY_SHAPE)
            .padding(horizontal = CAPABILITY_PADDING_H, vertical = CAPABILITY_PADDING_V)
    )
}

@Composable
private fun StreamFacts(size: String?, seeders: Int?, isTorrent: Boolean) {
    val secondary = LocalContentColor.current.copy(alpha = SECONDARY_ALPHA)
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(TEXT_GAP)) {
        if (size != null) {
            Text(text = size, style = TvType.caption1.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, softWrap = false)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(FACT_GAP), verticalAlignment = Alignment.CenterVertically) {
            if (seeders != null) StreamFact(R.drawable.ic_streams_seeders, seeders.toString(), secondary)
            StreamFact(
                icon = if (isTorrent) R.drawable.ic_streams_torrent else R.drawable.ic_streams_direct,
                text = if (isTorrent) "Torrent" else "Direct",
                color = secondary
            )
        }
    }
}

@Composable
private fun StreamFact(icon: Int, text: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(FACT_ICON_GAP), verticalAlignment = Alignment.CenterVertically) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(FACT_SYMBOL))
        Text(text = text, style = TvType.caption2, color = color, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun StreamRowProgressView(progress: StreamRowProgress) {
    Column(
        modifier = Modifier.widthIn(max = PROGRESS_MAX_WIDTH),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(TEXT_GAP)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(PROGRESS_GAP), verticalAlignment = Alignment.CenterVertically) {
            StreamsSpinner(size = PROGRESS_SPINNER)
            Text(
                text = progress.title,
                style = TvType.caption1.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        progress.fraction?.let { fraction -> StreamsProgressBar(fraction = fraction, modifier = Modifier.width(PROGRESS_BAR_WIDTH)) }
        progress.detail?.let { detail ->
            Text(
                text = detail,
                style = TvType.caption2,
                color = LocalContentColor.current.copy(alpha = SECONDARY_ALPHA),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private const val MAX_CAPABILITIES = 4
private val SECONDARY_ALPHA = TvColors.textSecondary.alpha
private const val BADGE_FILL_ALPHA = 0.1f
private val ROW_TITLE = TvType.body.copy(fontWeight = FontWeight.SemiBold) // 29 pt semibold
private val RESOLUTION_TEXT = TvType.body.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold) // 28 pt bold

// tvOS literals, pt / 2.
private val ROW_HEIGHT = 52.dp // frame(height: 104)
private val RESOLUTION_BADGE = DpSize(62.dp, 32.dp) // 124 x 64
private val ROW_PADDING_H = 19.dp // padding(.horizontal, 8) plus the bordered button's 30

/** Where a row's content (its resolution badge) starts inside the row button. */
internal val STREAM_ROW_CONTENT_INSET = ROW_PADDING_H
private val ROW_PADDING_V = 7.5.dp // the bordered button's 15
private val ROW_GAP = 14.dp // HStack spacing 28
private val TEXT_GAP = 4.dp // spacing 8 to 12
private val RESOLUTION_SYMBOL = 15.dp // 30 pt symbol
private val DETAIL_LINE_HEIGHT = 16.dp // frame(height: 32)
private val BADGE_GAP = 5.dp // HStack spacing 10
private val FILE_LINE_GAP = 3.dp // padding(.leading, 6)
private val CAPABILITY_STROKE = 1.dp // lineWidth 1.5
private val CAPABILITY_PADDING_H = 5.dp // padding(.horizontal, 10)
private val CAPABILITY_PADDING_V = 1.dp // padding(.vertical, 4), less so the badge fits the 16 dp line
private val FACT_GAP = 9.dp // HStack spacing 18
private val FACT_ICON_GAP = 3.dp
private val FACT_SYMBOL = 12.dp
private val PROGRESS_MAX_WIDTH = 150.dp // frame(maxWidth: 300)
private val PROGRESS_GAP = 8.dp // HStack spacing 16
private val PROGRESS_SPINNER = 14.dp
private val PROGRESS_BAR_WIDTH = 130.dp // frame(width: 260)

// Concentric corners (design/Concentric.kt): outer radius = inner radius + padding. The badge keeps
// the tvOS 14 pt corner and sits 17.5 dp inside the row's edge (7.5 dp padding plus (52 - 32) / 2),
// so the row button's corner is 7 + 17.5 = 24.5 dp. The 19 dp side inset is within 1.5 dp of it.
private val RESOLUTION_BADGE_RADIUS = 7.dp // cornerRadius 14
private val RESOLUTION_BADGE_INSET = ROW_PADDING_V + (ROW_HEIGHT - RESOLUTION_BADGE.height) / 2
private val RESOLUTION_BADGE_SHAPE = RoundedCornerShape(RESOLUTION_BADGE_RADIUS)

/** The row button's corner, the outer radius around the resolution badge. */
internal val STREAM_ROW_SHAPE = RoundedCornerShape(containerRadius(inner = RESOLUTION_BADGE_RADIUS, padding = RESOLUTION_BADGE_INSET))

// The tags sit in the text column, not nested in a corner, so the rule has nothing to derive
// them from: they keep the tvOS 6 pt.
private val CAPABILITY_SHAPE = RoundedCornerShape(3.dp)
