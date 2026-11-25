package com.kien.movies.streams

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import com.kien.movies.design.BakedScrim
import com.kien.movies.design.CardShape
import com.kien.movies.design.ScrimLayer
import com.kien.movies.design.TextStyles
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvType
import com.kien.movies.design.rememberBakedImage
import com.nuvio.tvos.TvosResumePoint
import com.nuvio.tvos.TvosStreamTarget

/**
 * The left column of Streams (tvOS StreamsHeader.swift), [STREAMS_HEADER_WIDTH] wide and as tall as
 * the page. The art fills it; the title, the subtitle (episode line), the resume bar when opened to
 * resume directly, and the source count with "Searching N more addons" sit on its shaded bottom
 * (S1). It stays put while the list beside it scrolls; nothing in it takes focus.
 */
@Composable
fun StreamsHeader(
    target: TvosStreamTarget,
    resume: TvosResumePoint?,
    sourceCount: Int,
    searchingAddonCount: Int,
    isResumingLastLink: Boolean,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.width(STREAMS_HEADER_WIDTH).fillMaxHeight().clip(CardShape.shape)) {
        StreamsArtwork(target = target, size = DpSize(maxWidth, maxHeight))
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(TEXT_INSET),
            verticalArrangement = Arrangement.spacedBy(HEADER_GAP)
        ) {
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
            if (isResumingLastLink) {
                SpinnerLine(text = RESUMING_LAST_LINK, modifier = Modifier.padding(top = SUMMARY_TOP))
            } else {
                StreamsSourceSummary(
                    sourceCount = sourceCount,
                    searchingAddonCount = searchingAddonCount,
                    modifier = Modifier.padding(top = SUMMARY_TOP)
                )
            }
        }
    }
}

/**
 * The poster, else the episode still, else the backdrop, cropped to fill the column, with the
 * shade under the text baked into the bitmap (box rule: no gradient drawn per frame). A plain
 * platter when there is no art.
 */
@Composable
private fun StreamsArtwork(target: TvosStreamTarget, size: DpSize) {
    val url = target.posterUrl ?: target.episodeThumbnailUrl ?: target.backdropUrl
    ArtworkImage(model = rememberBakedImage(url, size, ArtworkShade), modifier = Modifier.size(size))
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
        if (searchingAddonCount > 0) SpinnerLine(text = searchingText(searchingAddonCount))
    }
}

@Composable
private fun SpinnerLine(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(SEARCHING_GAP), verticalAlignment = Alignment.CenterVertically) {
        StreamsSpinner(size = SEARCHING_SPINNER)
        Text(text = text, style = TvType.caption1, color = TvColors.textSecondary)
    }
}

/** "1 source", "5 sources" (S1). */
fun sourceCountText(sourceCount: Int): String = if (sourceCount == 1) "1 source" else "$sourceCount sources"

/** "Searching 1 more addon", "Searching 3 more addons" (S1). */
fun searchingText(addonCount: Int): String = if (addonCount == 1) "Searching 1 more addon" else "Searching $addonCount more addons"

const val STREAMS_TITLE_TAG = "Streams"
private const val RESUMING_LAST_LINK = "Resuming the last source"

// tvOS literals, pt / 2.
val STREAMS_HEADER_WIDTH = 320.dp // width 640
private val TEXT_INSET = 20.dp // textInset 40
private const val TITLE_MAX_LINES = 3
private val SUBTITLE_COLOR = Color.White.copy(alpha = 0.75f)
private val HEADER_GAP = 5.dp // spacing 10
private val RESUME_GAP = 6.dp // spacing 12
private val RESUME_TOP = 4.dp // padding(.top, 8)
private val RESUME_BAR_WIDTH = 180.dp // frame(width: 360)
private val SUMMARY_TOP = 12.dp // padding(.top, 24)
private val SUMMARY_GAP = 7.dp // spacing 14
private val SEARCHING_GAP = 9.dp // HStack spacing 18
private val SEARCHING_SPINNER = 16.dp

// StreamsArtwork.shade: clear to 35%, then black 85% at the bottom.
private val ArtworkShade = BakedScrim(
    underlay = TvColors.artworkPlaceholder,
    layers = listOf(ScrimLayer.vertical(0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.85f)))
)
