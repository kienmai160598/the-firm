@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.screens.account

import com.kien.movies.ui.screens.settings.expressiveCard
import com.kien.movies.ui.screens.settings.expressiveCardFocusFill
import com.kien.movies.ui.screens.settings.expressiveRingless
import com.kien.movies.ui.theme.MoviesTheme
import com.kien.movies.ui.theme.bodyFloor
import com.kien.movies.ui.theme.tvControlHeight

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
            containerColor = MoviesTheme.colors.BackgroundCard,
            focusedContainerColor = expressiveCardFocusFill(MoviesTheme.colors.BackgroundCard)
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(MoviesTheme.spacing.hairline, MoviesTheme.colors.Border),
                shape = RoundedCornerShape(MoviesTheme.radii.md)
            ),
            focusedBorder = Border(
                border = MoviesTheme.focusRing.border(MoviesTheme.spacing.xxs),
                shape = RoundedCornerShape(MoviesTheme.radii.md)
            )
        ).expressiveRingless(),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(MoviesTheme.radii.md))
            .expressiveCard(),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MoviesTheme.spacing.lg, vertical = 14.dp)
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
                color = MoviesTheme.colors.TextPrimary,
                textDirection = TextDirection.Content
            ),
            cursorBrush = SolidColor(if (isEditing) MoviesTheme.colors.Secondary else Color.Transparent),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium.bodyFloor(),
                        color = MoviesTheme.colors.TextTertiary
                    )
                }
                innerTextField()
            }
        )
    }
}
