@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.screens.account

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
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.ui.components.MoviesDialog
import com.kien.movies.ui.components.expressive.MoviesButton
import com.kien.movies.ui.components.expressive.MoviesButtonStyle
import com.kien.movies.ui.theme.MoviesTheme

@Composable
internal fun AccountSignOutConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val cancelFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        cancelFocusRequester.requestFocus()
    }

    MoviesDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.account_sign_out_confirm_title),
        subtitle = stringResource(R.string.account_sign_out_confirm_subtitle),
        suppressFirstKeyUp = false
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.md)
        ) {
            MoviesButton(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(cancelFocusRequester)
            ) {
                Text(stringResource(R.string.action_cancel))
            }
            MoviesButton(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                style = MoviesButtonStyle.Destructive
            ) {
                Text(stringResource(R.string.account_sign_out))
            }
        }
    }
}
