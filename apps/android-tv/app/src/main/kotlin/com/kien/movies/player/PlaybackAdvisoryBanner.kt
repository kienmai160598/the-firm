package com.kien.movies.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.kien.movies.design.Spacing
import com.nuvio.tvos.rules.PlaybackAdvisory
import kotlinx.coroutines.delay

const val PLAYBACK_ADVISORY_TAG = "playback-advisory"

// tvOS PlaybackAdvisoryBanner.swift literals, pt / 2.
private const val SHOWN_FOR_MILLIS = 6_000L
private const val FADE_MILLIS = 600
private val TopInset = 30.dp // 60 pt
private val ItemGap = 9.dp // 18 pt
private val RatingPadding = 5.dp // 10 pt
private val RatingPaddingVertical = 2.dp // 3 pt
private val RatingBorder = 1.dp // 2 pt
private val RatingCorner = 3.dp // 6 pt
private val SlideFrom = 8.dp // 16 pt
private val TextShadow = Shadow(color = Color.Black.copy(alpha = 0.6f), blurRadius = 10f)
private val RatingStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, shadow = TextShadow) // 24 pt
private val GenreStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, shadow = TextShadow) // 26 pt

/**
 * The rating and genres, top left, once for 6 s after playback starts (tvOS
 * PlaybackAdvisoryBanner.swift, AC P6). Text with a shadow, no panel behind it.
 */
@Composable
fun PlaybackAdvisoryBanner(advisory: PlaybackAdvisory, hasPlaybackStarted: Boolean, modifier: Modifier = Modifier) {
    var hasShown by rememberSaveable { mutableStateOf(false) }
    var isShown by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(hasPlaybackStarted) {
        if (!hasPlaybackStarted || hasShown) return@LaunchedEffect
        hasShown = true
        isShown = true
        delay(SHOWN_FOR_MILLIS)
        isShown = false
    }
    val slide = with(LocalDensity.current) { SlideFrom.roundToPx() }
    AnimatedVisibility(
        visible = isShown,
        enter = fadeIn(tween(FADE_MILLIS)) + slideInHorizontally(tween(FADE_MILLIS)) { -slide },
        exit = fadeOut(tween(FADE_MILLIS)) + slideOutHorizontally(tween(FADE_MILLIS)) { -slide },
        modifier = modifier.padding(start = Spacing.screenHorizontal, top = TopInset)
    ) {
        val description = listOfNotNull(advisory.ageRating?.let { "Rated $it" }, advisory.genreLine).joinToString(", ")
        Row(
            horizontalArrangement = Arrangement.spacedBy(ItemGap),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag(PLAYBACK_ADVISORY_TAG).semantics(mergeDescendants = true) { contentDescription = description }
        ) {
            advisory.ageRating?.let { rating ->
                Text(
                    text = rating,
                    style = RatingStyle,
                    modifier = Modifier
                        .border(RatingBorder, Color.White.copy(alpha = 0.85f), RoundedCornerShape(RatingCorner))
                        .padding(horizontal = RatingPadding, vertical = RatingPaddingVertical)
                )
            }
            advisory.genreLine?.let { genres ->
                Text(text = genres, style = GenreStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
