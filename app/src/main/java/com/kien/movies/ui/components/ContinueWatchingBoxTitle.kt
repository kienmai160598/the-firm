package com.kien.movies.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.Text
import com.kien.movies.ui.theme.isExpressiveBuild

private const val ARTWORK_TITLE_MAX_LINES = 2
private const val PLACEHOLDER_TITLE_MAX_LINES = 1

/**
 * On the box build a movie's title on the landscape Continue Watching card ends in an ellipsis.
 * Upstream's one-line marquee clips the title mid-word once its three passes end, so a long
 * title reads as cut off. Episodes keep the marquee: their code and episode title already take
 * two lines on the card.
 */
internal fun continueWatchingTitleEllipsizes(episodeStr: String?): Boolean =
    isExpressiveBuild && episodeStr == null

/**
 * Two lines over artwork; one line over the no-artwork placeholder, whose centred play circle
 * a second line would run into.
 */
@Composable
internal fun ContinueWatchingEllipsizedTitle(
    text: String,
    style: TextStyle,
    color: Color,
    hasArtwork: Boolean
) {
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = if (hasArtwork) ARTWORK_TITLE_MAX_LINES else PLACEHOLDER_TITLE_MAX_LINES,
        overflow = TextOverflow.Ellipsis
    )
}
