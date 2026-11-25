package com.kien.movies.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import com.kien.movies.design.ArtworkImage
import com.kien.movies.design.CardFocus
import com.kien.movies.design.CardSize
import com.kien.movies.design.FactLine
import com.kien.movies.design.Focus
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvType
import com.kien.movies.design.focusLift
import com.kien.movies.design.liftsShelfTitle
import com.kien.movies.design.rememberShelfTitleLift
import com.kien.movies.details.WatchProgressBar
import com.kien.movies.shell.AppRoute

/**
 * Continue Watching or Upcoming (tvOS ContinueWatchingRowView.swift). Select resumes without
 * asking (AC H4). A long press of select opens the card's menu ([ContinueWatchingMenu]): "Go to
 * Show" or "Go to Movie" opens the title's Details (AC W5), and "Remove from Continue Watching"
 * takes the card off; after removing, focus moves to the card's neighbour (next, else previous:
 * [RowNeighbor]) before the card leaves the row (AC H5).
 */
@Composable
fun ContinueWatchingRow(
    row: WatchingRowState,
    focusMemory: HomeFocusMemory,
    onOpen: (AppRoute) -> Unit,
    onRemove: (key: String) -> Unit,
    modifier: Modifier = Modifier,
    titleTop: Dp = ShelfMetrics.titleTop
) {
    var menuFor by remember { mutableStateOf<WatchingCard?>(null) }
    var removalAfterMenuCloses by remember { mutableStateOf<WatchingCard?>(null) }
    var detailsAfterMenuCloses by remember { mutableStateOf<WatchingCard?>(null) }
    val focusManager = LocalFocusManager.current

    val lift = rememberShelfTitleLift(height = WatchingArtSize.height)
    Column(modifier = modifier) {
        ShelfHeader(row.title, titleTop = titleTop, lift = lift)
        ShelfRow(rowKey = row.key, focusMemory = focusMemory) {
            items(row.cards, key = WatchingCard::key, contentType = { WATCHING_CONTENT_TYPE }) { card ->
                ContinueWatchingCard(
                    card = card,
                    onClick = { onOpen(card.route) },
                    onLongClick = { menuFor = card },
                    modifier = Modifier.rememberedFocus(focusMemory, cardFocusKey(row.key, card.key), row.key).liftsShelfTitle(lift)
                ) {
                    if (menuFor?.key == card.key) {
                        ContinueWatchingMenu(
                            card = card,
                            onGoToDetails = {
                                menuFor = null
                                detailsAfterMenuCloses = card
                            },
                            onRemove = {
                                menuFor = null
                                removalAfterMenuCloses = card
                            },
                            onDismiss = { menuFor = null }
                        )
                    }
                }
            }
        }
    }

    // As tvOS: Details opens once the menu has closed and focus is back on the card, so Back
    // returns to it (AC N4). The card stays in the row. The menu's popup window held focus, so
    // the card is focused again explicitly before Home is left.
    detailsAfterMenuCloses?.let { card ->
        LaunchedEffect(card.key) {
            withFrameNanos {}
            focusMemory.focus(cardFocusKey(row.key, card.key))
            withFrameNanos {}
            detailsAfterMenuCloses = null
            onOpen(card.detailsRoute)
        }
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

private const val WATCHING_CONTENT_TYPE = "watching"
