package com.kien.movies.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.kien.movies.R
import com.kien.movies.design.BakedScrim
import com.kien.movies.design.ImdbBadge
import com.kien.movies.design.Motion
import com.kien.movies.design.ScrimLayer
import com.kien.movies.design.TextStyles
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvGrid
import com.kien.movies.design.TvShapes
import com.kien.movies.design.TvType
import com.kien.movies.design.rememberBakedImage
import com.kien.movies.shell.AppRoute
import com.kien.movies.shell.RootTab
import kotlinx.coroutines.delay

/**
 * The top of Home (docs/tv-look-spec.md, section 3, "Hero"), the first item of Home's list,
 * [HERO_SLOT_HEIGHT] tall. Its art is a full screen opaque bitmap with the scrim baked in
 * ([BakedScrim]); below the slot it shows under the peek of the first row only while the hero has
 * focus ([showsArtBelow]), else Home's rows background starts at the slot's end, as in
 * `03-sidebar-down.png`. A focus anywhere in the hero brings the whole slot into view, so the list
 * scrolls back to the top. The text block moves up faster than the list ([PARALLAX]).
 *
 * - [HeroMode.Featured]: one item of the first row at a time, with the page dots. Left and right
 *   on the button change the item; left on the first item is not taken, so focus goes on to the
 *   nav pill (AC N3). The item advances every 8 s while the hero does not have focus (AC H1).
 * - [HeroMode.Loading]: a placeholder of the same layout.
 * - [HeroMode.Status]: [HomeStatus].
 *
 * One button serves all three, called from one place, so the focused node survives the change
 * from loading to loaded and launch focus stays on it (AC N1). Trailer autoplay is off on the box.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeHero(
    mode: HeroMode,
    items: List<HeroItem>,
    statusMessage: String?,
    focusMemory: HomeFocusMemory,
    onOpen: (AppRoute) -> Unit,
    showsArtBelow: State<Boolean>,
    onFocusChanged: (hasFocus: Boolean) -> Unit,
    scrollOffset: () -> Float,
    modifier: Modifier = Modifier
) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val shown = if (items.isEmpty()) 0 else index.coerceIn(0, items.lastIndex)
    val current = items.getOrNull(shown).takeIf { mode == HeroMode.Featured }
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(HERO_SLOT_HEIGHT)
            .bringIntoViewResponder(rememberWholeSlotResponder())
            .onFocusChanged { state ->
                isFocused = state.hasFocus
                onFocusChanged(state.hasFocus)
            }
    ) {
        if (current != null) {
            HeroArt(
                item = current,
                dots = if (items.size > 1) items.size else 0,
                shownIndex = shown,
                showsArtBelow = showsArtBelow
            )
        }
        val parallax = Modifier.graphicsLayer { translationY = -scrollOffset() * PARALLAX }
        when (mode) {
            HeroMode.Status -> HomeStatus(statusMessage, Modifier.align(Alignment.Center).padding(bottom = STATUS_TEXT_LIFT))
            HeroMode.Featured, HeroMode.Loading, HeroMode.None -> Unit
        }
        HeroTextStack(
            modifier = Modifier.fillMaxSize().then(parallax),
            details = {
                when {
                    current != null -> HeroDetails(current)
                    mode == HeroMode.Loading -> HeroPlaceholder()
                }
            },
            button = {
                HeroButton(
                    mode = mode,
                    itemName = current?.name,
                    onClick = {
                        when (mode) {
                            HeroMode.Featured -> current?.let { item -> onOpen(item.route) }
                            HeroMode.Status -> onOpen(AppRoute.Tab(RootTab.Addons))
                            HeroMode.Loading, HeroMode.None -> Unit
                        }
                    },
                    onStep = { step ->
                        val next = shown + step
                        if (mode == HeroMode.Featured && next in items.indices) index = next
                        // Right is always taken on the hero, so it never jumps down to the first row.
                        mode == HeroMode.Featured && (step > 0 || next >= 0)
                    },
                    modifier = Modifier.rememberedFocus(focusMemory, HERO_FOCUS_KEY)
                )
            },
            centresButton = mode == HeroMode.Status
        )
    }

    if (mode == HeroMode.Featured && items.size > 1 && !isFocused) {
        LaunchedEffect(shown, items.size) {
            delay(Motion.heroAdvanceMillis)
            index = (shown + 1) % items.size
        }
    }
}

/** Any focus in the hero asks for the whole slot, so the list returns to its top. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberWholeSlotResponder(): BringIntoViewResponder {
    val density = LocalDensity.current
    return remember(density) {
        val slotHeight = with(density) { HERO_SLOT_HEIGHT.toPx() }
        object : BringIntoViewResponder {
            override fun calculateRectForParent(localRect: Rect): Rect = Rect(0f, 0f, localRect.right, slotHeight)

            override suspend fun bringChildIntoView(localRect: () -> Rect?) = Unit
        }
    }
}

/**
 * The art, a full screen tall from the slot's top, drawn past the slot's bottom only while
 * [showsArtBelow] (read in the draw phase, so a focus change redraws without recomposing).
 * The page dots sit in the part below the slot.
 */
@Composable
private fun HeroArt(item: HeroItem, dots: Int, shownIndex: Int, showsArtBelow: State<Boolean>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(align = Alignment.Top, unbounded = true)
            .height(SCREEN_HEIGHT)
            .drawWithContent {
                if (showsArtBelow.value) {
                    drawContent()
                } else {
                    clipRect(bottom = HERO_SLOT_HEIGHT.toPx()) { this@drawWithContent.drawContent() }
                }
            }
    ) {
        AsyncImage(
            model = rememberBakedImage(item.backdropUrl, DpSize(SCREEN_WIDTH, SCREEN_HEIGHT), heroScrim),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (dots > 0) {
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
 * Places the text block by baselines, measured on `05-up-hero.png` and `01-home.png`: the button
 * row's top at [BUTTONS_TOP], the overview's last baseline [OVERVIEW_TO_BUTTONS] above it, the
 * metadata baseline [META_TO_OVERVIEW] above the overview's first baseline, the logo's bottom
 * [LOGO_TO_META] above the metadata baseline. [details] emits the logo, the metadata line and the
 * overview, in that order (any may be missing).
 */
@Composable
private fun HeroTextStack(modifier: Modifier, details: @Composable () -> Unit, button: @Composable () -> Unit, centresButton: Boolean) {
    Layout(
        modifier = modifier,
        contents = listOf(details, button)
    ) { (detailMeasurables, buttonMeasurables), constraints ->
        val loose = Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight)
        val start = TvGrid.sideMargin.roundToPx()
        val buttonPlaceable = buttonMeasurables.first().measure(loose)
        val buttonRowTop = BUTTONS_TOP.roundToPx()
        val buttonY = buttonRowTop + (BUTTON_ROW_HEIGHT.roundToPx() - buttonPlaceable.height) / 2
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
            if (centresButton) {
                buttonPlaceable.place((constraints.maxWidth - buttonPlaceable.width) / 2, constraints.maxHeight / 2 + STATUS_BUTTON_DROP.roundToPx())
            } else {
                buttonPlaceable.place(start, buttonY)
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
                model = rememberSizedImage(logoUrl, LOGO_MAX),
                contentDescription = name,
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomStart,
                onSuccess = { showsLogo = true },
                onError = { showsLogo = false },
                modifier = Modifier.sizeIn(maxWidth = LOGO_MAX.width, maxHeight = LOGO_MAX.height)
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

/**
 * Watch Now on a featured item, a placeholder while loading, Open Addons on the status: the
 * primary capsule (glass at rest, white on focus). [onStep] gets -1 for left and 1 for right and
 * says whether it took the key.
 */
@Composable
private fun HeroButton(mode: HeroMode, itemName: String?, onClick: () -> Unit, onStep: (Int) -> Boolean, modifier: Modifier) {
    val tag = when (mode) {
        HeroMode.Featured, HeroMode.None -> HERO_BUTTON_TAG
        HeroMode.Loading -> HERO_PLACEHOLDER_TAG
        HeroMode.Status -> HOME_STATUS_TAG
    }
    Button(
        onClick = onClick,
        modifier = modifier
            .size(PRIMARY_SIZE)
            .testTag(tag)
            .semantics {
                if (mode == HeroMode.Loading) contentDescription = LOADING_LABEL
                if (itemName != null) stateDescription = itemName
            }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> onStep(-1)
                    Key.DirectionRight -> onStep(1)
                    else -> false
                }
            },
        colors = ButtonDefaults.colors(
            containerColor = TvColors.platter,
            contentColor = TvColors.textPrimary,
            focusedContainerColor = TvColors.focusedFill,
            focusedContentColor = TvColors.focusedContent,
            pressedContainerColor = TvColors.focusedFill,
            pressedContentColor = TvColors.focusedContent
        ),
        scale = ButtonDefaults.scale(focusedScale = PRIMARY_FOCUS_SCALE),
        shape = ButtonDefaults.shape(shape = TvShapes.pill),
        border = ButtonDefaults.border(border = glassBorder, focusedBorder = Border.None, pressedBorder = Border.None),
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
    ) {
        val labelColor = if (mode == HeroMode.Loading) LocalContentColor.current.copy(alpha = REDACTED_ALPHA) else LocalContentColor.current
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            if (mode != HeroMode.Status) {
                Icon(
                    painter = painterResource(R.drawable.ic_home_play),
                    contentDescription = null,
                    tint = labelColor,
                    modifier = Modifier.size(PLAY_ICON)
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            }
            Text(text = if (mode == HeroMode.Status) OPEN_ADDONS else WATCH_NOW, style = TvType.bodyEmphasized, color = labelColor, maxLines = 1)
        }
    }
}

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

const val HERO_BUTTON_TAG = "home-hero-button"
const val HERO_PLACEHOLDER_TAG = "home-hero-placeholder"
const val HOME_STATUS_TAG = "home-status"
const val WATCH_NOW = "Watch Now"
private const val LOADING_LABEL = "Loading"
private const val REDACTED_ALPHA = 0.35f
private const val FACT_SEPARATOR = " · "
private const val OVERVIEW_MAX_LINES = 3

/**
 * The hero's slot in Home's list. Measured on `03-sidebar-down.png`: with Continue Watching
 * focused (cards at y 426 pt), the art ends at y 324 pt, 102 pt above the cards. With the cards at
 * y 931 pt on the unscrolled Home (`01-home.png`), the slot ends at 829 pt.
 */
internal val HERO_SLOT_HEIGHT: Dp = 414.5.dp

/** Text block over list scroll: the buttons move 619 pt while the cards move 504 pt (`05` to `03`). */
private const val PARALLAX = 0.228f
private const val PRIMARY_FOCUS_SCALE = 1.05f

private val SCREEN_WIDTH = 960.dp
private val SCREEN_HEIGHT = 540.dp

// Measured, pt / 2: buttons 706 to 774 pt focused (68 pt; rest 64 pt), primary 306 pt wide focused;
// overview last baseline 669 pt; metadata baseline 614 pt; logo bottom 569 pt; logo up to
// 420 x 125 pt (spec 420 x 105; `05-up-hero.png` logo is 123 pt tall); overview wraps at about 560 pt.
private val BUTTONS_TOP = 353.dp
private val BUTTON_ROW_HEIGHT = 34.dp
private val PRIMARY_SIZE = DpSize(148.dp, 32.dp)
private val PLAY_ICON = 14.dp
private val OVERVIEW_TO_BUTTONS = 18.5.dp
private val META_TO_OVERVIEW = 27.5.dp
private val LOGO_TO_META = 22.5.dp
private val LOGO_MAX = DpSize(210.dp, 62.dp)
private val OVERVIEW_WIDTH = 280.dp
private val META_GAP = 8.dp

// Page dots measured on `03-sidebar-down.png` and `05-up-hero.png`: centred at y 868 pt, dots
// 9 pt on a 21 pt pitch, current 30 pt wide, on a black 50% capsule 27 pt tall.
private val DOTS_CENTRE_Y = 434.dp
private val DOTS_PLATTER_HEIGHT = 13.5.dp
private val DOTS_PADDING = 8.dp
private val DOT_SIZE = 4.5.dp
private val DOT_GAP = 6.dp
private val DOT_CURRENT_WIDTH = 15.dp
private val DOTS_PLATTER = Color.Black.copy(alpha = 0.5f)
private val DOT_REST = Color.White.copy(alpha = 0.3f)

private val SKELETON_FILL = Color.White.copy(alpha = 0.12f)
private val SKELETON_SHAPE = RoundedCornerShape(4.dp)
private val glassBorder = Border(border = BorderStroke(1.dp, TvColors.glassEdge), shape = TvShapes.pill)

// The status text sits above the centre and the button below it.
private val STATUS_TEXT_LIFT = 40.dp
private val STATUS_BUTTON_DROP = 36.dp

/**
 * The hero art with its shades baked in. Stops from `01-home.png`: the sky is about 32% darker at
 * the left edge than right of the text block, 13% at x 300 pt and clear from about x 560 pt; the
 * bottom darkens by about 40% under the first row.
 */
private val heroScrim = BakedScrim(
    underlay = TvColors.homeRows,
    layers = listOf(
        ScrimLayer.horizontal(0f to Color.Black.copy(alpha = 0.4f), 0.16f to Color.Black.copy(alpha = 0.2f), 0.33f to Color.Transparent),
        ScrimLayer.vertical(0.7f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.45f))
    )
)
