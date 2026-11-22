@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.screens.account

import com.kien.movies.ui.theme.MoviesPrimitives
import com.kien.movies.ui.theme.MoviesTheme

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.ui.res.stringResource
import com.kien.movies.R
import com.kien.movies.ui.components.expressive.M3eShapes
import com.kien.movies.ui.components.expressive.MoviesButton
import com.kien.movies.ui.components.expressive.MoviesButtonStyle
import com.kien.movies.ui.screens.settings.TenFootTypography

@Composable
fun SyncCodeClaimScreen(
    onBackPress: () -> Unit = {},
    viewModel: AccountViewModel = hiltViewModel()
) = TenFootTypography {
    BackHandler { onBackPress() }

    val uiState by viewModel.uiState.collectAsState()
    var code by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .background(
                    color = MoviesTheme.colors.BackgroundElevated,
                    shape = M3eShapes.panel
                )
                .padding(MoviesTheme.spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.sync_claim_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MoviesTheme.colors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(MoviesTheme.spacing.sm))

            Text(
                text = stringResource(R.string.sync_claim_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MoviesTheme.colors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(MoviesTheme.spacing.xl))

            if (uiState.syncClaimSuccess) {
                // Success state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MoviesPrimitives.green700.copy(alpha = 0.15f),
                            shape = M3eShapes.panel
                        )
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.sync_claim_success),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MoviesPrimitives.green300,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(MoviesTheme.spacing.xl))
                MoviesButton(
                    onClick = {
                        viewModel.clearSyncClaimSuccess()
                        onBackPress()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    style = MoviesButtonStyle.Primary
                ) {
                    Text(stringResource(R.string.sync_claim_done), modifier = Modifier.padding(vertical = MoviesTheme.spacing.xs))
                }
            } else {
                // Code input
                Text(
                    text = stringResource(R.string.sync_claim_code_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MoviesTheme.colors.TextSecondary,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(MoviesTheme.spacing.xs))
                InputField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    placeholder = stringResource(R.string.sync_claim_code_placeholder),
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
                )

                Spacer(modifier = Modifier.height(MoviesTheme.spacing.lg))

                // PIN input
                Text(
                    text = stringResource(R.string.sync_claim_pin_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MoviesTheme.colors.TextSecondary,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(MoviesTheme.spacing.xs))
                InputField(
                    value = pin,
                    onValueChange = { if (it.length <= 8) pin = it },
                    placeholder = stringResource(R.string.sync_claim_pin_placeholder),
                    keyboardType = KeyboardType.NumberPassword,
                    isPassword = true,
                    imeAction = ImeAction.Done,
                    onImeAction = {
                        keyboardController?.hide()
                        if (code.isNotBlank() && pin.length >= 4) {
                            viewModel.claimSyncCode(code, pin)
                        }
                    }
                )

                if (uiState.error != null) {
                    Spacer(modifier = Modifier.height(MoviesTheme.spacing.sm))
                    Text(
                        text = uiState.error ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = accountErrorContent()
                    )
                }

                Spacer(modifier = Modifier.height(MoviesTheme.spacing.xl))

                MoviesButton(
                    onClick = { viewModel.claimSyncCode(code, pin) },
                    enabled = !uiState.isLoading && code.isNotBlank() && pin.length >= 4,
                    modifier = Modifier.fillMaxWidth(),
                    style = MoviesButtonStyle.Primary
                ) {
                    Text(
                        text = if (uiState.isLoading) stringResource(R.string.sync_claim_linking) else stringResource(R.string.sync_claim_title),
                        modifier = Modifier.padding(vertical = MoviesTheme.spacing.xs),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
