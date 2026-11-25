package com.kien.movies.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.design.Focus
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvControls
import com.kien.movies.design.TvGlass
import com.kien.movies.design.TvType
import com.kien.movies.design.containerRadius
import com.kien.movies.design.ignoringTheHeldSelect
import com.kien.movies.design.liquidGlassEdge
import kotlin.math.roundToInt

/**
 * The tvOS context menu of a Continue Watching card, in its order: "Go to Show" or "Go to Movie"
 * first and focused, then "Remove from Continue Watching" in red. Each row is a glyph and a label
 * with no platter until focused, as the tvOS menu draws them. Like tvOS, it opens beside the card
 * ([BesideTheCard]) over an undimmed Home. Back closes it. The held select that opened it cannot
 * press an item. Call it inside a box the size of the card's art, which it is anchored to.
 */
@Composable
internal fun ContinueWatchingMenu(card: WatchingCard, onGoToDetails: () -> Unit, onRemove: () -> Unit, onDismiss: () -> Unit) {
    val firstFocus = remember { FocusRequester() }
    val density = LocalDensity.current
    val position = remember(density) { BesideTheCard(gapPx = with(density) { MENU_GAP.roundToPx() }, scale = Focus.cardScale) }
    Popup(popupPositionProvider = position, onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        Column(
            modifier = Modifier
                .ignoringTheHeldSelect()
                .width(MENU_WIDTH)
                .background(TvColors.dialogPanel, MENU_SHAPE)
                .liquidGlassEdge(MENU_SHAPE)
                .padding(MENU_PADDING),
            verticalArrangement = Arrangement.spacedBy(ROW_GAP)
        ) {
            MenuRow(
                title = card.goToDetailsTitle,
                icon = R.drawable.ic_home_menu_details,
                onClick = onGoToDetails,
                modifier = Modifier.focusRequester(firstFocus).testTag(GO_TO_DETAILS_BUTTON_TAG)
            )
            MenuRow(
                title = REMOVE_TITLE,
                icon = R.drawable.ic_home_menu_remove,
                onClick = onRemove,
                tint = DESTRUCTIVE,
                modifier = Modifier.testTag(REMOVE_BUTTON_TAG)
            )
        }
    }
    LaunchedEffect(Unit) {
        withFrameNanos {}
        firstFocus.requestFocus()
    }
}

/**
 * Places the menu [gapPx] to the right of the focused card, its top on the card's top, as tvOS
 * does; on the left when the right has no room. The anchor is the card's layout size, so the
 * focus [scale] is added back to reach the edge the viewer sees.
 */
internal class BesideTheCard(private val gapPx: Int, private val scale: Float) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val overhangX = (anchorBounds.width * (scale - 1f) / 2f).roundToInt()
        val overhangY = (anchorBounds.height * (scale - 1f) / 2f).roundToInt()
        val right = anchorBounds.right + overhangX + gapPx
        val x = if (right + popupContentSize.width <= windowSize.width) right else anchorBounds.left - overhangX - gapPx - popupContentSize.width
        val y = (anchorBounds.top - overhangY).coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0))
        return IntOffset(x.coerceAtLeast(0), y)
    }
}

@Composable
private fun MenuRow(title: String, icon: Int, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = tint.takeOrElse { TvColors.textPrimary },
            focusedContainerColor = TvColors.focusedFill,
            focusedContentColor = tint.takeOrElse { TvColors.focusedContent },
            pressedContainerColor = TvColors.focusedFill,
            pressedContentColor = tint.takeOrElse { TvColors.focusedContent }
        ),
        scale = TvGlass.buttonScale(),
        shape = TvGlass.pillShape(),
        contentPadding = TvGlass.pillPadding,
        modifier = modifier.fillMaxWidth().height(TvControls.pillHeight)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(ICON_GAP), verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(ICON_SIZE))
            Text(text = title, style = TvType.caption1)
        }
    }
}

const val GO_TO_DETAILS_BUTTON_TAG = "continue-watching-go-to-details"
const val REMOVE_BUTTON_TAG = "continue-watching-remove"
const val REMOVE_TITLE = "Remove from Continue Watching"

/** tvOS systemRed in dark mode, the destructive menu item. */
private val DESTRUCTIVE = Color(0xFFFF453A)

// tvOS menu, pt / 2 from the simulator capture: 12 pt from the focused card, glyphs about 28 pt,
// 18 pt from the label. The menu is wider than tvOS's 446 pt so Remove fits on one line in the
// 32 dp capsule.
private val MENU_GAP = 6.dp
private val MENU_WIDTH = 280.dp
private val MENU_PADDING = 8.dp
private val ROW_GAP = 4.dp
private val ICON_SIZE = 14.dp
private val ICON_GAP = 9.dp

// Concentric with the capsule rows: 16 dp pill radius + 8 dp padding.
private val MENU_RADIUS = containerRadius(inner = TvControls.pillRadius, padding = MENU_PADDING)
private val MENU_SHAPE = RoundedCornerShape(MENU_RADIUS)
