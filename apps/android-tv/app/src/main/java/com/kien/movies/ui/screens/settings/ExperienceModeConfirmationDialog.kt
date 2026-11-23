package com.kien.movies.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.domain.model.ExperienceMode
import com.kien.movies.ui.components.MoviesDialog
import com.kien.movies.ui.components.expressive.MoviesButton
import com.kien.movies.ui.components.expressive.MoviesButtonStyle
import com.kien.movies.ui.theme.MoviesTheme

@Composable
internal fun ExperienceModeConfirmationDialog(
    targetMode: ExperienceMode,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isEssential = targetMode == ExperienceMode.ESSENTIAL
    val confirmFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        confirmFocusRequester.requestFocus()
    }

    MoviesDialog(
        onDismiss = onDismiss,
        title = stringResource(
            if (isEssential) {
                R.string.experience_mode_confirm_essential_title
            } else {
                R.string.experience_mode_confirm_advanced_title
            }
        ),
        subtitle = if (isEssential) {
            stringResource(R.string.experience_mode_confirm_essential_subtitle)
        } else {
            stringResource(R.string.experience_mode_confirm_advanced_subtitle)
        },
        suppressFirstKeyUp = false
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.md)
        ) {
            MoviesButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.action_cancel))
            }
            MoviesButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(confirmFocusRequester),
                style = MoviesButtonStyle.Primary
            ) {
                Text(
                    stringResource(
                        if (isEssential) {
                            R.string.experience_mode_switch_to_essential
                        } else {
                            R.string.experience_mode_switch_to_advanced
                        }
                    )
                )
            }
        }
    }
}
