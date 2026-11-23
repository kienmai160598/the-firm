package com.kien.movies.player

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.streams.TvColors
import com.kien.movies.streams.TvShapes
import com.kien.movies.streams.TvType
import com.kien.movies.streams.glassEdge

const val PLAYER_TRACK_MENU_TAG = "player-track-menu"
const val PLAYER_INFO_MENU_TAG = "player-info-menu"

/** Test tag of a track menu row. */
fun playerOptionTag(optionId: String) = "player-option-$optionId"

// tvOS MpvPanels.swift literals, pt / 2.
private val TrackMenuWidth = 280.dp // 560 pt
private val InfoMenuWidth = 320.dp // 640 pt
private val MaxListHeight = 260.dp // 520 pt
private val PanelPadding = 7.dp // 14 pt
private val RowInset = 12.dp // 24 pt
private val RowLeading = 7.dp // 14 pt
private val RowVertical = 8.dp // 16 pt
private val InfoRowVertical = 5.dp // 10 pt
private val CheckSize = 15.dp // 30 pt
private val CheckGap = 8.dp // 16 pt
private val RowGap = 1.dp // 2 pt
private val HeaderTop = 5.dp // 10 pt
private val HeaderBottom = 3.dp // 6 pt
private val SeparatorVertical = 5.dp // 10 pt
private val SeparatorHeight = 1.dp
private val RowFont = TvType.caption1
private val DetailFont = TvType.caption2
private val HeaderFont = TvType.caption1.copy(fontWeight = TvType.body.fontWeight)
private const val PRESSED_SCALE = 0.97f
private val DETAIL_ALPHA = TvColors.textSecondary.alpha

/** Opaque, so a menu over the video is one layer even where it meets the controls' scrim band. */
private val PanelFill = TvColors.sidebarPanel.copy(alpha = 1f)

/**
 * The audio or subtitle menu (tvOS MpvTrackMenu). Opens with the selected row focused; focus stays
 * in the menu until a row is chosen or Back closes it (tvOS disables the other controls while a
 * track menu is open).
 */
@Composable
fun TrackMenu(kind: TrackKind, options: List<TrackOption>, onSelect: (TrackOption) -> Unit, modifier: Modifier = Modifier) {
    val selectedIndex = options.indexOfFirst(TrackOption::isSelected).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val selectedFocus = remember { FocusRequester() }
    MenuPanel(width = TrackMenuWidth, modifier = modifier.testTag(PLAYER_TRACK_MENU_TAG)) {
        MenuHeader(kind.menuTitle)
        if (options.isEmpty()) {
            Text(
                text = "No ${kind.name.lowercase()} tracks",
                style = RowFont,
                modifier = Modifier.padding(horizontal = RowInset, vertical = RowVertical)
            )
            return@MenuPanel
        }
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(RowGap),
            modifier = Modifier
                .heightIn(max = MaxListHeight)
                .focusProperties { onExit = { cancelFocusChange() } }
                .focusGroup()
        ) {
            itemsIndexed(options, key = { _, option -> option.id }, contentType = { _, _ -> "option" }) { index, option ->
                TrackRow(
                    option = option,
                    onClick = { onSelect(option) },
                    modifier = if (index == selectedIndex) Modifier.focusRequester(selectedFocus) else Modifier
                )
            }
        }
    }
    LaunchedEffect(kind) {
        withFrameNanos {}
        selectedFocus.requestFocus()
    }
}

/** Source and playback facts (tvOS MpvInfoMenu); nothing in it takes focus. */
@Composable
fun InfoMenu(source: String, rows: List<InfoRow>, modifier: Modifier = Modifier) {
    MenuPanel(width = InfoMenuWidth, modifier = modifier.testTag(PLAYER_INFO_MENU_TAG)) {
        MenuHeader("Source")
        Text(
            text = source,
            style = RowFont,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = RowInset, vertical = InfoRowVertical)
        )
        Box(
            modifier = Modifier
                .padding(horizontal = RowInset, vertical = SeparatorVertical)
                .fillMaxWidth()
                .height(SeparatorHeight)
                .background(TvColors.glassEdge)
        )
        MenuHeader("Playback")
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(RowInset),
                modifier = Modifier.fillMaxWidth().padding(horizontal = RowInset, vertical = InfoRowVertical)
            ) {
                Text(text = row.label, style = RowFont, color = TvColors.textSecondary)
                Spacer(modifier = Modifier.weight(1f))
                Text(text = row.value, style = RowFont, textAlign = TextAlign.End)
            }
        }
    }
}

@Composable
private fun TrackRow(option: TrackOption, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(TvShapes.pill),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = PRESSED_SCALE),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = TvColors.textPrimary,
            focusedContainerColor = TvColors.focusedFill,
            focusedContentColor = TvColors.focusedContent,
            pressedContainerColor = TvColors.focusedFill,
            pressedContentColor = TvColors.focusedContent
        ),
        modifier = modifier.fillMaxWidth().testTag(playerOptionTag(option.id))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = RowLeading, end = RowInset, top = RowVertical, bottom = RowVertical)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_player_check),
                contentDescription = if (option.isSelected) "Selected" else null,
                modifier = Modifier.size(CheckSize).alpha(if (option.isSelected) 1f else 0f)
            )
            Spacer(modifier = Modifier.width(CheckGap))
            Column {
                Text(text = option.label, style = RowFont, maxLines = 2, overflow = TextOverflow.Ellipsis)
                option.detail?.let { detail ->
                    Text(text = detail, style = DetailFont, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.alpha(DETAIL_ALPHA))
                }
            }
        }
    }
}

/** A platter over the video: opaque near black panel, panel corner and the glass edge. */
@Composable
private fun MenuPanel(width: Dp, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .width(width)
            .background(PanelFill, TvShapes.panel)
            .glassEdge(TvShapes.panel)
            .padding(PanelPadding),
        content = content
    )
}

@Composable
private fun MenuHeader(title: String) {
    Text(
        text = title,
        style = HeaderFont,
        color = TvColors.textSecondary,
        modifier = Modifier.padding(start = RowInset, end = RowInset, top = HeaderTop, bottom = HeaderBottom)
    )
}
