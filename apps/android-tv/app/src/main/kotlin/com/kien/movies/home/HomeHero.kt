package com.kien.movies.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.design.Motion
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvShapes
import com.kien.movies.design.TvType
import com.kien.movies.design.focusLift
import com.kien.movies.settings.SettingsPage
import com.kien.movies.shell.AppRoute
import com.kien.movies.shell.LocalNavPillState
import kotlinx.coroutines.delay

/**
 * The top of Home (docs/tv-look-spec.md, section 3, "Hero"), the first item of Home's list,
 * [HERO_SLOT_HEIGHT] tall: the text block ([HeroInfo], which crossfades to the next item) and the
 * button, which stays. The art is [HomeHeroArt], behind the
 * list. Both move with the list, as the tvOS page does: no parallax. A focus anywhere in the hero
 * brings the whole slot into view, so the list scrolls back to the top.
 *
 * - [HeroMode.Featured]: one item of the first row at a time, with the page dots. Left and right
 *   on the button change the item; left on the first item is not taken, so focus goes on to the
 *   nav pill (AC N3). The item advances every 8 s while the hero does not have focus (AC H1).
 * - [HeroMode.Loading]: a placeholder of the same layout.
 * - [HeroMode.Status]: [HomeStatus].
 *
 * One button serves all three, called from one place, so the focused node survives the change
 * from loading to loaded and launch focus stays on it (AC N1). [onFocusChange] tells Home when the
 * hero holds focus, which plays the item's trailer (AC H8).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeHero(
    mode: HeroMode,
    items: List<HeroItem>,
    statusMessage: String?,
    focusMemory: HomeFocusMemory,
    onOpen: (AppRoute) -> Unit,
    index: Int,
    onIndexChange: (Int) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val shown = shownHeroIndex(index, items.size)
    val current = items.getOrNull(shown).takeIf { mode == HeroMode.Featured }
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(HERO_SLOT_HEIGHT)
            .bringIntoViewResponder(rememberWholeSlotResponder())
            .onFocusChanged { state ->
                isFocused = state.hasFocus
                onFocusChange(state.hasFocus)
            }
    ) {
        when (mode) {
            HeroMode.Status -> HomeStatus(statusMessage, Modifier.align(Alignment.Center).padding(bottom = STATUS_TEXT_LIFT))
            HeroMode.Featured, HeroMode.Loading, HeroMode.None -> Unit
        }
        HeroInfo(mode = mode, item = current, modifier = Modifier.fillMaxSize())
        HeroTextStack(
            modifier = Modifier.fillMaxSize(),
            details = {},
            button = {
                HeroButton(
                    mode = mode,
                    itemName = current?.name,
                    onClick = {
                        when (mode) {
                            HeroMode.Featured -> current?.let { item -> onOpen(item.route) }
                            HeroMode.Status -> onOpen(AppRoute.Settings(SettingsPage.Addons))
                            HeroMode.Loading, HeroMode.None -> Unit
                        }
                    },
                    onStep = { step ->
                        val next = shown + step
                        if (mode == HeroMode.Featured && next in items.indices) onIndexChange(next)
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
            onIndexChange((shown + 1) % items.size)
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
    val pillFocus = LocalNavPillState.current.focusRequester
    Button(
        onClick = onClick,
        modifier = modifier
            .height(PRIMARY_HEIGHT)
            .widthIn(min = PRIMARY_MIN_WIDTH)
            .width(IntrinsicSize.Max)
            .focusProperties { up = pillFocus }
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
            }
            .focusLift(PRIMARY_FOCUS_SCALE),
        colors = ButtonDefaults.colors(
            containerColor = TvColors.platter,
            contentColor = TvColors.textPrimary,
            focusedContainerColor = TvColors.focusedFill,
            focusedContentColor = TvColors.focusedContent,
            pressedContainerColor = TvColors.focusedFill,
            pressedContentColor = TvColors.focusedContent
        ),
        scale = ButtonDefaults.scale(focusedScale = 1f),
        shape = ButtonDefaults.shape(shape = TvShapes.pill),
        border = ButtonDefaults.border(border = glassBorder, focusedBorder = Border.None, pressedBorder = Border.None),
        contentPadding = PaddingValues(horizontal = PRIMARY_PADDING)
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
            Text(text = if (mode == HeroMode.Status) OPEN_ADDONS else WATCH_NOW, style = PRIMARY_LABEL_STYLE, color = labelColor, maxLines = 1)
        }
    }
}

const val HERO_BUTTON_TAG = "home-hero-button"
const val HERO_PLACEHOLDER_TAG = "home-hero-placeholder"
const val HOME_STATUS_TAG = "home-status"
const val WATCH_NOW = "Watch Now"
private const val LOADING_LABEL = "Loading"
private const val REDACTED_ALPHA = 0.35f

/**
 * The hero's slot in Home's list: with the cards at y 931 pt on the unscrolled Home
 * (`01-home.png`), the slot ends at 829 pt.
 */
internal val HERO_SLOT_HEIGHT: Dp = 414.5.dp

private const val PRIMARY_FOCUS_SCALE = 1.05f
private val PRIMARY_LABEL_STYLE = TvType.body.copy(fontWeight = FontWeight.SemiBold)

// Watch Now as the tvOS app draws it (simulator, 2026-11-24): 267 x 66 pt focused, the play glyph
// 24 pt tall (the vector's glyph fills 14 of its 24 box), label body semibold. At least that wide,
// wider when the label needs it.
private val PRIMARY_HEIGHT = 32.dp
private val PRIMARY_MIN_WIDTH = 127.dp
private val PLAY_ICON = 20.dp
private val PRIMARY_PADDING = 12.dp

private val glassBorder = Border(border = BorderStroke(1.dp, TvColors.glassEdge), shape = TvShapes.pill)

// The status text sits above the centre and the button below it.
private val STATUS_TEXT_LIFT = 40.dp
