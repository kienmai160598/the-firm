package com.kien.movies.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.design.ArtworkImage
import com.kien.movies.design.BakedScrim
import com.kien.movies.design.CardFocus
import com.kien.movies.design.Focus
import com.kien.movies.design.ScrimLayer
import com.kien.movies.design.TitleLogo
import com.kien.movies.design.TvType
import com.kien.movies.design.focusLift
import com.kien.movies.design.rememberBakedImage
import com.kien.movies.design.rememberLogoRequest
import com.kien.movies.details.WatchProgressBar

/**
 * A Continue Watching or Upcoming card, as tvOS ContinueWatchingCard and the Apple TV app (AC H11):
 * the artwork alone, with the title logo and one line (play glyph, progress, "S1, E1 · 17m") on a
 * shade at its foot. The shade is baked into the decoded art ([BakedScrim]), so the box draws one
 * opaque image per card. The line is dimmed until the card has focus. [menu] is anchored to it.
 */
@Composable
internal fun ContinueWatchingCard(
    card: WatchingCard,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier,
    menu: @Composable () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Box {
        Card(
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier
                .onFocusChanged { state -> isFocused = state.isFocused }
                .semantics {
                    contentDescription = card.title
                    stateDescription = card.progressLabel.orEmpty()
                }
                .testTag(CONTINUE_WATCHING_CARD_TAG)
                .focusLift(Focus.cardScale),
            shape = CardFocus.shape(),
            colors = CardFocus.colors(),
            scale = CardFocus.noScale(),
            border = CardFocus.border(),
            glow = CardFocus.glow()
        ) {
            Box(modifier = Modifier.size(WatchingArtSize)) {
                ArtworkImage(model = rememberBakedImage(card.imageUrl, WatchingArtSize, FOOT_SHADE), modifier = Modifier.fillMaxSize())
                CardFoot(card, isFocused, Modifier.align(Alignment.BottomStart))
            }
        }
        menu()
    }
}

@Composable
private fun CardFoot(card: WatchingCard, isFocused: Boolean, modifier: Modifier) {
    Column(modifier = modifier.padding(FOOT_PADDING), verticalArrangement = Arrangement.spacedBy(FOOT_GAP)) {
        TitleLogo(
            name = card.title,
            logo = rememberLogoRequest(card.target.logoUrl, LOGO_BOX),
            box = LOGO_BOX,
            textStyle = FALLBACK_TITLE,
            textMaxLines = 1,
            textMaxWidth = WatchingArtSize.width - FOOT_PADDING * 2
        )
        CaptionLine(card, color = if (isFocused) Color.White else DIMMED)
    }
}

@Composable
private fun CaptionLine(card: WatchingCard, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LINE_GAP)) {
        Icon(painter = painterResource(R.drawable.ic_home_play), contentDescription = null, tint = color, modifier = Modifier.size(PLAY_ICON))
        if (!card.isNextUp && card.progressFraction > 0f) {
            WatchProgressBar(fraction = card.progressFraction, track = PROGRESS_TRACK, height = PROGRESS_HEIGHT, modifier = Modifier.width(PROGRESS_WIDTH))
        }
        Text(text = card.caption, style = CAPTION, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

const val CONTINUE_WATCHING_CARD_TAG = "continue-watching-card"

/** tvOS ContinueWatchingCard: 320 pt wide (the five-column grid preset), 16:9. */
internal val WatchingArtSize = DpSize(160.dp, 90.dp)

// tvOS pt / 2: logo up to 190 x 56, 16 inset, 8 between logo and line, 10 inside the line, progress
// 38 x 5, title 25 pt bold, caption 23 pt medium, white 72% until focused. The shade covers the
// bottom 62%: clear, then black 55% at its middle, 80% at the foot.
private val LOGO_BOX = DpSize(95.dp, 28.dp)
private val FOOT_PADDING = 8.dp
private val FOOT_GAP = 4.dp
private val LINE_GAP = 5.dp
private val PLAY_ICON = 11.dp
private val PROGRESS_WIDTH = 19.dp
private val PROGRESS_HEIGHT = 2.5.dp
private val PROGRESS_TRACK = Color.White.copy(alpha = 0.3f)
private val DIMMED = Color.White.copy(alpha = 0.72f)
private val FALLBACK_TITLE = TvType.body.copy(fontSize = 12.5.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold)
private val CAPTION = TvType.body.copy(fontSize = 11.5.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
private val FOOT_SHADE = BakedScrim(
    underlay = Color.Black,
    layers = listOf(ScrimLayer.vertical(0.38f to Color.Transparent, 0.72f to Color.Black.copy(alpha = 0.55f), 1f to Color.Black.copy(alpha = 0.8f)))
)
