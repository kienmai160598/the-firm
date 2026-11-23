package com.kien.movies.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import com.kien.movies.design.ArtworkImage
import com.kien.movies.design.CardFocus
import com.kien.movies.design.CardSize
import com.kien.movies.design.FactLine
import com.kien.movies.design.GlassStyle
import com.kien.movies.design.Palette
import com.kien.movies.design.Spacing
import com.kien.movies.design.TextStyles
import com.kien.movies.design.darkGlassPanel
import com.kien.movies.shell.AppRoute

/**
 * Continue Watching or Upcoming (tvOS ContinueWatchingRowView.swift). Select resumes without
 * asking (AC H4). A long press of select opens a small dialog with "Remove from Continue
 * Watching", the stand-in for the tvOS context menu; after removing, focus moves to the card's
 * neighbour (next, else previous: [RowNeighbor]) before the card leaves the row (AC H5).
 */
@Composable
fun ContinueWatchingRow(row: WatchingRowState, focusMemory: HomeFocusMemory, onOpen: (AppRoute) -> Unit, onRemove: (key: String) -> Unit, modifier: Modifier = Modifier) {
    var menuFor by remember { mutableStateOf<WatchingCard?>(null) }
    var removalAfterMenuCloses by remember { mutableStateOf<WatchingCard?>(null) }
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(WATCHING_HEADER_GAP)) {
        ShelfHeader(row.title)
        ShelfRow(rowKey = row.key, focusMemory = focusMemory) {
            items(row.cards, key = WatchingCard::key, contentType = { WATCHING_CONTENT_TYPE }) { card ->
                ContinueWatchingCard(
                    card = card,
                    onClick = { onOpen(card.route) },
                    onLongClick = { menuFor = card },
                    modifier = Modifier.rememberedFocus(focusMemory, cardFocusKey(row.key, card.key), row.key)
                )
            }
        }
    }

    menuFor?.let { card ->
        RemoveDialog(
            title = card.title,
            onRemove = {
                menuFor = null
                removalAfterMenuCloses = card
            },
            onDismiss = { menuFor = null }
        )
    }

    // As tvOS: remove once the menu has closed and focus is back on the row, after moving focus
    // to the neighbour, so focus never falls out of the row with the removed card.
    removalAfterMenuCloses?.let { card ->
        LaunchedEffect(card.key) {
            withFrameNanos {}
            val neighbour = RowNeighbor.key(beside = card.key, keys = row.cards.map(WatchingCard::key))
            val movedToNeighbour = neighbour?.let { key -> focusMemory.focus(cardFocusKey(row.key, key)) } == true
            if (!movedToNeighbour) {
                // The last card of the row: the row goes with it, so focus leaves the row first.
                focusManager.moveFocus(FocusDirection.Down) || focusManager.moveFocus(FocusDirection.Up)
            }
            removalAfterMenuCloses = null
            onRemove(card.key)
        }
    }
}

@Composable
private fun ContinueWatchingCard(card: WatchingCard, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier) {
    Column(modifier = Modifier.width(CardSize.wide.width), verticalArrangement = Arrangement.spacedBy(Spacing.textGap)) {
        Card(
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier
                .semantics {
                    contentDescription = card.title
                    stateDescription = card.progressLabel.orEmpty()
                }
                .testTag(CONTINUE_WATCHING_CARD_TAG),
            shape = CardFocus.shape(),
            colors = CardFocus.colors(),
            scale = CardFocus.scale(),
            border = CardFocus.border(),
            glow = CardFocus.glow()
        ) {
            Box(modifier = Modifier.size(CardSize.wide)) {
                ArtworkImage(
                    model = rememberSizedImage(card.imageUrl, CardSize.wide),
                    modifier = Modifier.fillMaxSize()
                )
                if (!card.isNextUp) {
                    WatchProgressBar(
                        fraction = card.progressFraction,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(CardSize.contentInset)
                            .fillMaxWidth()
                    )
                }
            }
        }
        Captions(card)
    }
}

@Composable
private fun Captions(card: WatchingCard) {
    Column(verticalArrangement = Arrangement.spacedBy(CAPTION_GAP)) {
        Text(
            text = card.title,
            style = TextStyles.cardTitle.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (card.details.isNotEmpty()) {
            CompositionLocalProvider(LocalContentColor provides CAPTION_DETAIL_COLOR) {
                ProvideTextStyle(TextStyles.small) { FactLine(facts = card.details) }
            }
        }
    }
}

/** A capsule track with the watched part in white (tvOS `WatchProgressBar`), one draw call each. */
@Composable
fun WatchProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(PROGRESS_HEIGHT)
            .drawBehind {
                val radius = CornerRadius(size.height / 2, size.height / 2)
                drawRoundRect(color = PROGRESS_TRACK, cornerRadius = radius)
                val filled = size.width * fraction.coerceIn(0f, 1f)
                if (filled > 0f) drawRoundRect(color = Color.White, size = Size(filled, size.height), cornerRadius = radius)
            }
    )
}

/** The tvOS context menu as a small dialog: Remove first, then Cancel. Back closes it. */
@Composable
private fun RemoveDialog(title: String, onRemove: () -> Unit, onDismiss: () -> Unit) {
    val removeFocus = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .darkGlassPanel()
                .padding(DIALOG_PADDING),
            verticalArrangement = Arrangement.spacedBy(Spacing.textGapLarge)
        ) {
            Text(text = title, style = TextStyles.body, color = Palette.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Button(
                onClick = onRemove,
                colors = GlassStyle.buttonColors(),
                scale = GlassStyle.buttonScale(),
                shape = GlassStyle.buttonShape(),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(removeFocus)
                    .testTag(REMOVE_BUTTON_TAG)
            ) {
                Text(text = REMOVE_TITLE)
            }
            Button(
                onClick = onDismiss,
                colors = GlassStyle.buttonColors(),
                scale = GlassStyle.buttonScale(),
                shape = GlassStyle.buttonShape(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = CANCEL_TITLE)
            }
        }
    }
    LaunchedEffect(Unit) {
        withFrameNanos {}
        removeFocus.requestFocus()
    }
}

const val CONTINUE_WATCHING_CARD_TAG = "continue-watching-card"
const val REMOVE_BUTTON_TAG = "continue-watching-remove"
const val REMOVE_TITLE = "Remove from Continue Watching"
private const val CANCEL_TITLE = "Cancel"
private const val WATCHING_CONTENT_TYPE = "watching"

// tvOS: header gap 6 pt, caption gap 14 pt, caption detail white 55%, progress bar 8 pt on
// black 55%.
private val WATCHING_HEADER_GAP = 3.dp
private val CAPTION_GAP = 7.dp
private val CAPTION_DETAIL_COLOR = Color.White.copy(alpha = 0.55f)
private val PROGRESS_HEIGHT = 4.dp
private val PROGRESS_TRACK = Color.Black.copy(alpha = 0.55f)
private val DIALOG_PADDING = 24.dp
