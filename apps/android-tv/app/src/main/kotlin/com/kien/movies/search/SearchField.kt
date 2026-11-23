package com.kien.movies.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvGrid
import com.kien.movies.design.TvType

/**
 * The search field of tvOS `.searchable` (spec section 3 "Search"): a magnifier and the query in
 * Title 2, or the placeholder. It takes no focus; the keyboard under it types into it.
 *
 * [isEditing] swaps the text for a foundation BasicTextField and focuses it, which opens the
 * system IME: the "123" key's fallback for digits and symbols. While the text field has focus
 * with the IME closed, the arrows move focus (Down reaches the keyboard) and Select opens the IME
 * again. Losing focus ends editing through [onEditingEnd].
 *
 * tvOS also shows "Hold (mic) to dictate" on the right; the app has no dictation, so it is left out.
 */
@Composable
internal fun SearchField(
    state: TextFieldState,
    isEditing: Boolean,
    onEditingEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = FIELD_ICON_START, end = TvGrid.sideMargin).testTag(SEARCH_FIELD_TAG),
        horizontalArrangement = Arrangement.spacedBy(FIELD_ICON_GAP),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_tab_search),
            contentDescription = null,
            tint = TvColors.textSecondary,
            modifier = Modifier.size(FIELD_ICON)
        )
        Box(modifier = Modifier.weight(1f)) {
            if (isEditing) {
                SearchInput(state = state, onEditingEnd = onEditingEnd)
            } else {
                val text = state.text.toString()
                Text(
                    text = text.ifEmpty { PROMPT },
                    style = TvType.searchField,
                    color = if (text.isEmpty()) TvColors.textTertiary else TvColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SearchInput(state: TextFieldState, onEditingEnd: () -> Unit) {
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var hasHadFocus by remember { mutableStateOf(false) }

    BasicTextField(
        state = state,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focus)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    hasHadFocus = true
                } else if (hasHadFocus) {
                    onEditingEnd()
                }
            }
            .onPreviewKeyEvent { event -> handleRemoteKey(event, focusManager, keyboard) }
            .testTag(SEARCH_INPUT_TAG),
        textStyle = TvType.searchField.copy(color = TvColors.textPrimary),
        cursorBrush = SolidColor(TvColors.textPrimary),
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        onKeyboardAction = {
            keyboard?.hide()
            focusManager.moveFocus(FocusDirection.Down)
        },
        decorator = { innerTextField ->
            Box {
                if (state.text.isEmpty()) {
                    Text(text = PROMPT, style = TvType.searchField, color = TvColors.textTertiary, maxLines = 1)
                }
                innerTextField()
            }
        }
    )
    LaunchedEffect(Unit) { focus.requestFocus() }
}

/**
 * Arrows move focus out of the field (the IME has them while it is open); Select or Enter opens
 * the IME. Both halves of a handled key are consumed so the text field does not move its cursor.
 * The text field's own D-pad handling acts only for keys from a physical D-pad device, so never
 * for keys a UI test sends; hence this.
 */
private fun handleRemoteKey(event: KeyEvent, focusManager: FocusManager, keyboard: SoftwareKeyboardController?): Boolean {
    val direction = when (event.key) {
        Key.DirectionUp -> FocusDirection.Up
        Key.DirectionDown -> FocusDirection.Down
        Key.DirectionLeft -> FocusDirection.Left
        Key.DirectionRight -> FocusDirection.Right
        else -> null
    }
    if (direction != null) {
        if (event.type == KeyEventType.KeyDown) focusManager.moveFocus(direction)
        return true
    }
    if (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter) {
        if (event.type == KeyEventType.KeyUp) keyboard?.show()
        return true
    }
    return false
}

const val SEARCH_FIELD_TAG = "search-field"
const val SEARCH_INPUT_TAG = "search-input"

/** tvOS TV app placeholder. */
private const val PROMPT = "Shows, Movies, and More"

// Measured on 38-search.png: magnifier ink 55 pt from 25 pt past the margin, text 120 pt past it.
// The Material glyph fills 73% of its box, so the box is larger than the ink and starts earlier.
private val FIELD_ICON = 37.dp
private val FIELD_ICON_START = TvGrid.sideMargin + 8.dp
private val FIELD_ICON_GAP = 15.dp
