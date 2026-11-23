package com.kien.movies.streams

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.kien.movies.design.Palette
import com.kien.movies.design.Spacing
import com.kien.movies.design.TextStyles
import com.nuvio.tvos.TvosResumePoint
import com.nuvio.tvos.TvosStreamTarget

/**
 * Title, subtitle (episode line) and, when opened to resume directly, the resume bar (tvOS
 * StreamsHeader.swift).
 */
@Composable
fun StreamsHeader(target: TvosStreamTarget, resume: TvosResumePoint?, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(HEADER_GAP)) {
        Text(
            text = target.title,
            style = TextStyles.streamsTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag(STREAMS_TITLE_TAG)
        )
        target.subtitle?.let { subtitle ->
            Text(
                text = subtitle,
                style = TextStyles.body.copy(fontWeight = FontWeight.Medium),
                color = Color.White.copy(alpha = SUBTITLE_ALPHA),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        resume?.let { point -> StreamsResume(point, Modifier.padding(top = RESUME_TOP)) }
    }
}

@Composable
private fun StreamsResume(resume: TvosResumePoint, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.textGap)) {
        StreamsProgressBar(fraction = resume.watchedFraction, modifier = Modifier.width(RESUME_BAR_WIDTH))
        Text(text = resume.label, style = TextStyles.small.copy(fontWeight = FontWeight.Medium), color = Palette.textSecondary)
    }
}

/**
 * A capsule bar (tvOS WatchProgressBar), also the torrent buffer bar of a row. Private to Streams:
 * Home and Details keep their own until it moves to design/.
 */
@Composable
fun StreamsProgressBar(fraction: Float, modifier: Modifier = Modifier, height: Dp = BAR_HEIGHT) {
    Box(modifier = modifier.height(height).background(BAR_TRACK, CircleShape)) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(Color.White, CircleShape)
        )
    }
}

const val STREAMS_TITLE_TAG = "Streams"

// tvOS literals, pt / 2.
private val HEADER_GAP = 5.dp // spacing 10
private val RESUME_TOP = 4.dp // padding(.top, 8)
private val RESUME_BAR_WIDTH = 180.dp // frame(width: 360)
private val BAR_HEIGHT = 4.dp // WatchProgressBar height 8
private val BAR_TRACK = Color.Black.copy(alpha = 0.55f)
private const val SUBTITLE_ALPHA = 0.75f
