package com.kien.movies.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kien.movies.design.BakedScrim
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvShapes
import com.kien.movies.design.rememberBakedImage
import com.kien.movies.trailer.HeroTrailer
import com.kien.movies.trailer.PlayTrailerWhile
import com.kien.movies.trailer.TrailerBackdrop
import com.nuvio.tvos.TvosTrailers

/**
 * The hero's art, drawn behind Home's list as tvOS draws `HeroBackdrop` behind its shelves
 * (HomeHeroPage.swift): [ART_HEIGHT] tall from the top of the page, so it runs under the first
 * shelf, with the tvOS shades and the bottom fade into the Home colour baked into the bitmap
 * ([BakedScrim.homeHero]). It moves with the list ([heroScroll], read in the draw phase), so the
 * rows slide over its faded end and no edge shows. Behind the list, it outlives the hero item,
 * which a lazy list drops once the slot has scrolled away while the art's end still shows. The
 * art fades into the focused item's [trailer] (AC H8).
 */
@Composable
fun HomeHeroArt(mode: HeroMode, items: List<HeroItem>, index: Int, listState: LazyListState, trailer: HeroTrailer) {
    val shown = shownHeroIndex(index, items.size)
    val item = items.getOrNull(shown)?.takeIf { mode == HeroMode.Featured } ?: return
    val heroHeight = with(LocalDensity.current) { HERO_SLOT_HEIGHT.toPx() }
    HeroArt(item = item, dots = items.size, shownIndex = shown, trailer = trailer) {
        heroScroll(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset, heroHeight)
    }
}

@Composable
private fun HeroArt(item: HeroItem, dots: Int, shownIndex: Int, trailer: HeroTrailer, heroScroll: () -> Float?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ART_HEIGHT)
            .graphicsLayer { translationY = -(heroScroll() ?: ART_HEIGHT.toPx()) }
    ) {
        val model = rememberBakedImage(item.backdropUrl, DpSize(ART_WIDTH, ART_HEIGHT), heroScrim)
        TrailerBackdrop(trailer = trailer, itemId = item.metaId, scrim = heroScrim, modifier = Modifier.fillMaxSize()) { imageModifier ->
            AsyncImage(model = model, contentDescription = null, contentScale = ContentScale.Crop, modifier = imageModifier)
        }
        if (dots > 1) {
            PageDots(
                count = dots,
                index = shownIndex,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = DOTS_CENTRE_Y - DOTS_PLATTER_HEIGHT / 2)
            )
        }
    }
}

/**
 * The focused spotlight item's trailer plays after a second of focus on the hero (AC H8); the links
 * of the item showing and the next one are looked up ahead.
 */
@Composable
fun HeroTrailerFollowsFocus(trailer: HeroTrailer, mode: HeroMode, items: List<HeroItem>, index: Int, heroHasFocus: Boolean) {
    val shown = shownHeroIndex(index, items.size)
    val item = items.getOrNull(shown)?.takeIf { mode == HeroMode.Featured }
    PlayTrailerWhile(trailer, item?.type, item?.metaId, isDue = heroHasFocus)
    LaunchedEffect(item?.metaId) {
        listOfNotNull(item, items.getOrNull(shown + 1)).forEach { next -> TvosTrailers.prefetch(next.type, next.metaId) }
    }
}

/**
 * How far the hero slot has scrolled off the top, from the list's first visible item: the hero is
 * item 0 and [heroHeight] tall. Null once a later item is first, when the art is off screen.
 */
internal fun heroScroll(firstIndex: Int, firstOffset: Int, heroHeight: Float): Float? = when (firstIndex) {
    0 -> firstOffset.toFloat()
    1 -> heroHeight + firstOffset
    else -> null
}

/** The item index the hero shows: [index] kept inside the list of [count] items. */
internal fun shownHeroIndex(index: Int, count: Int): Int = if (count == 0) 0 else index.coerceIn(0, count - 1)

/** Page dots on a dark capsule; the current dot is a short bar. */
@Composable
private fun PageDots(count: Int, index: Int, modifier: Modifier) {
    Row(
        modifier = modifier
            .height(DOTS_PLATTER_HEIGHT)
            .background(DOTS_PLATTER, TvShapes.pill)
            .padding(horizontal = DOTS_PADDING),
        horizontalArrangement = Arrangement.spacedBy(DOT_GAP),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { position ->
            val isCurrent = position == index
            Box(
                modifier = Modifier
                    .size(width = if (isCurrent) DOT_CURRENT_WIDTH else DOT_SIZE, height = DOT_SIZE)
                    .background(if (isCurrent) Color.White else DOT_REST, CircleShape)
            )
        }
    }
}

/**
 * tvOS: the page is the screen above the bottom safe area less the 150 pt peek of the first shelf
 * (870 pt), and the art runs the peek plus the 60 pt safe area further (HomeHero.swift
 * `artExtension`): one screen, 1080 pt.
 */
private val ART_HEIGHT: Dp = 540.dp
private val ART_WIDTH: Dp = 960.dp
private val heroScrim = BakedScrim.homeHero(TvColors.homeRows)

// Page dots: tvOS puts them 36 pt above the page's end (HomeHero.swift), centred at y 819 pt on
// the simulator; here as far above the slot's end. Dots 9 pt on a 21 pt pitch, current 30 pt
// wide, on a black 50% capsule 27 pt tall.
private val DOTS_CENTRE_Y = HERO_SLOT_HEIGHT - 25.5.dp
private val DOTS_PLATTER_HEIGHT = 13.5.dp
private val DOTS_PADDING = 8.dp
private val DOT_SIZE = 4.5.dp
private val DOT_GAP = 6.dp
private val DOT_CURRENT_WIDTH = 15.dp
private val DOTS_PLATTER = Color.Black.copy(alpha = 0.5f)
private val DOT_REST = Color.White.copy(alpha = 0.3f)
