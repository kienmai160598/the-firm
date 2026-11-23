package com.kien.movies.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.Color
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
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.design.CardSize
import com.kien.movies.design.Palette
import com.kien.movies.design.TextStyles

/**
 * The search field (tvOS `.searchable`, docs section 4: a text field with the system IME).
 *
 * Two steps, so the IME opens on a press and not on every pass of the D-pad focus:
 * - At rest a tv-material Surface shows the query. The shell's first focus lands here without
 *   opening the IME, so Back still reaches the sidebar in one press.
 * - Select swaps in a foundation BasicTextField and focuses it, which opens the IME. While it has
 *   focus with the IME closed, the arrows move focus (Down reaches the first row) and Select opens
 *   the IME again. Losing focus returns to the Surface.
 *
 * The arrows are handled here rather than by the text field's own D-pad handling, which acts only
 * for keys from a physical D-pad device and so never for keys a UI test sends.
 */
@Composable
internal fun SearchField(state: TextFieldState, buttonFocus: FocusRequester, modifier: Modifier = Modifier) {
    var isEditing by remember { mutableStateOf(false) }
    if (isEditing) {
        SearchInput(state = state, onEditingEnd = { isEditing = false }, modifier = modifier)
    } else {
        SearchFieldButton(text = state.text.toString(), onClick = { isEditing = true }, modifier = modifier.focusRequester(buttonFocus))
    }
}

@Composable
private fun SearchFieldButton(text: String, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.width(FIELD_WIDTH).testTag(SEARCH_FIELD_TAG),
        shape = ClickableSurfaceDefaults.shape(shape = fieldShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Palette.frostRest,
            contentColor = Palette.textPrimary,
            focusedContainerColor = Palette.focusedContainer,
            focusedContentColor = Palette.focusedContent,
            pressedContainerColor = Palette.focusedContainer,
            pressedContentColor = Palette.focusedContent
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
    ) {
        FieldContent {
            Text(
                text = text.ifEmpty { PROMPT },
                style = TextStyles.body,
                color = if (text.isEmpty()) LocalContentColor.current.copy(alpha = PROMPT_ALPHA) else Color.Unspecified,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SearchInput(state: TextFieldState, onEditingEnd: () -> Unit, modifier: Modifier) {
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var hasHadFocus by remember { mutableStateOf(false) }

    BasicTextField(
        state = state,
        modifier = modifier
            .width(FIELD_WIDTH)
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
        textStyle = TextStyles.body.copy(color = Palette.focusedContent),
        cursorBrush = SolidColor(Palette.focusedContent),
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        onKeyboardAction = { keyboard?.hide() },
        decorator = { innerTextField ->
            CompositionLocalProvider(LocalContentColor provides Palette.focusedContent) {
                FieldContent(modifier = Modifier.background(Palette.focusedContainer, fieldShape)) {
                    Box {
                        if (state.text.isEmpty()) {
                            Text(text = PROMPT, style = TextStyles.body, color = Palette.focusedContent.copy(alpha = PROMPT_ALPHA), maxLines = 1)
                        }
                        innerTextField()
                    }
                }
            }
        }
    )
    LaunchedEffect(Unit) { focus.requestFocus() }
}

/** The magnifier and the text, the same frame at rest and while editing. */
@Composable
private fun FieldContent(modifier: Modifier = Modifier, text: @Composable () -> Unit) {
    Row(
        modifier = modifier.padding(horizontal = FIELD_PADDING_H, vertical = FIELD_PADDING_V),
        horizontalArrangement = Arrangement.spacedBy(FIELD_ICON_GAP),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = painterResource(R.drawable.ic_tab_search), contentDescription = null, modifier = Modifier.size(FIELD_ICON))
        text()
    }
}

/**
 * Arrows move focus out of the field (the IME has them while it is open); Select or Enter opens
 * the IME. Both halves of a handled key are consumed so the text field does not move its cursor.
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

/** tvOS `.searchable(prompt:)`. */
private const val PROMPT = "Movies and shows"
private const val PROMPT_ALPHA = 0.6f

private val fieldShape = RoundedCornerShape(CardSize.cornerRadius)

// No tvOS literal (the system draws the search field); sized for one line of body text.
private val FIELD_WIDTH = 430.dp
private val FIELD_PADDING_H = 16.dp
private val FIELD_PADDING_V = 10.dp
private val FIELD_ICON = 18.dp
private val FIELD_ICON_GAP = 10.dp
