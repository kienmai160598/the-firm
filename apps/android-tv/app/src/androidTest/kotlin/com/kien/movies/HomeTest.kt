package com.kien.movies

import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kien.movies.home.CONTINUE_WATCHING_CARD_TAG
import com.kien.movies.home.HERO_BUTTON_TAG
import com.kien.movies.home.HERO_PLACEHOLDER_TAG
import com.kien.movies.home.HOME_STATUS_TAG
import com.kien.movies.home.POSTER_CARD_TAG
import com.kien.movies.shell.RootTab
import com.kien.movies.shell.tabTag
import com.kien.movies.support.TvRobot
import kotlin.math.abs
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Home's ACs (docs/android-tv-rebuild.md, section 6), from apps/apple-tv/UITests/NavigationTests.
 * Catalogs come from the default addon over the network: a test that needs the featured hero is
 * skipped when Home ends on its status instead.
 */
@RunWith(AndroidJUnit4::class)
class HomeTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val tv = TvRobot(compose)

    @After
    fun closeApp() = tv.close()

    /** N1: launch puts focus on the hero button of Home, not in the sidebar. */
    @Test
    fun n1_launchFocusesTheHeroButton() {
        launchUntilHomeLoaded()

        tv.assertFocusedTag(HERO_BUTTON_TAG)
    }

    /** H3: from the first frame, loading and then loaded or status, one Home element has focus. */
    @Test
    fun h3_everyHomeStateHoldsFocus() {
        tv.launch()
        tv.awaitFocus(heroSlot, TvRobot.DEFAULT_TIMEOUT_MS)

        tv.await(hasTestTag(HERO_BUTTON_TAG) or hasTestTag(HOME_STATUS_TAG) or hasTestTag(POSTER_CARD_TAG))

        tv.awaitFocus(heroSlot or hasTestTag(POSTER_CARD_TAG) or hasTestTag(CONTINUE_WATCHING_CARD_TAG))
    }

    /** H1: right and left on the hero button change the featured item and come back. */
    @Test
    fun h1_heroArrowsChangeTheItem() {
        launchUntilHomeLoaded()
        tv.assertFocusedTag(HERO_BUTTON_TAG)
        val first = focused().stateDescription
        tv.press(KeyEvent.KEYCODE_DPAD_RIGHT)
        val second = focused().stateDescription
        assumeTrue("The hero has one item", second != first)

        tv.press(KeyEvent.KEYCODE_DPAD_LEFT)

        tv.assertFocusedTag(HERO_BUTTON_TAG)
        assertEquals(first, focused().stateDescription)
    }

    /** N3: left from the first hero item opens the sidebar; right comes back; down reaches the first row. */
    @Test
    fun n3_leftFromTheHeroOpensTheSidebarAndDownReachesTheFirstRow() {
        launchUntilHomeLoaded()
        tv.assertFocusedTag(HERO_BUTTON_TAG)
        val item = focused().stateDescription

        tv.press(KeyEvent.KEYCODE_DPAD_LEFT)
        tv.assertFocusedTag(tabTag(RootTab.Home))
        tv.press(KeyEvent.KEYCODE_DPAD_RIGHT)
        tv.assertFocusedTag(HERO_BUTTON_TAG)
        assertEquals("Right from the sidebar changed the item", item, focused().stateDescription)

        tv.down()

        tv.awaitFocus(hasTestTag(POSTER_CARD_TAG) or hasTestTag(CONTINUE_WATCHING_CARD_TAG))
    }

    /** H2: a catalog row scrolls sideways with the next card partly visible, and keeps its card. */
    @Test
    fun h2_rowFocusRestoresAndTheNextCardShowsPartly() {
        launchUntilHomeLoaded()
        repeat(MAX_ROWS) { if (!focusedHas(POSTER_CARD_TAG)) tv.down() }
        tv.awaitFocus(hasTestTag(POSTER_CARD_TAG))
        repeat(3) { tv.press(KeyEvent.KEYCODE_DPAD_RIGHT) }
        val card = focused()
        val screenWidth = compose.onRoot().fetchSemanticsNode().size.width

        val rowCards = compose.onAllNodes(hasTestTag(POSTER_CARD_TAG)).fetchSemanticsNodes()
            .filter { node -> node.boundsInRoot.top == card.boundsInRoot.top }
        tv.down()
        tv.press(KeyEvent.KEYCODE_DPAD_UP)

        assertTrue("No card cut by the right edge", rowCards.any { node -> node.boundsInRoot.left < screenWidth && node.boundsInRoot.right > screenWidth })
        tv.awaitFocus(hasTestTag(POSTER_CARD_TAG))
        assertNotEquals(null, card.contentDescription)
        assertEquals(card.contentDescription, focused().contentDescription)
    }

    /**
     * H7: down from the hero brings the first row in only as far as tvOS does, its cards 90 dp
     * above the screen's bottom, and the hero moves with the rows (no parallax); up brings it back.
     */
    @Test
    fun h7_theFirstRowComesInAsFarAsOnTvosAndTheHeroMovesWithIt() {
        launchUntilHomeLoaded()
        val heroBefore = centreY(hasTestTag(HERO_BUTTON_TAG))
        // Cards the list keeps composed but has not placed report no position: count placed ones.
        val rowBefore = compose.onAllNodes(anyCard).fetchSemanticsNodes().map { node -> node.centreY }.filter { y -> y > heroBefore }.min()

        tv.down()
        tv.awaitFocus(anyCard)
        val focusedCard = focused()
        val neighbour = compose.onAllNodes(anyCard and !isFocused()).fetchSemanticsNodes()
            .first { node -> abs(node.centreY - focusedCard.centreY) < 2f }
        val screenHeight = compose.onRoot().fetchSemanticsNode().size.height
        val inset = with(compose.density) { ROW_INSET_DP.dp.toPx() }

        assertEquals(screenHeight - inset, neighbour.positionInRoot.y + neighbour.size.height, 2f)
        assertEquals(rowBefore - neighbour.centreY, heroBefore - centreY(hasTestTag(HERO_BUTTON_TAG)), 2f)
        tv.press(KeyEvent.KEYCODE_DPAD_UP)
        tv.awaitFocus(hasTestTag(HERO_BUTTON_TAG))
        assertEquals(heroBefore, centreY(hasTestTag(HERO_BUTTON_TAG)), 2f)
    }

    private val heroSlot: SemanticsMatcher = hasTestTag(HERO_PLACEHOLDER_TAG) or hasTestTag(HERO_BUTTON_TAG) or hasTestTag(HOME_STATUS_TAG)

    private fun launchUntilHomeLoaded() {
        tv.launch()
        tv.await(hasTestTag(HERO_BUTTON_TAG) or hasTestTag(HOME_STATUS_TAG))
        assumeTrue("Home has no featured row (no network?)", compose.onAllNodes(hasTestTag(HERO_BUTTON_TAG)).fetchSemanticsNodes().isNotEmpty())
    }

    private val anyCard: SemanticsMatcher = hasTestTag(POSTER_CARD_TAG) or hasTestTag(CONTINUE_WATCHING_CARD_TAG)

    private fun centreY(matcher: SemanticsMatcher): Float = compose.onNode(matcher).fetchSemanticsNode().centreY

    /** The centre of the laid out node; `boundsInRoot` is clipped to the screen. */
    private val SemanticsNode.centreY: Float
        get() = positionInRoot.y + size.height / 2f

    private fun focused(): SemanticsNode = compose.onNode(isFocused()).fetchSemanticsNode()

    private fun focusedHas(tag: String): Boolean = compose.onAllNodes(isFocused() and hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    private val SemanticsNode.stateDescription: String?
        get() = config.getOrNull(SemanticsProperties.StateDescription)

    private val SemanticsNode.contentDescription: String?
        get() = config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()

    private companion object {
        const val MAX_ROWS = 4
        const val ROW_INSET_DP = 90
    }
}
