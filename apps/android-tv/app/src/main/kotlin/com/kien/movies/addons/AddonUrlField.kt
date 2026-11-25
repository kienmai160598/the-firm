package com.kien.movies.addons

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kien.movies.design.Focus
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvGlass
import com.kien.movies.design.TvShapes
import com.kien.movies.design.TvType
import com.kien.movies.design.focusLift
import com.kien.movies.settings.SETTINGS_ROW_TITLE_INSET
import com.kien.movies.settings.SettingsMetrics

/**
 * The Add Addon row (the tvOS `TextField` of AddonsView.swift). A text field that takes D-pad
 * focus opens the keyboard at once and keeps the arrows, so the row rests as a focusable surface;
 * Select starts editing in the system keyboard, Done submits, Back leaves editing. Either way
 * focus returns to the surface. [modifier] goes on the row's focus group, editing included.
 */
@Composable
fun AddonUrlField(value: String, onValueChange: (String) -> Unit, onSubmit: () -> Unit, focusRequester: FocusRequester, modifier: Modifier = Modifier) {
    var isEditing by remember { mutableStateOf(false) }
    var refocusTick by remember { mutableIntStateOf(0) }
    val stopEditing: () -> Unit = {
        isEditing = false
        refocusTick++
    }

    Box(modifier = modifier.fillMaxWidth().focusRequester(focusRequester).focusGroup()) {
        if (isEditing) {
            BackHandler(onBack = stopEditing)
            EditingField(value = value, onValueChange = onValueChange, onDone = {
                stopEditing()
                onSubmit()
            }, onFocusLost = { isEditing = false })
        } else {
            Surface(
                onClick = { isEditing = true },
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = TvColors.settingsRow,
                    contentColor = TvColors.textPrimary,
                    focusedContainerColor = TvColors.focusedFill,
                    focusedContentColor = TvColors.focusedContent
                ),
                scale = TvGlass.noSurfaceScale(),
                shape = TvGlass.surfaceShape(TvShapes.pill),
                modifier = Modifier.fillMaxWidth().height(SettingsMetrics.rowHeight).testTag(URL_SURFACE_TAG).focusLift(Focus.wideRowScale)
            ) {
                val color = LocalContentColor.current
                Text(
                    text = value.ifEmpty { PLACEHOLDER },
                    style = TvType.body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (value.isEmpty()) color.copy(alpha = PLACEHOLDER_ALPHA) else color,
                    modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = TEXT_INSET)
                )
            }
        }
    }
    LaunchedEffect(refocusTick) {
        if (refocusTick == 0) return@LaunchedEffect
        withFrameNanos {}
        focusRequester.requestFocus()
    }
}

@Composable
private fun EditingField(value: String, onValueChange: (String) -> Unit, onDone: () -> Unit, onFocusLost: () -> Unit) {
    var hadFocus by remember { mutableStateOf(false) }
    val textFocus = remember { FocusRequester() }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TvType.body.copy(color = TvColors.focusedContent),
        cursorBrush = SolidColor(TvColors.focusedContent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier
            .fillMaxWidth()
            .height(SettingsMetrics.rowHeight)
            .focusRequester(textFocus)
            .onFocusChanged { focus ->
                // Called once unfocused on attach: only a real focus loss ends editing.
                if (focus.hasFocus) {
                    hadFocus = true
                } else if (hadFocus) {
                    hadFocus = false
                    onFocusLost()
                }
            }
            .testTag(URL_FIELD_TAG),
        decorationBox = { field ->
            Box(
                modifier = Modifier.fillMaxWidth().height(SettingsMetrics.rowHeight).background(TvColors.focusedFill, TvShapes.pill).padding(horizontal = TEXT_INSET),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) Text(text = PLACEHOLDER, style = TvType.body, color = TvColors.focusedContent.copy(alpha = PLACEHOLDER_ALPHA))
                field()
            }
        }
    )
    LaunchedEffect(Unit) {
        withFrameNanos {}
        textFocus.requestFocus()
    }
}

const val URL_SURFACE_TAG = "addon-url"
const val URL_FIELD_TAG = "addon-url-field"

private const val PLACEHOLDER = "Add Addon"
private const val PLACEHOLDER_ALPHA = 0.45f // the tvOS placeholder grey on the focused field
private val TEXT_INSET = SETTINGS_ROW_TITLE_INSET
