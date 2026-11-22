@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.screens.account

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun SyncCodeGenerateScreen(
    onBackPress: () -> Unit = {},
    viewModel: AccountViewModel = hiltViewModel()
) = TenFootTypography {
    BackHandler { onBackPress() }

    val uiState by viewModel.uiState.collectAsState()
    var pin by remember { mutableStateOf("") }
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
                text = stringResource(R.string.sync_generate_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MoviesTheme.colors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(MoviesTheme.spacing.sm))

            Text(
                text = stringResource(R.string.sync_generate_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MoviesTheme.colors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(MoviesTheme.spacing.xl))

            if (uiState.generatedSyncCode != null) {
                // Show the generated code
                Text(
                    text = stringResource(R.string.sync_generate_code_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MoviesTheme.colors.TextSecondary
                )
                Spacer(modifier = Modifier.height(MoviesTheme.spacing.sm))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MoviesTheme.colors.Secondary.copy(alpha = 0.1f),
                            shape = M3eShapes.panel
                        )
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.generatedSyncCode ?: "",
                        style = MaterialTheme.typography.titleLarge.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MoviesTheme.colors.Secondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(MoviesTheme.spacing.lg))
                Text(
                    text = stringResource(R.string.sync_generate_instruction),
                    style = MaterialTheme.typography.bodySmall,
                    color = MoviesTheme.colors.TextTertiary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(MoviesTheme.spacing.xl))
                MoviesButton(
                    onClick = {
                        viewModel.clearGeneratedSyncCode()
                        onBackPress()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    style = MoviesButtonStyle.Primary
                ) {
                    Text(stringResource(R.string.sync_generate_done), modifier = Modifier.padding(vertical = MoviesTheme.spacing.xs))
                }
            } else {
                // PIN input form
                Text(
                    text = stringResource(R.string.sync_generate_pin_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MoviesTheme.colors.TextSecondary,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(MoviesTheme.spacing.xs))
                InputField(
                    value = pin,
                    onValueChange = { if (it.length <= 8) pin = it },
                    placeholder = stringResource(R.string.sync_generate_pin_placeholder),
                    keyboardType = KeyboardType.NumberPassword,
                    isPassword = true,
                    imeAction = ImeAction.Done,
                    onImeAction = {
                        keyboardController?.hide()
                        if (pin.length >= 4) viewModel.generateSyncCode(pin)
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
                    onClick = { viewModel.generateSyncCode(pin) },
                    enabled = !uiState.isLoading && pin.length >= 4,
                    modifier = Modifier.fillMaxWidth(),
                    style = MoviesButtonStyle.Primary
                ) {
                    Text(
                        text = if (uiState.isLoading) stringResource(R.string.sync_generate_generating) else stringResource(R.string.sync_generate_code_btn),
                        modifier = Modifier.padding(vertical = MoviesTheme.spacing.xs),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
