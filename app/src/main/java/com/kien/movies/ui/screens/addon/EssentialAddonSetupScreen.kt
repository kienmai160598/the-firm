package com.kien.movies.ui.screens.addon

import com.kien.movies.ui.theme.MoviesTheme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.core.server.AddonWebConfigMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.TextStyle
import com.kien.movies.ui.screens.settings.expressiveCardShape
import com.kien.movies.ui.screens.settings.expressivePill
import com.kien.movies.ui.screens.settings.expressiveRingless
import com.kien.movies.ui.screens.settings.expressiveControlGrow
import com.kien.movies.ui.screens.settings.expressiveControl
import com.kien.movies.ui.screens.settings.TenFootTypography
import com.kien.movies.ui.theme.isExpressiveBuild
import com.kien.movies.ui.theme.tvControlHeight
import com.kien.movies.ui.theme.bodyFloor
import androidx.compose.ui.unit.Dp

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EssentialAddonSetupScreen(
    onSkip: () -> Unit,
    viewModel: AddonManagerViewModel = hiltViewModel()
) {
    TenFootTypography {
        EssentialAddonSetupBody(onSkip = onSkip, viewModel = viewModel)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EssentialAddonSetupBody(
    onSkip: () -> Unit,
    viewModel: AddonManagerViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    DisposableEffect(Unit) {
        onDispose { viewModel.stopQrMode() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 58.dp, vertical = 42.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.essential_addon_setup_title),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, textDirection = TextDirection.Content),
                color = MoviesTheme.colors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.essential_addon_setup_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MoviesTheme.colors.TextSecondary
            )
            Spacer(modifier = Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(setupCardHeight()),
                    colors = CardDefaults.cardColors(containerColor = MoviesTheme.colors.BackgroundCard),
                    shape = expressiveCardShape(RoundedCornerShape(MoviesTheme.radii.md))
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.addon_install_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, textDirection = TextDirection.Content),
                            color = MoviesTheme.colors.TextPrimary,
                        )
                        BasicTextField(
                            value = uiState.installUrl,
                            onValueChange = viewModel::onInstallUrlChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(MoviesTheme.spacing.xxxl),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    viewModel.installAddon()
                                    keyboardController?.hide()
                                }
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium.bodyFloor().copy(
                                color = MoviesTheme.colors.TextPrimary,
                                textDirection = TextDirection.Content
                            ),
                            cursorBrush = SolidColor(MoviesTheme.colors.Primary),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 14.dp, vertical = MoviesTheme.spacing.md)
                                ) {
                                    if (uiState.installUrl.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.addon_install_placeholder),
                                            style = MaterialTheme.typography.bodyMedium.bodyFloor(),
                                            color = MoviesTheme.colors.TextTertiary
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        Button(
                            onClick = {
                                viewModel.installAddon()
                                keyboardController?.hide()
                            },
                            enabled = !uiState.isInstalling,
                            modifier = Modifier.tvControlHeight(),
                            colors = ButtonDefaults.colors(
                                containerColor = MoviesTheme.colors.Secondary,
                                contentColor = MoviesTheme.colors.OnSecondary,
                                focusedContainerColor = MoviesTheme.colors.SecondaryVariant
                            ).expressiveControl(primary = true),
                            shape = ButtonDefaults.shape(RoundedCornerShape(50)).expressivePill(),
                            border = ButtonDefaults.border().expressiveRingless(),
                            scale = ButtonDefaults.scale().expressiveControlGrow()
                        ) {
                            Text(
                                text = if (uiState.isInstalling) {
                                    stringResource(R.string.addon_installing)
                                } else {
                                    stringResource(R.string.addon_install_btn)
                                }
                            )
                        }
                        AnimatedVisibility(visible = uiState.error != null) {
                            Text(
                                text = uiState.error.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MoviesTheme.colors.Error
                            )
                        }
                    }
                }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(setupCardHeight()),
                    colors = CardDefaults.cardColors(containerColor = MoviesTheme.colors.BackgroundCard),
                    shape = expressiveCardShape(RoundedCornerShape(MoviesTheme.radii.md))
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = MoviesTheme.colors.Primary
                        )
                        Text(
                            text = stringResource(R.string.addon_manage_from_phone_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, textDirection = TextDirection.Content),
                            color = MoviesTheme.colors.TextPrimary,
                        )
                        Text(
                            text = stringResource(R.string.addon_manage_addons_only_from_phone_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MoviesTheme.colors.TextSecondary
                        )
                        Button(
                            onClick = { viewModel.startQrMode(AddonWebConfigMode.ADDONS_ONLY) },
                            modifier = Modifier.tvControlHeight(),
                            colors = ButtonDefaults.colors(
                                containerColor = MoviesTheme.colors.BackgroundElevated,
                                contentColor = MoviesTheme.colors.TextPrimary,
                                focusedContainerColor = MoviesTheme.colors.FocusBackground,
                                focusedContentColor = MoviesTheme.colors.Primary
                            ).expressiveControl(),
                            shape = ButtonDefaults.shape(RoundedCornerShape(50)).expressivePill(),
                            border = ButtonDefaults.border().expressiveRingless(),
                            scale = ButtonDefaults.scale().expressiveControlGrow()
                        ) {
                            Icon(imageVector = Icons.Default.QrCode2, contentDescription = null)
                            Text(text = stringResource(R.string.essential_addon_show_qr))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(MoviesTheme.spacing.xl))
            Button(
                onClick = onSkip,
                modifier = Modifier.tvControlHeight(),
                colors = ButtonDefaults.colors(
                    containerColor = MoviesTheme.colors.BackgroundCard,
                    contentColor = MoviesTheme.colors.TextPrimary,
                    focusedContainerColor = MoviesTheme.colors.FocusBackground,
                    focusedContentColor = MoviesTheme.colors.Primary
                ).expressiveControl(),
                shape = ButtonDefaults.shape(RoundedCornerShape(50)).expressivePill(),
                border = ButtonDefaults.border().expressiveRingless(),
                scale = ButtonDefaults.scale().expressiveControlGrow()
            ) {
                Text(
                    text = stringResource(R.string.essential_addon_continue_for_now),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                )
            }
        }

        if (uiState.isQrModeActive || uiState.pendingChange != null) {
            if (uiState.isQrModeActive) {
                QrCodeOverlay(
                    qrBitmap = uiState.qrCodeBitmap,
                    serverUrl = uiState.serverUrl,
                    instruction = stringResource(R.string.addon_qr_addons_only_scan_instruction),
                    onClose = viewModel::stopQrMode,
                    hasPendingChange = uiState.pendingChange != null
                )
            }

            uiState.pendingChange?.let { pending ->
                ConfirmAddonChangesDialog(
                    pendingChange = pending,
                    onConfirm = viewModel::confirmPendingChange,
                    onReject = viewModel::rejectPendingChange
                )
            }
        }

        AddonMessageOverlay(
            message = uiState.transientMessage,
            isError = uiState.transientMessageIsError
        )
    }
}

private val UpstreamSetupCardHeight = 220.dp

// Room for the 10-foot type and 48dp buttons, which outgrow upstream's 220dp.
private val TenFootSetupCardHeight = 256.dp

private fun setupCardHeight(): Dp =
    if (isExpressiveBuild) TenFootSetupCardHeight else UpstreamSetupCardHeight
