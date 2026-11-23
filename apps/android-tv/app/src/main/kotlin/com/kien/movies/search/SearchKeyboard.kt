package com.kien.movies.search

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kien.movies.R

/** Focus handles of the keyboard: the row as a whole (restores the last key), "a", and "123". */
internal class SearchKeyboardFocus {
    val row = FocusRequester()
    val firstLetter = FocusRequester()
    val numbers = FocusRequester()
}

/**
 * The one row keyboard of tvOS `.searchable` (spec section 3 "Search", reference 38-search.png):
 * "123", "SPACE", a to z, delete, centred on the content width. Android has no inline system
 * keyboard, so it is drawn here; "123" opens the system IME for digits and symbols.
 *
 * Entering the row lands on "a" the first time and on the last key afterwards. A focused key turns
 * into a light rounded square and grows in its graphics layer, over its neighbours, without
 * changing layout.
 */
@Composable
internal fun SearchKeyboard(
    focus: SearchKeyboardFocus,
    onType: (String) -> Unit,
    onDelete: () -> Unit,
    onOpenIme: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TvGrid.margin)
            .focusRequester(focus.row)
            .focusRestorer(focus.firstLetter)
            .focusGroup()
            .testTag(SEARCH_KEYBOARD_TAG),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Keycap(label = NUMBERS_LABEL, id = KEY_NUMBERS, width = NUMBERS_WIDTH, onClick = onOpenIme, modifier = Modifier.focusRequester(focus.numbers))
        Spacer(Modifier.width(NUMBERS_TO_SPACE))
        Keycap(label = SPACE_LABEL, id = KEY_SPACE, width = SPACE_WIDTH, onClick = { onType(" ") })
        Spacer(Modifier.width(SPACE_TO_LETTERS))
        LETTERS.forEachIndexed { index, letter ->
            val requester = if (index == 0) Modifier.focusRequester(focus.firstLetter) else Modifier
            KeyCell(id = letter.toString(), onClick = { onType(letter.toString()) }, modifier = requester) {
                Text(text = letter.toString(), style = letterStyle)
            }
        }
        Spacer(Modifier.width(LETTERS_TO_DELETE))
        KeyCell(id = KEY_DELETE, onClick = onDelete, modifier = Modifier.semantics { contentDescription = DELETE_DESCRIPTION }) {
            Icon(painter = painterResource(R.drawable.ic_key_delete), contentDescription = null, modifier = Modifier.size(DELETE_ICON))
        }
    }
}

/** A letter or the delete key: a transparent cell on the letter pitch, light square when focused. */
@Composable
private fun KeyCell(id: String, onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = modifier.size(letterCell).raisedWhenFocused(interactionSource).testTag(searchKeyTag(id)),
        shape = ClickableSurfaceDefaults.shape(shape = keyShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = TvColors.textSecondary,
            focusedContainerColor = TvColors.focusedFill,
            focusedContentColor = TvColors.focusedContent,
            pressedContainerColor = TvColors.focusedFill,
            pressedContentColor = TvColors.focusedContent
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = KEY_FOCUS_SCALE),
        interactionSource = interactionSource
    ) {
        Box(modifier = Modifier.align(Alignment.Center), content = content)
    }
}

/** "123" and "SPACE": small grey keycaps with dark labels, centred on the letter row. */
@Composable
private fun Keycap(label: String, id: String, width: Dp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = modifier.size(width, KEYCAP_HEIGHT).raisedWhenFocused(interactionSource).testTag(searchKeyTag(id)),
        shape = ClickableSurfaceDefaults.shape(shape = keycapShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = KEYCAP_FILL,
            contentColor = KEYCAP_LABEL,
            focusedContainerColor = TvColors.focusedFill,
            focusedContentColor = TvColors.focusedContent,
            pressedContainerColor = TvColors.focusedFill,
            pressedContentColor = TvColors.focusedContent
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = KEY_FOCUS_SCALE),
        interactionSource = interactionSource
    ) {
        Text(text = label, style = keycapStyle, modifier = Modifier.align(Alignment.Center))
    }
}

/** The focused key grows over its neighbours, so it draws after them. */
@Composable
private fun Modifier.raisedWhenFocused(interactionSource: MutableInteractionSource): Modifier {
    val isFocused by interactionSource.collectIsFocusedAsState()
    return zIndex(if (isFocused) 1f else 0f)
}

fun searchKeyTag(id: String): String = "search-key-$id"

const val SEARCH_KEYBOARD_TAG = "search-keyboard"
const val KEY_NUMBERS = "123"
const val KEY_SPACE = "space"
const val KEY_DELETE = "delete"

private const val LETTERS = "abcdefghijklmnopqrstuvwxyz"
private const val NUMBERS_LABEL = "123"
private const val SPACE_LABEL = "SPACE"
private const val DELETE_DESCRIPTION = "Delete"

// Measured on 38-search.png (4K px / 4 = dp), not in the spec: see the slice report.

/** Letter pitch 54 pt; the focused square is 66 x 76 pt, so the cell is 27 x 32 dp at scale 1.2. */
private val letterCell = DpSize(27.dp, 32.dp)
private const val KEY_FOCUS_SCALE = 1.2f
private val keyShape = RoundedCornerShape(8.dp)
private val NUMBERS_WIDTH = 29.dp
private val SPACE_WIDTH = 44.dp
private val KEYCAP_HEIGHT = 19.dp
private val keycapShape = RoundedCornerShape(4.dp)
private val NUMBERS_TO_SPACE = 16.dp
private val SPACE_TO_LETTERS = 15.5.dp
private val LETTERS_TO_DELETE = 10.dp
private val DELETE_ICON = 22.dp
private val KEYCAP_FILL = Color(0xFF95999C)
private val KEYCAP_LABEL = TvColors.searchBackground

/** Title 3, 48 pt (x-height 25 pt measured). */
private val letterStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Normal, lineHeight = 28.sp)

/** Keycap labels measure 10 sp; 11 sp is the spec's floor. */
private val keycapStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 13.sp)
