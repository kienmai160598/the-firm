package com.kien.movies.streams

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Text
import com.kien.movies.design.ArtworkImage
import com.kien.movies.design.CardShape
import com.kien.movies.design.TextStyles
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvType
import com.kien.movies.home.rememberSizedImage
import com.nuvio.tvos.TvosResumePoint
import com.nuvio.tvos.TvosStreamTarget

/**
 * The left column of Streams (tvOS StreamsHeader.swift), [STREAMS_HEADER_WIDTH] wide. It stays
 * put while the list beside it scrolls: the episode still or the poster, the title, the subtitle
 * (episode line), the resume bar when opened to resume directly, and the source count with
 * "Searching N more addons" (S1). Nothing in it takes focus.
 */
@Composable
fun StreamsHeader(
    target: TvosStreamTarget,
    resume: TvosResumePoint?,
    sourceCount: Int,
    searchingAddonCount: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.width(STREAMS_HEADER_WIDTH), verticalArrangement = Arrangement.spacedBy(HEADER_GAP)) {
        StreamsArtwork(target = target, modifier = Modifier.padding(bottom = ARTWORK_BOTTOM))
        Text(
            text = target.title,
            style = TextStyles.streamsTitle,
            color = TvColors.textPrimary,
            maxLines = TITLE_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag(STREAMS_TITLE_TAG)
        )
        target.subtitle?.let { subtitle ->
            Text(text = subtitle, style = TvType.body, color = SUBTITLE_COLOR, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        resume?.let { point -> StreamsResume(point, Modifier.padding(top = RESUME_TOP)) }
        StreamsSourceSummary(
            sourceCount = sourceCount,
            searchingAddonCount = searchingAddonCount,
            modifier = Modifier.padding(top = SUMMARY_TOP)
        )
    }
}

/** The episode still for an episode, otherwise the poster; nothing when neither exists. */
@Composable
private fun StreamsArtwork(target: TvosStreamTarget, modifier: Modifier = Modifier) {
    val still = target.episodeThumbnailUrl
    val poster = target.posterUrl
    when {
        still != null -> Artwork(still, STILL_SIZE, target.title, modifier)
        poster != null -> Artwork(poster, POSTER_SIZE, target.title, modifier)
    }
}

@Composable
private fun Artwork(url: String, size: DpSize, title: String, modifier: Modifier) {
    ArtworkImage(
        model = rememberSizedImage(url, size),
        fallbackTitle = title,
        modifier = modifier.size(size).clip(CardShape.shape)
    )
}

@Composable
private fun StreamsResume(resume: TvosResumePoint, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(RESUME_GAP)) {
        StreamsProgressBar(fraction = resume.watchedFraction, modifier = Modifier.width(RESUME_BAR_WIDTH))
        Text(text = resume.label, style = TvType.caption1, color = TvColors.textSecondary)
    }
}

/** "5 sources" and, while addons have not answered, a spinner with "Searching N more addons". */
@Composable
private fun StreamsSourceSummary(sourceCount: Int, searchingAddonCount: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(SUMMARY_GAP)) {
        if (sourceCount > 0) {
            Text(text = sourceCountText(sourceCount), style = TvType.caption1, color = TvColors.textSecondary)
        }
        if (searchingAddonCount > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(SEARCHING_GAP), verticalAlignment = Alignment.CenterVertically) {
                StreamsSpinner(size = SEARCHING_SPINNER)
                Text(text = searchingText(searchingAddonCount), style = TvType.caption1, color = TvColors.textSecondary)
            }
        }
    }
}

/**
 * A capsule bar (tvOS WatchProgressBar) in the content colour, also the torrent buffer bar of a row. Private to Streams:
 * Home and Details keep their own until it moves to design/.
 */
@Composable
fun StreamsProgressBar(fraction: Float, modifier: Modifier = Modifier, height: Dp = BAR_HEIGHT) {
    val content = LocalContentColor.current
    Box(modifier = modifier.height(height).background(content.copy(alpha = TRACK_ALPHA), CircleShape)) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(content, CircleShape)
        )
    }
}

/** "1 source", "5 sources" (S1). */
fun sourceCountText(sourceCount: Int): String = if (sourceCount == 1) "1 source" else "$sourceCount sources"

/** "Searching 1 more addon", "Searching 3 more addons" (S1). */
fun searchingText(addonCount: Int): String = if (addonCount == 1) "Searching 1 more addon" else "Searching $addonCount more addons"

const val STREAMS_TITLE_TAG = "Streams"

// tvOS literals, pt / 2.
val STREAMS_HEADER_WIDTH = 280.dp // width 560
private val POSTER_SIZE = DpSize(150.dp, 225.dp) // 300 x 450
private val STILL_SIZE = DpSize(STREAMS_HEADER_WIDTH, STREAMS_HEADER_WIDTH * 9 / 16) // 560 wide, 16:9
private const val TITLE_MAX_LINES = 3
private val SUBTITLE_COLOR = Color.White.copy(alpha = 0.75f)
private val HEADER_GAP = 5.dp // spacing 10
private val ARTWORK_BOTTOM = 13.dp // padding(.bottom, 26)
private val RESUME_GAP = 6.dp // spacing 12
private val RESUME_TOP = 4.dp // padding(.top, 8)
private val RESUME_BAR_WIDTH = 180.dp // frame(width: 360)
private val BAR_HEIGHT = 4.dp // WatchProgressBar height 8
private val SUMMARY_TOP = 12.dp // padding(.top, 24)
private val SUMMARY_GAP = 7.dp // spacing 14
private val SEARCHING_GAP = 9.dp // HStack spacing 18
private val SEARCHING_SPINNER = 16.dp

// The track is the platter's 22% of the content colour, so it shows on a focused white row too.
private const val TRACK_ALPHA = 0.22f
