@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.account

import com.nuvio.tv.ui.screens.settings.expressiveCard
import com.nuvio.tv.ui.screens.settings.expressiveCardFocusFill
import com.nuvio.tv.ui.screens.settings.expressiveRingless
import com.nuvio.tv.ui.theme.NuvioTheme
import com.nuvio.tv.ui.theme.bodyFloor
import com.nuvio.tv.ui.theme.tvControlHeight

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.TextStyle
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

@Composable
internal fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val textFieldFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(isEditing) {
        if (isEditing) {
            textFieldFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Surface(
        onClick = { isEditing = true },
        modifier = modifier.tvControlHeight(),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = NuvioTheme.colors.BackgroundCard,
            focusedContainerColor = expressiveCardFocusFill(NuvioTheme.colors.BackgroundCard)
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(NuvioTheme.spacing.hairline, NuvioTheme.colors.Border),
                shape = RoundedCornerShape(NuvioTheme.radii.md)
            ),
            focusedBorder = Border(
                border = NuvioTheme.focusRing.border(NuvioTheme.spacing.xxs),
                shape = RoundedCornerShape(NuvioTheme.radii.md)
            )
        ).expressiveRingless(),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(NuvioTheme.radii.md))
            .expressiveCard(),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NuvioTheme.spacing.lg, vertical = 14.dp)
                .focusRequester(textFieldFocusRequester)
                .onFocusChanged {
                    if (!it.isFocused && isEditing) {
                        isEditing = false
                        keyboardController?.hide()
                    }
                },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    onImeAction()
                    isEditing = false
                    keyboardController?.hide()
                },
                onNext = {
                    onImeAction()
                    isEditing = false
                    keyboardController?.hide()
                }
            ),
            textStyle = MaterialTheme.typography.bodyMedium.bodyFloor().copy(
                color = NuvioTheme.colors.TextPrimary,
                textDirection = TextDirection.Content
            ),
            cursorBrush = SolidColor(if (isEditing) NuvioTheme.colors.Secondary else Color.Transparent),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium.bodyFloor(),
                        color = NuvioTheme.colors.TextTertiary
                    )
                }
                innerTextField()
            }
        )
    }
}
