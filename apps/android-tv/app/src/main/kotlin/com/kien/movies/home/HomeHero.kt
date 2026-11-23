package com.kien.movies.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.kien.movies.R
import com.kien.movies.design.Backdrop
import com.kien.movies.design.GlassStyle
import com.kien.movies.design.MetadataLine
import com.kien.movies.design.Motion
import com.kien.movies.design.RailSize
import com.kien.movies.design.Spacing
import com.kien.movies.design.TextStyles
import com.kien.movies.design.glassPanel
import com.kien.movies.shell.AppRoute
import com.kien.movies.shell.RootTab
import kotlinx.coroutines.delay

/**
 * The top of Home (tvOS HomeHero.swift), in the hero slot of the list, as tall as the screen
 * minus the peek of the next shelf (the caller sets the height).
 *
 * - [HeroMode.Featured]: one item of the first row at a time over its backdrop, with a page
 *   indicator. Left and right on the button change the item; left on the first item is not
 *   taken, so focus goes on to the sidebar (AC N3). The item advances every 8 s while the hero
 *   does not have focus (AC H1).
 * - [HeroMode.Loading]: a placeholder of the same layout.
 * - [HeroMode.Status]: [HomeStatus].
 *
 * One button serves all three, called from one place, so the focused node survives the change
 * from loading to loaded and launch focus stays on it (AC N1).
 *
 * Box GPU: one backdrop with its one leading scrim (no second, bottom shade as on tvOS) and no
 * slide between items: the next item replaces the current one.
 */
@Composable
fun HomeHero(
    mode: HeroMode,
    items: List<HeroItem>,
    statusMessage: String?,
    buttonFocus: FocusRequester,
    onOpen: (AppRoute) -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier
) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val shown = if (items.isEmpty()) 0 else index.coerceIn(0, items.lastIndex)
    val current = items.getOrNull(shown).takeIf { mode == HeroMode.Featured }
    var isFocused by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        when (mode) {
            HeroMode.Featured -> current?.let { item -> HeroPage(item, Modifier.fillMaxSize()) }
            HeroMode.Loading -> HeroPlaceholder(Modifier.align(Alignment.BottomStart))
            HeroMode.Status -> HomeStatus(statusMessage, Modifier.align(Alignment.Center).padding(bottom = STATUS_TEXT_LIFT))
            HeroMode.None -> Unit
        }
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
            modifier = Modifier
                .align(if (mode == HeroMode.Status) Alignment.Center else Alignment.BottomStart)
                .padding(buttonPadding(mode))
                .focusRequester(buttonFocus)
                .onFocusChanged { state ->
                    if (state.isFocused && !isFocused) onFocused()
                    isFocused = state.isFocused
                }
        )
        if (mode == HeroMode.Featured && items.size > 1) {
            PageIndicator(
                count = items.size,
                index = shown,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = INDICATOR_BOTTOM)
            )
        }
    }

    if (mode == HeroMode.Featured && items.size > 1 && !isFocused) {
        LaunchedEffect(shown, items.size) {
            delay(Motion.heroAdvanceMillis)
            index = (shown + 1) % items.size
        }
    }
}

private fun buttonPadding(mode: HeroMode) = if (mode == HeroMode.Status) {
    PaddingValues(top = STATUS_BUTTON_DROP)
} else {
    PaddingValues(start = RailSize.contentStart, bottom = HERO_BOTTOM_PADDING)
}

/**
 * Watch Now on a featured item, a placeholder while loading, Open Addons on the status.
 * [onStep] gets -1 for left and 1 for right and says whether it took the key.
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
            .height(HERO_BUTTON_HEIGHT)
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
        colors = GlassStyle.prominentButtonColors(),
        scale = GlassStyle.buttonScale(),
        shape = GlassStyle.buttonShape(),
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
    ) {
        val labelColor = if (mode == HeroMode.Loading) LocalContentColor.current.copy(alpha = REDACTED_ALPHA) else LocalContentColor.current
        if (mode != HeroMode.Status) {
            Icon(
                painter = painterResource(R.drawable.ic_home_play),
                contentDescription = null,
                tint = labelColor,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        }
        Text(
            text = if (mode == HeroMode.Status) OPEN_ADDONS else WATCH_NOW,
            style = TextStyles.button.copy(fontWeight = FontWeight.SemiBold),
            color = labelColor
        )
    }
}

@Composable
private fun HeroPage(item: HeroItem, modifier: Modifier) {
    Box(modifier = modifier) {
        Backdrop(url = item.backdropUrl, modifier = Modifier.fillMaxSize())
        HeroDetails(
            item = item,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = RailSize.contentStart, end = Spacing.screenHorizontal, bottom = DETAILS_BOTTOM)
                .widthIn(max = Spacing.heroTextWidth)
        )
    }
}

@Composable
private fun HeroDetails(item: HeroItem, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(HERO_STACK_GAP)) {
        HeroTitle(name = item.name, logoUrl = item.logoUrl)
        CompositionLocalProvider(LocalContentColor provides FACTS_COLOR) {
            ProvideTextStyle(TextStyles.body.copy(fontWeight = FontWeight.SemiBold)) {
                MetadataLine(facts = item.facts, imdbRating = item.imdbRating)
            }
        }
        item.overview?.let { overview ->
            Text(
                text = overview,
                style = TextStyles.body,
                color = OVERVIEW_COLOR,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** The logo, decoded at its largest size; the title in text until it loads or when it fails. */
@Composable
private fun HeroTitle(name: String, logoUrl: String?) {
    var showsLogo by remember(logoUrl) { mutableStateOf(false) }
    Box(contentAlignment = Alignment.BottomStart) {
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

/** Same layout as a hero page, so the screen does not jump when the real hero replaces it. */
@Composable
private fun HeroPlaceholder(modifier: Modifier) {
    Column(
        modifier = modifier.padding(start = RailSize.contentStart, bottom = DETAILS_BOTTOM),
        verticalArrangement = Arrangement.spacedBy(HERO_STACK_GAP)
    ) {
        SkeletonBar(DpSize(260.dp, 45.dp))
        SkeletonBar(DpSize(190.dp, 14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SkeletonBar(DpSize(380.dp, 12.dp))
            SkeletonBar(DpSize(320.dp, 12.dp))
        }
    }
}

@Composable
private fun SkeletonBar(size: DpSize) {
    Box(modifier = Modifier.size(size).background(SKELETON_FILL, SKELETON_SHAPE))
}

@Composable
private fun PageIndicator(count: Int, index: Int, modifier: Modifier) {
    Row(
        modifier = modifier
            .glassPanel(cornerRadius = INDICATOR_CORNER)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(INDICATOR_DOT)
    ) {
        repeat(count) { position ->
            val isCurrent = position == index
            Box(
                modifier = Modifier
                    .size(width = if (isCurrent) INDICATOR_CURRENT_WIDTH else INDICATOR_DOT, height = INDICATOR_DOT)
                    .background(if (isCurrent) Color.White else INDICATOR_REST, CircleShape)
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

// tvOS literals, pt / 2: button height 66 to 76 pt, button 70 pt above the hero's bottom with
// 6 pt above it, stack spacing 22 pt, logo at most 620 x 200 pt, indicator 36 pt from the bottom
// with 10 pt dots, a 28 pt current dot and a 20 pt corner.
private val HERO_BUTTON_HEIGHT = 36.dp
private val HERO_BOTTOM_PADDING = 35.dp
private val HERO_STACK_GAP = 11.dp
private val DETAILS_BOTTOM = HERO_BOTTOM_PADDING + HERO_BUTTON_HEIGHT + 3.dp + HERO_STACK_GAP
private val LOGO_MAX = DpSize(310.dp, 100.dp)
private val INDICATOR_BOTTOM = 18.dp
private val INDICATOR_DOT = 5.dp
private val INDICATOR_CURRENT_WIDTH = 14.dp
private val INDICATOR_CORNER = 10.dp
private val INDICATOR_REST = Color.White.copy(alpha = 0.35f)
private val FACTS_COLOR = Color.White.copy(alpha = 0.8f)
private val OVERVIEW_COLOR = Color.White.copy(alpha = 0.85f)
private val SKELETON_FILL = Color.White.copy(alpha = 0.12f)
private val SKELETON_SHAPE = RoundedCornerShape(4.dp)

// The status text sits above the centre and the button below it.
private val STATUS_TEXT_LIFT = 40.dp
private val STATUS_BUTTON_DROP = 56.dp
