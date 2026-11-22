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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.ui.components.NuvioDialog
import com.kien.movies.ui.theme.NuvioPrimitives
import com.kien.movies.ui.theme.NuvioTheme
import com.kien.movies.ui.theme.tvControlHeight
import com.kien.movies.ui.screens.settings.expressiveControl
import com.kien.movies.ui.screens.settings.expressiveControlGrow
import com.kien.movies.ui.screens.settings.expressivePill
import com.kien.movies.ui.screens.settings.expressiveRingless

@Composable
internal fun AccountSignOutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val cancelFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        cancelFocusRequester.requestFocus()
    }

    NuvioDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.account_sign_out_confirm_title),
        subtitle = stringResource(R.string.account_sign_out_confirm_subtitle),
        suppressFirstKeyUp = false
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md)
        ) {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .tvControlHeight()
                    .focusRequester(cancelFocusRequester),
                colors = ButtonDefaults.colors(
                    containerColor = NuvioTheme.colors.BackgroundCard,
                    contentColor = NuvioTheme.colors.TextPrimary
                ).expressiveControl(),
                shape = ButtonDefaults.shape().expressivePill(),
                border = ButtonDefaults.border().expressiveRingless(),
                scale = ButtonDefaults.scale().expressiveControlGrow()
            ) {
                Text(stringResource(R.string.action_cancel))
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f).tvControlHeight(),
                colors = accountDestructiveColors(ButtonDefaults.colors(
                    containerColor = NuvioPrimitives.red600.copy(alpha = 0.25f),
                    contentColor = Color(0xFFF44336)
                )),
                shape = ButtonDefaults.shape().expressivePill(),
                border = ButtonDefaults.border().expressiveRingless(),
                scale = ButtonDefaults.scale().expressiveControlGrow()
            ) {
                Text(stringResource(R.string.account_sign_out))
            }
        }
    }
}
