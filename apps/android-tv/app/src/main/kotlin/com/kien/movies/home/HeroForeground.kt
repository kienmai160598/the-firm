package com.kien.movies.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.kien.movies.design.ImdbBadge
import com.kien.movies.design.ModulatedCrossfade
import com.kien.movies.design.Motion
import com.kien.movies.design.TextStyles
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvGrid
import com.kien.movies.design.TvType
import com.kien.movies.design.rememberLogoRequest

/**
 * The spotlight's text (tvOS HeroForeground in HomeHeroPage.swift): logo, facts and overview of
 * the item showing, or the placeholder while Home loads. It stays in place while the item changes
 * and crossfades to the next item's text in [Motion.heroInfoChangeMillis] (AC H1); the alpha
 * applies to each draw call, so the box GPU draws no extra layer. The button is not part of it.
 */
@Composable
internal fun HeroInfo(mode: HeroMode, item: HeroItem?, modifier: Modifier) {
    ModulatedCrossfade(targetState = item, durationMillis = Motion.heroInfoChangeMillis, modifier = modifier) { shown ->
        HeroTextStack(
            modifier = Modifier.fillMaxSize(),
            details = {
                when {
                    shown != null -> HeroDetails(shown)
                    mode == HeroMode.Loading -> HeroPlaceholder()
                }
            }
        )
    }
}

/**
 * Places the text block by baselines, measured on `05-up-hero.png` and `01-home.png`: the button
 * row's top at [BUTTONS_TOP], the overview's last baseline [OVERVIEW_TO_BUTTONS] above it, the
 * metadata baseline [META_TO_OVERVIEW] above the overview's first baseline, the logo's bottom
 * [LOGO_TO_META] above the metadata baseline. [details] emits the logo, the metadata line and the
 * overview, in that order (any may be missing). [button] may be empty.
 */
@Composable
internal fun HeroTextStack(modifier: Modifier, details: @Composable () -> Unit, button: @Composable () -> Unit = {}, centresButton: Boolean = false) {
    Layout(
        modifier = modifier,
        contents = listOf(details, button)
    ) { (detailMeasurables, buttonMeasurables), constraints ->
        val loose = Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight)
        val start = TvGrid.sideMargin.roundToPx()
        val buttonPlaceable = buttonMeasurables.firstOrNull()?.measure(loose)
        val buttonRowTop = BUTTONS_TOP.roundToPx()
        val placeables = detailMeasurables.map { measurable -> measurable.measure(loose) }
        val positions = IntArray(placeables.size)
        if (placeables.size == HERO_DETAIL_PARTS) {
            val (logo, meta, overview) = placeables
            positions[2] = buttonRowTop - OVERVIEW_TO_BUTTONS.roundToPx() - overview.baseline(LastBaseline)
            val metaBaseline = positions[2] + overview.baseline(FirstBaseline) - META_TO_OVERVIEW.roundToPx()
            positions[1] = metaBaseline - meta.baseline(FirstBaseline)
            positions[0] = metaBaseline - LOGO_TO_META.roundToPx() - logo.height
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEachIndexed { i, placeable -> placeable.place(start, positions[i]) }
            when {
                buttonPlaceable == null -> Unit
                centresButton -> buttonPlaceable.place((constraints.maxWidth - buttonPlaceable.width) / 2, constraints.maxHeight / 2 + STATUS_BUTTON_DROP.roundToPx())
                else -> buttonPlaceable.place(start, buttonRowTop + (BUTTON_ROW_HEIGHT.roundToPx() - buttonPlaceable.height) / 2)
            }
        }
    }
}

/** A child's [line], or its bottom when it has no text (a skeleton bar, a logo). */
private fun Placeable.baseline(line: HorizontalAlignmentLine): Int = get(line).takeIf { position -> position != AlignmentLine.Unspecified } ?: height

/** [HeroDetails] and [HeroPlaceholder] emit logo, metadata line and overview. */
private const val HERO_DETAIL_PARTS = 3

/** Logo, metadata line and overview of a featured item, as three children of [HeroTextStack]. */
@Composable
private fun HeroDetails(item: HeroItem) {
    HeroTitle(name = item.name, logoUrl = item.logoUrl)
    Row(horizontalArrangement = Arrangement.spacedBy(META_GAP), verticalAlignment = Alignment.CenterVertically) {
        if (item.facts.isNotEmpty()) {
            Text(text = item.facts.joinToString(FACT_SEPARATOR), style = TvType.caption1, color = TvColors.textPrimary, maxLines = 1)
        }
        item.imdbRating?.let { rating -> ImdbBadge(rating) }
    }
    Text(
        text = item.overview.orEmpty(),
        style = TvType.caption2,
        color = TvColors.textSecondary,
        maxLines = OVERVIEW_MAX_LINES,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.width(OVERVIEW_WIDTH)
    )
}

/** The logo, decoded at its largest size; the title in text until it loads or when it fails. */
@Composable
private fun HeroTitle(name: String, logoUrl: String?) {
    var showsLogo by remember(logoUrl) { mutableStateOf(false) }
    Box(contentAlignment = Alignment.BottomStart, modifier = Modifier.sizeIn(maxWidth = LOGO_MAX.width)) {
        if (!showsLogo) {
            Text(text = name, style = TextStyles.heroTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (logoUrl != null) {
            AsyncImage(
                model = rememberLogoRequest(logoUrl, LOGO_MAX),
                contentDescription = name,
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomStart,
                onSuccess = { showsLogo = true },
                onError = { showsLogo = false },
                modifier = Modifier.size(LOGO_MAX)
            )
        }
    }
}

/** Same layout as a featured item, so the screen does not jump when the real hero replaces it. */
@Composable
private fun HeroPlaceholder() {
    SkeletonBar(DpSize(200.dp, 50.dp))
    SkeletonBar(DpSize(170.dp, 12.dp))
    SkeletonBar(DpSize(280.dp, 24.dp))
}

@Composable
private fun SkeletonBar(size: DpSize) {
    Box(modifier = Modifier.size(size).background(SKELETON_FILL, SKELETON_SHAPE))
}

private const val FACT_SEPARATOR = " · "
private const val OVERVIEW_MAX_LINES = 3

// Measured, pt / 2: buttons 706 to 774 pt focused (68 pt; rest 64 pt), primary 306 pt wide focused;
// overview last baseline 669 pt; metadata baseline 614 pt; logo bottom 569 pt; logo up to
// 420 x 125 pt (spec 420 x 105; `05-up-hero.png` logo is 123 pt tall); overview wraps at about 560 pt.
private val BUTTONS_TOP = 353.dp
private val BUTTON_ROW_HEIGHT = 34.dp
private val OVERVIEW_TO_BUTTONS = 18.5.dp
private val META_TO_OVERVIEW = 27.5.dp
private val LOGO_TO_META = 22.5.dp
private val LOGO_MAX = DpSize(210.dp, 62.dp)
private val OVERVIEW_WIDTH = 280.dp
private val META_GAP = 8.dp
private val SKELETON_FILL = Color.White.copy(alpha = 0.12f)
private val SKELETON_SHAPE = RoundedCornerShape(4.dp)
private val STATUS_BUTTON_DROP = 36.dp
