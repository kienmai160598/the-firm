package com.kien.movies.shell

import android.text.format.DateFormat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.DrawerState
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.Icon
import androidx.tv.material3.ModalNavigationDrawer
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvShapes
import com.kien.movies.design.TvType
import java.util.Date
import kotlinx.coroutines.delay

/**
 * The navigation around the tab roots (docs/tv-look-spec.md, section 3, "Navigation"; tvOS
 * `TabView` with `.sidebarAdaptable`): tv-material's ModalNavigationDrawer whose content follows
 * the drawer value.
 *
 * - Closed: a "‹ Home" pill floating at the top left of the content. It is the selected tab's
 *   item, so Up from the top control or Left from the first item of a row reaches it.
 * - Open: a floating panel (avatar, name, clock, one row per tab). The drawer opens when focus
 *   enters it and closes when focus leaves; entering always lands on the selected tab.
 *
 * The selected tab's item is the same node in both states (keyed, same modifier chain), so the
 * focus that opens the drawer stays on it while the pill turns into a panel row. No scrim: the
 * panel is one opaque layer with the glass edge (box GPU rules).
 */
@Composable
fun RootTabs(
    selected: RootTab,
    drawerState: DrawerState,
    selectedTabFocus: FocusRequester,
    contentFocus: FocusRequester,
    onSelect: (RootTab) -> Unit,
    content: @Composable () -> Unit
) {
    val pillState = remember(selectedTabFocus) { NavPillState(selectedTabFocus) }
    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimBrush = SolidColor(Color.Transparent),
        drawerContent = { value ->
            SidebarContent(
                isOpen = value == DrawerValue.Open,
                hidesPill = pillState.isContentScrolled,
                selected = selected,
                selectedTabFocus = selectedTabFocus,
                contentFocus = contentFocus,
                onSelect = onSelect
            )
        },
        content = { CompositionLocalProvider(LocalNavPillState provides pillState, content = content) }
    )
}

/**
 * Whether the tab root's content is scrolled away from its top. Apple's collapsed pill shows only
 * at the top of the page and is gone once the rows scroll under it (`03-sidebar-down.png`,
 * `10-row3.png`); Up back to the top brings it back (`05-up-hero.png`). A tab root that scrolls
 * sets [isContentScrolled]; the pill fades but stays focusable, so Left from the first card still
 * opens the panel.
 *
 * [focusRequester] is the pill itself. The closed drawer's focus group spans the full screen
 * height, so the geometric search never finds it "above" a control: a tab root's top control sets
 * `focusProperties { up = focusRequester }` (spec section 3: Up from the top control reaches it).
 */
@Stable
class NavPillState(val focusRequester: FocusRequester) {
    var isContentScrolled: Boolean by mutableStateOf(false)
}

/** The [NavPillState] of the shell around the current tab root. */
val LocalNavPillState = staticCompositionLocalOf { NavPillState(FocusRequester.Default) }

@Composable
private fun SidebarContent(
    isOpen: Boolean,
    hidesPill: Boolean,
    selected: RootTab,
    selectedTabFocus: FocusRequester,
    contentFocus: FocusRequester,
    onSelect: (RootTab) -> Unit
) {
    val outer = if (isOpen) PANEL_ORIGIN else PILL_ORIGIN
    val alpha = remember { Animatable(1f) }
    LaunchedEffect(isOpen, hidesPill) {
        when {
            isOpen -> alpha.snapTo(1f)
            hidesPill -> alpha.animateTo(0f, tween(PILL_FADE_MS))
            else -> alpha.animateTo(1f, tween(PILL_FADE_MS))
        }
    }
    Column(
        modifier = Modifier
            .graphicsLayer { this.alpha = alpha.value }
            .padding(start = outer, top = outer)
            .sizeIn(
                minWidth = if (isOpen) PANEL_WIDTH else 0.dp,
                maxWidth = if (isOpen) PANEL_WIDTH else Dp.Unspecified,
                minHeight = if (isOpen) PANEL_HEIGHT else 0.dp,
                maxHeight = if (isOpen) PANEL_HEIGHT else Dp.Unspecified
            )
            .drawBehind { if (isOpen) drawPanel() }
            .padding(horizontal = if (isOpen) ROW_INSET else 0.dp)
            .selectableGroup()
            .focusProperties { onEnter = { selectedTabFocus.requestFocus() } }
            .focusGroup()
    ) {
        SidebarHeader(isOpen)
        RootTab.entries.forEach { tab ->
            key(tab) {
                val isSelected = tab == selected
                if (isOpen || isSelected) {
                    TabItem(
                        tab = tab,
                        isOpen = isOpen,
                        isSelected = isSelected,
                        onClick = { onSelect(tab) },
                        modifier = Modifier
                            .then(if (isSelected) Modifier.focusRequester(selectedTabFocus) else Modifier)
                            // The content's root group spans the whole screen, also under the
                            // panel, so the geometric search never finds it "right of" a row:
                            // Right goes to the content, whose restorer lands on the element that
                            // had focus (`04-back-to-content.png`).
                            .focusProperties { right = contentFocus }
                    )
                }
            }
        }
    }
}

/** Open: avatar, name and clock above the rows. Closed: the "‹" left of the pill, taking no space. */
@Composable
private fun SidebarHeader(isOpen: Boolean) {
    if (isOpen) {
        Row(
            modifier = Modifier
                .padding(top = AVATAR_TOP)
                .width(ROW_WIDTH)
                .padding(start = AVATAR_START - ROW_INSET, end = CLOCK_END - ROW_INSET),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(AVATAR_SIZE).background(TvColors.navPillIcon, CircleShape), contentAlignment = Alignment.Center) {
                Text(text = APP_INITIAL, style = TvType.caption1, color = TvColors.textPrimary)
            }
            Spacer(Modifier.width(AVATAR_NAME_GAP))
            Text(text = APP_NAME, style = TvType.caption1, color = TvColors.textSecondary, modifier = Modifier.weight(1f), maxLines = 1)
            Clock()
        }
        Spacer(Modifier.size(FIRST_ROW_GAP))
    } else {
        Text(
            text = CHEVRON,
            style = TvType.bodyEmphasized,
            color = TvColors.textSecondary,
            modifier = Modifier.layout { measurable, constraints ->
                // Drawn left of the pill and centred on it; takes no room in the column.
                val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                layout(0, 0) {
                    placeable.place(-placeable.width - CHEVRON_GAP.roundToPx(), (PILL_HEIGHT.roundToPx() - placeable.height) / 2)
                }
            }
        )
    }
}

/** The time on the open panel, updated every minute while the panel shows. */
@Composable
private fun Clock() {
    val context = LocalContext.current
    val format = remember(context) { DateFormat.getTimeFormat(context) }
    var now by remember { mutableStateOf(format.format(Date())) }
    LaunchedEffect(format) {
        while (true) {
            now = format.format(Date())
            delay(CLOCK_TICK_MS - System.currentTimeMillis() % CLOCK_TICK_MS)
        }
    }
    Text(text = now, style = TvType.caption1, color = TvColors.textTertiary, maxLines = 1)
}

/**
 * One tab: the pill when the drawer is closed (only the selected tab), a panel row when open.
 * The Surface and its modifiers keep one shape in both states, so its focus node survives.
 */
@Composable
private fun TabItem(tab: RootTab, isOpen: Boolean, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val restFill = when {
        !isOpen -> TvColors.navPill
        isSelected -> TvColors.sidebarCurrent
        else -> Color.Transparent
    }
    Surface(
        onClick = onClick,
        modifier = modifier
            .padding(bottom = if (isOpen) ROW_GAP else 0.dp)
            .sizeIn(
                minWidth = if (isOpen) ROW_WIDTH else 0.dp,
                maxWidth = if (isOpen) ROW_WIDTH else Dp.Unspecified,
                minHeight = if (isOpen) ROW_HEIGHT else PILL_HEIGHT,
                maxHeight = if (isOpen) ROW_HEIGHT else PILL_HEIGHT
            )
            .testTag(tabTag(tab))
            .semantics {
                role = Role.Tab
                this.selected = isSelected
            },
        shape = ClickableSurfaceDefaults.shape(shape = TvShapes.pill),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = restFill,
            contentColor = if (isOpen) ROW_LABEL else TvColors.textPrimary,
            focusedContainerColor = TvColors.focusedFill,
            focusedContentColor = TvColors.focusedContent,
            pressedContainerColor = TvColors.focusedFill,
            pressedContentColor = TvColors.focusedContent
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(border = if (isOpen) Border.None else glassBorder, focusedBorder = Border.None),
        glow = ClickableSurfaceDefaults.glow()
    ) {
        Row(modifier = Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
            if (isOpen) RowContent(tab) else PillContent(tab)
        }
    }
}

@Composable
private fun PillContent(tab: RootTab) {
    Spacer(Modifier.width(PILL_ICON_START))
    Box(modifier = Modifier.size(PILL_ICON_CIRCLE).background(TvColors.navPillIcon, CircleShape), contentAlignment = Alignment.Center) {
        Icon(painter = painterResource(tab.icon), contentDescription = null, modifier = Modifier.size(PILL_ICON))
    }
    Spacer(Modifier.width(PILL_LABEL_GAP))
    Text(text = tab.title, style = TvType.bodyEmphasized, maxLines = 1)
    Spacer(Modifier.width(PILL_END))
}

@Composable
private fun RowContent(tab: RootTab) {
    Spacer(Modifier.width(ROW_ICON_START))
    Icon(painter = painterResource(tab.icon), contentDescription = null, modifier = Modifier.size(ROW_ICON))
    Spacer(Modifier.width(ROW_LABEL_GAP))
    Text(text = tab.title, style = TvType.caption1, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

private fun DrawScope.drawPanel() {
    val radius = CornerRadius(PANEL_CORNER.toPx())
    drawRoundRect(color = TvColors.sidebarPanel, cornerRadius = radius)
    drawGlassEdge(radius)
}

private fun DrawScope.drawGlassEdge(radius: CornerRadius) {
    val stroke = EDGE_WIDTH.toPx()
    val inset = stroke / 2
    drawRoundRect(
        color = TvColors.glassEdge,
        topLeft = Offset(inset, inset),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(radius.x - inset, radius.y - inset),
        style = Stroke(width = stroke)
    )
}

/** Test tag of a sidebar item, e.g. `tab-addons`. */
fun tabTag(tab: RootTab): String = "tab-${tab.name.lowercase()}"

private const val CHEVRON = "‹"
private const val APP_NAME = "Movies"
private const val APP_INITIAL = "M"
private const val CLOCK_TICK_MS = 60_000L

// Unmeasured: Apple's pill fade when the page scrolls.
private const val PILL_FADE_MS = 200

// Measured on 01-home.png and 05-up-hero.png (pt / 2): capsule x 50 to 222, y 50 to 110; "‹" at
// x 35 to 44; icon circle 50 pt at x 54 with a 25 pt glyph; label at x 118; 27 pt after the label.
private val PILL_ORIGIN = 25.dp
private val PILL_HEIGHT = 30.dp
private val CHEVRON_GAP = 3.dp
private val PILL_ICON_START = 2.dp
private val PILL_ICON_CIRCLE = 25.dp
private val PILL_ICON = 15.dp
private val PILL_LABEL_GAP = 7.dp
private val PILL_END = 13.dp

// Measured on 30-sidebar.png and 60-lib-hover.png: panel x 34 to 374, y 34 to 1050, corner 32;
// avatar 50 pt at x 54, y 56; clock ends at x 343; rows 72 pt on a 78 pt pitch from y 164; row
// capsule x 44 to 364; glyph ink 20 pt at x 71 (13 dp box: the vector has a margin); label at x 120;
// unfocused label white about 75%.
private val PANEL_ORIGIN = 17.dp
private val PANEL_WIDTH = 170.dp
private val PANEL_HEIGHT = 508.dp
private val PANEL_CORNER = 16.dp
private val ROW_INSET = 5.dp
private val ROW_WIDTH = PANEL_WIDTH - ROW_INSET * 2
private val ROW_HEIGHT = 36.dp
private val ROW_GAP = 3.dp
private val ROW_ICON_START = 12.dp
private val ROW_ICON = 13.dp
private val ROW_LABEL_GAP = 13.dp
private val ROW_LABEL = Color.White.copy(alpha = 0.75f)
private val AVATAR_START = 10.dp
private val AVATAR_TOP = 11.dp
private val AVATAR_SIZE = 25.dp
private val AVATAR_NAME_GAP = 7.dp
private val CLOCK_END = 15.5.dp

// Avatar row ends at 52 dp from the panel top (11 + 25 + centring); the first row starts at 65 dp.
private val FIRST_ROW_GAP = 29.dp
private val EDGE_WIDTH = 1.dp
private val glassBorder = Border(border = BorderStroke(EDGE_WIDTH, TvColors.glassEdge), shape = TvShapes.pill)
