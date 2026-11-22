@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.screens.account

import com.kien.movies.ui.theme.MoviesPrimitives
import com.kien.movies.ui.theme.MoviesTheme
import com.kien.movies.ui.theme.bodyFloor
import com.kien.movies.ui.theme.tvControlHeight
import com.kien.movies.ui.screens.settings.TenFootTypography
import com.kien.movies.ui.screens.settings.buttonIconSize
import com.kien.movies.ui.screens.settings.expressiveCardShape
import com.kien.movies.ui.screens.settings.expressiveControlGrow
import com.kien.movies.ui.screens.settings.expressivePill
import com.kien.movies.ui.screens.settings.expressiveRingless

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.ui.res.stringResource
import com.kien.movies.R
import com.kien.movies.domain.model.AuthState

private const val SHOW_SYNC_CODE_FEATURES = false

@Composable
fun AccountScreen(
    onNavigateToAuthSignIn: () -> Unit = {},
    onNavigateToSyncGenerate: () -> Unit = {},
    onNavigateToSyncClaim: () -> Unit = {},
    onBackPress: () -> Unit = {},
    viewModel: AccountViewModel = hiltViewModel()
) = TenFootTypography {
    BackHandler { onBackPress() }

    val uiState by viewModel.uiState.collectAsState()
    var showSignOutConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.authState) {
        if (uiState.authState is AuthState.FullAccount) {
            viewModel.loadLinkedDevices()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MoviesTheme.spacing.xxxl),
        contentPadding = PaddingValues(vertical = MoviesTheme.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.lg)
    ) {
        item {
            Text(
                text = stringResource(R.string.account_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MoviesTheme.colors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(MoviesTheme.spacing.sm))
        }

        when (val authState = uiState.authState) {
            is AuthState.Loading -> {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.account_loading),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MoviesTheme.colors.TextSecondary
                        )
                    }
                }
            }

            is AuthState.SignedOut -> {
                item {
                    Text(
                        text = stringResource(R.string.account_sign_in_description),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MoviesTheme.colors.TextSecondary
                    )
                }
                item {
                    AccountActionCard(
                        icon = Icons.Default.Person,
                        title = stringResource(R.string.account_signin_create_title),
                        description = stringResource(R.string.account_signin_create_desc),
                        onClick = onNavigateToAuthSignIn
                    )
                }
                if (SHOW_SYNC_CODE_FEATURES) {
                    item {
                        Spacer(modifier = Modifier.height(MoviesTheme.spacing.sm))
                        Text(
                            text = stringResource(R.string.account_sync_code_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MoviesTheme.colors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(MoviesTheme.spacing.xs))
                        Text(
                            text = stringResource(R.string.account_sync_code_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MoviesTheme.colors.TextSecondary
                        )
                    }
                    item {
                        AccountActionCard(
                            icon = Icons.Default.VpnKey,
                            title = stringResource(R.string.sync_generate_title),
                            description = stringResource(R.string.account_generate_sync_desc),
                            onClick = onNavigateToSyncGenerate
                        )
                    }
                    item {
                        AccountActionCard(
                            icon = Icons.Default.Sync,
                            title = stringResource(R.string.sync_claim_title),
                            description = stringResource(R.string.account_enter_sync_desc),
                            onClick = onNavigateToSyncClaim
                        )
                    }
                }
            }

            is AuthState.FullAccount -> {
                item {
                    AccountInfoCard(
                        label = stringResource(R.string.account_signed_in_as),
                        value = authState.email
                    )
                }
                item {
                    LinkedDevicesSection(
                        devices = uiState.linkedDevices,
                        onUnlink = { viewModel.unlinkDevice(it) }
                    )
                }
                if (SHOW_SYNC_CODE_FEATURES) {
                    item {
                        AccountActionCard(
                            icon = Icons.Default.VpnKey,
                            title = stringResource(R.string.sync_generate_title),
                            description = stringResource(R.string.account_generate_sync_signed_in_desc),
                            onClick = onNavigateToSyncGenerate
                        )
                    }
                }
                item {
                    SignOutButton(onClick = { showSignOutConfirmation = true })
                }
            }

        }
    }

    if (showSignOutConfirmation) {
        AccountSignOutConfirmationDialog(
            onConfirm = {
                viewModel.signOut()
                showSignOutConfirmation = false
            },
            onDismiss = { showSignOutConfirmation = false }
        )
    }
}

@Composable
private fun AccountActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.colors(
            containerColor = MoviesTheme.colors.BackgroundCard,
            focusedContainerColor = MoviesTheme.colors.FocusBackground,
            contentColor = MoviesTheme.colors.TextPrimary,
            focusedContentColor = MoviesTheme.colors.TextPrimary
        ),
        shape = ButtonDefaults.shape(
            expressiveCardShape(RoundedCornerShape(MoviesTheme.radii.md))
        ),
        border = ButtonDefaults.border().expressiveRingless()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MoviesTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MoviesTheme.colors.Secondary
            )
            Spacer(modifier = Modifier.width(MoviesTheme.spacing.lg))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MoviesTheme.colors.TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MoviesTheme.colors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun AccountInfoCard(label: String, value: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MoviesTheme.colors.BackgroundCard,
                shape = expressiveCardShape(RoundedCornerShape(MoviesTheme.radii.md))
            )
            .padding(MoviesTheme.spacing.lg)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MoviesTheme.colors.TextTertiary
            )
            Spacer(modifier = Modifier.height(MoviesTheme.spacing.xs))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MoviesTheme.colors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun LinkedDevicesSection(
    devices: List<com.kien.movies.data.remote.supabase.SupabaseLinkedDevice>,
    onUnlink: (String) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Devices,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MoviesTheme.colors.TextSecondary
            )
            Spacer(modifier = Modifier.width(MoviesTheme.spacing.sm))
            Text(
                text = stringResource(R.string.account_linked_devices, devices.size),
                style = MaterialTheme.typography.titleMedium,
                color = MoviesTheme.colors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(MoviesTheme.spacing.sm))
        if (devices.isEmpty()) {
            Text(
                text = stringResource(R.string.account_no_linked_devices),
                style = MaterialTheme.typography.bodyMedium,
                color = MoviesTheme.colors.TextTertiary
            )
        } else {
            devices.forEach { device ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MoviesTheme.colors.BackgroundCard,
                            shape = expressiveCardShape(RoundedCornerShape(MoviesTheme.radii.sm))
                        )
                        .padding(MoviesTheme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = device.deviceName ?: stringResource(R.string.account_unknown_device),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MoviesTheme.colors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { onUnlink(device.deviceUserId) },
                        modifier = Modifier.tvControlHeight(),
                        colors = accountDestructiveColors(ButtonDefaults.colors(
                            containerColor = MoviesPrimitives.red600.copy(alpha = 0.2f),
                            focusedContainerColor = MoviesPrimitives.red600.copy(alpha = 0.4f),
                            contentColor = Color(0xFFF44336),
                            focusedContentColor = Color(0xFFF44336)
                        )),
                        shape = ButtonDefaults.shape(RoundedCornerShape(MoviesTheme.radii.sm))
                            .expressivePill(),
                        border = ButtonDefaults.border().expressiveRingless(),
                        scale = ButtonDefaults.scale().expressiveControlGrow()
                    ) {
                        Icon(
                            imageVector = Icons.Default.LinkOff,
                            contentDescription = stringResource(R.string.cd_unlink),
                            modifier = Modifier.size(buttonIconSize(MoviesTheme.spacing.lg))
                        )
                        Spacer(modifier = Modifier.width(MoviesTheme.spacing.xs))
                        Text(
                            stringResource(R.string.account_unlink),
                            style = MaterialTheme.typography.labelSmall.bodyFloor()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(MoviesTheme.spacing.sm))
            }
        }
    }
}

@Composable
private fun SignOutButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.tvControlHeight(),
        colors = accountDestructiveColors(ButtonDefaults.colors(
            containerColor = MoviesPrimitives.red600.copy(alpha = 0.15f),
            focusedContainerColor = MoviesPrimitives.red600.copy(alpha = 0.3f),
            contentColor = Color(0xFFF44336),
            focusedContentColor = Color(0xFFF44336)
        )),
        shape = ButtonDefaults.shape(RoundedCornerShape(MoviesTheme.radii.md)).expressivePill(),
        border = ButtonDefaults.border().expressiveRingless(),
        scale = ButtonDefaults.scale().expressiveControlGrow()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = MoviesTheme.spacing.lg, vertical = MoviesTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Logout,
                contentDescription = null,
                modifier = Modifier.size(buttonIconSize(20.dp))
            )
            Spacer(modifier = Modifier.width(MoviesTheme.spacing.sm))
            Text(
                text = stringResource(R.string.account_sign_out),
                style = MaterialTheme.typography.bodyMedium.bodyFloor(),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
