package com.kien.movies.ui.screens.library

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kien.movies.R
import com.kien.movies.core.tracking.LOCAL_LIBRARY_LIST_KEY
import com.kien.movies.core.tracking.TrackingListManagementCapabilities
import com.kien.movies.domain.model.LibraryListPrivacy
import com.kien.movies.domain.model.LibraryListTab
import com.kien.movies.ui.theme.MoviesTheme
import com.kien.movies.ui.components.posteroptions.PosterListPickerDialog
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryListDialogsTest {
    @get:Rule
    val composeRule = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val mdblist = TrackingListManagementCapabilities(listOf(LibraryListPrivacy.PRIVATE, LibraryListPrivacy.PUBLIC))

    @Test
    fun remoteSelectionDistinguishesTraktAndMdbListWatchlists() {
        var selected: String? = null
        val tabs = listOf(
            LibraryListTab("watchlist", "Watchlist", LibraryListTab.Type.WATCHLIST, trackingProviderId = "trakt"),
            LibraryListTab("mdblist:watchlist", "Watchlist", LibraryListTab.Type.WATCHLIST, trackingProviderId = "mdblist")
        )
        composeRule.setContent {
            MoviesTheme {
                PosterListPickerDialog("Lists", tabs, emptyMap(), false, null, { selected = it }, {}, {})
            }
        }
        val watchlist = context.getString(R.string.library_watchlist)
        composeRule.onNodeWithText("${context.getString(R.string.trakt_name)} · $watchlist")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            .assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.onNodeWithText("${context.getString(R.string.mdblist_name)} · $watchlist")
            .assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.runOnIdle { assertEquals("mdblist:watchlist", selected) }
    }

    @Test
    fun pickerMarksSavedListsAsChecked() {
        val tabs = listOf(
            LibraryListTab(LOCAL_LIBRARY_LIST_KEY, "Movies", LibraryListTab.Type.WATCHLIST),
            LibraryListTab("favorites", "Favorites", LibraryListTab.Type.PERSONAL, trackingProviderId = "trakt")
        )
        composeRule.setContent {
            MoviesTheme {
                PosterListPickerDialog("Lists", tabs, mapOf(LOCAL_LIBRARY_LIST_KEY to true, "favorites" to false), false, null, {}, {}, {})
            }
        }
        composeRule.onNode(hasText("Movies", substring = true) and isToggleable()).assertIsOn()
        composeRule.onNode(hasText("Favorites", substring = true) and isToggleable()).assertIsOff()
    }

    @Test
    fun mdbListEditorOnlyOffersSupportedPrivacyAndFields() {
        composeRule.setContent {
            MoviesTheme {
                ListEditorDialog(LibraryListEditorState(LibraryListEditorState.Mode.CREATE), mdblist, false, {}, {}, {}, {}, {})
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.library_list_name_label)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.library_list_description_label)).assertDoesNotExist()
        composeRule.onNodeWithText("Private").assertIsDisplayed()
        composeRule.onNodeWithText("Public").assertIsDisplayed()
        composeRule.onNodeWithText("Friends").assertDoesNotExist()
        composeRule.onNodeWithText("Link").assertDoesNotExist()
    }

    @Test
    fun traktEditorRetainsDescriptionAndAllPrivacyChoices() {
        composeRule.setContent {
            MoviesTheme {
                ListEditorDialog(LibraryListEditorState(LibraryListEditorState.Mode.EDIT),
                    TrackingListManagementCapabilities(LibraryListPrivacy.entries, true, true), false, {}, {}, {}, {}, {})
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.library_list_description_label)).assertIsDisplayed()
        composeRule.onNodeWithText("Friends").assertIsDisplayed()
        composeRule.onNodeWithText("Link").assertIsDisplayed()
    }

    @Test
    fun mdbListManagementHidesReorderingAndAcceptsRemoteSelection() {
        var selected: String? = null
        composeRule.setContent {
            MoviesTheme {
                ManageListsDialog(
                    tabs = listOf(LibraryListTab("mdblist:list:7", "Private favourites", LibraryListTab.Type.PERSONAL)),
                    capabilities = mdblist, selectedKey = "mdblist:list:7", errorMessage = null, pending = false,
                    onSelect = { selected = it }, onCreate = {}, onEdit = {}, onMoveUp = {}, onMoveDown = {}, onDelete = {}, onDismiss = {}
                )
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.library_manage_lists)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.library_list_move_up)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.library_list_move_down)).assertDoesNotExist()
        composeRule.onNodeWithText("Private favourites").assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.runOnIdle { assertEquals("mdblist:list:7", selected) }
    }

    @Test
    fun pendingListOperationDisablesDuplicateManagementActions() {
        composeRule.setContent {
            MoviesTheme {
                ManageListsDialog(emptyList(), mdblist, null, null, true, {}, {}, {}, {}, {}, {}, {})
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.library_list_create)).assertIsNotEnabled()
        composeRule.onNodeWithText(context.getString(R.string.library_list_edit)).assertIsNotEnabled()
        composeRule.onNodeWithText(context.getString(R.string.library_list_delete)).assertIsNotEnabled()
    }

    @Test
    fun emptyLibraryAllowsCreatingTheFirstListWithTheRemote() {
        var created = 0
        composeRule.setContent {
            MoviesTheme {
                ManageListsDialog(emptyList(), mdblist, null, null, false, {}, { created++ }, {}, {}, {}, {}, {})
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.library_list_create))
            .assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.runOnIdle { assertEquals(1, created) }
    }
}
