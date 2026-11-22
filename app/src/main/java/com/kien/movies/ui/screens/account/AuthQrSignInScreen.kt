@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.screens.account

import com.kien.movies.ui.theme.MoviesTheme

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.kien.movies.BuildConfig
import com.kien.movies.R
import com.kien.movies.domain.model.AuthState
import com.kien.movies.ui.components.BrandWordmark
import com.kien.movies.ui.components.expressive.M3eShapes
import com.kien.movies.ui.components.expressive.MoviesButton
import com.kien.movies.ui.components.expressive.MoviesButtonStyle
import com.kien.movies.ui.screens.settings.TenFootTypography
import com.kien.movies.ui.theme.metaFloor
import com.kien.movies.ui.components.SkeletonBar
import com.kien.movies.ui.components.rememberShimmerBrush
import com.kien.movies.ui.screens.detail.requestFocusAfterFrames
import kotlinx.coroutines.delay

@Composable
fun AuthQrSignInScreen(
    onBackPress: () -> Unit = {},
    onContinue: (() -> Unit)? = null,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val fullAccount = uiState.authState as? AuthState.FullAccount
    val isSignedIn = fullAccount != null
    val isOnboardingMode = onContinue != null
    val useEmailLogin = viewModel.usesEmailPasswordLogin
    val useQrLogin = !useEmailLogin
    val isApproved = remember(uiState.qrLoginStatus) {
        uiState.qrLoginStatus?.contains("approved", ignoreCase = true) == true
    }
    var onboardingTransitionHandled by remember(isOnboardingMode) { mutableStateOf(false) }
    var exitRequested by remember { mutableStateOf(false) }
    var showSignOutConfirmation by remember { mutableStateOf(false) }
    val loginFocusRequester = remember { FocusRequester() }

    fun leaveAuthScreen() {
        exitRequested = true
        viewModel.clearQrLoginSession()
        onBackPress()
    }

    fun continueFromAuthScreen() {
        exitRequested = true
        if (onContinue != null && !isSignedIn) {
            viewModel.signOut()
        }
        viewModel.clearQrLoginSession()
        if (onContinue != null) {
            onContinue()
        } else {
            onBackPress()
        }
    }

    BackHandler {
        leaveAuthScreen()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearQrLoginSession()
        }
    }

    LaunchedEffect(uiState.authState, isSignedIn, uiState.qrLoginCode, uiState.isLoading, uiState.error, exitRequested) {
        if (
            useQrLogin &&
            !exitRequested &&
            uiState.authState !is AuthState.Loading &&
            !isSignedIn &&
            uiState.qrLoginCode.isNullOrBlank() &&
            uiState.error.isNullOrBlank() &&
            !uiState.isLoading
        ) {
            viewModel.startQrLogin()
        }
    }

    LaunchedEffect(isSignedIn) {
        if (useQrLogin && isSignedIn && !uiState.qrLoginCode.isNullOrBlank()) {
            viewModel.clearQrLoginSession()
        }
    }

    LaunchedEffect(isApproved, uiState.isLoading) {
        if (useQrLogin && isApproved && !uiState.isLoading) {
            viewModel.exchangeQrLogin()
        }
    }

    LaunchedEffect(isOnboardingMode, isSignedIn) {
        if (!isOnboardingMode || onboardingTransitionHandled) return@LaunchedEffect
        if (isSignedIn) {
            onboardingTransitionHandled = true
            exitRequested = true
            viewModel.clearQrLoginSession()
            onContinue.invoke()
        }
    }

    val paneBorder = accountOutlineColor()
    val screenBase = accountScreenBase()
    val gradientAccent = accountGradientAccent()

    TenFootTypography {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBase)
            .accountGradientBackground(gradientAccent, screenBase)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            AuthQrBrandPanel(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 56.dp, end = 56.dp),
                isSignedIn = isSignedIn,
                fullAccount = fullAccount,
                useEmailLogin = useEmailLogin
            )

            AuthQrLoginPane(
                modifier = Modifier
                    .width(460.dp)
                    .fillMaxHeight()
                    .background(accountPaneColor())
                    .drawBehind {
                        drawLine(
                            color = paneBorder,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    },
                uiState = uiState,
                isSignedIn = isSignedIn,
                isOnboardingMode = isOnboardingMode,
                useEmailLogin = useEmailLogin,
                onSignIn = viewModel::signIn,
                onRefreshOrSignOut = {
                    if (isSignedIn) {
                        showSignOutConfirmation = true
                    } else {
                        viewModel.startQrLogin()
                    }
                },
                onBackOrContinue = {
                    if (isOnboardingMode) {
                        continueFromAuthScreen()
                    } else {
                        leaveAuthScreen()
                    }
                },
                initialFocusRequester = loginFocusRequester
            )
        }

        if (BuildConfig.FEATURE_CUSTOM_SERVER_CONNECTIONS_ENABLED) {
            ServerOptionsMenuHost(
                viewModel = hiltViewModel(),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(28.dp)
            )
        }
    }

    LaunchedEffect(Unit) {
        loginFocusRequester.requestFocusAfterFrames(frames = 3)
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
}

@Composable
private fun AuthQrBrandPanel(
    modifier: Modifier,
    isSignedIn: Boolean,
    fullAccount: AuthState.FullAccount?,
    useEmailLogin: Boolean
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        BrandWordmark(
            contentDescription = stringResource(R.string.cd_nuvio),
            modifier = Modifier.height(60.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.auth_qr_tagline),
            modifier = Modifier.widthIn(max = 440.dp),
            style = MaterialTheme.typography.displayLarge.copy(
                color = accountTextPrimary(),
                fontSize = 40.sp,
                lineHeight = 45.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
        if (isSignedIn || useEmailLogin) {
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = if (isSignedIn) {
                    stringResource(R.string.auth_qr_connected)
                } else {
                    stringResource(R.string.auth_email_hint)
                },
                modifier = Modifier.widthIn(max = 400.dp),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = accountTextSecondary(),
                    fontSize = 17.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Normal
                )
            )
        }
        if (isSignedIn && fullAccount != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = fullAccount.email,
                style = MaterialTheme.typography.titleMedium,
                color = accountSuccessColor()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = fullAccount.userId,
                style = MaterialTheme.typography.bodySmall,
                color = accountTextSecondary()
            )
        }
    }
}

@Composable
private fun AuthQrLoginPane(
    modifier: Modifier,
    uiState: AccountUiState,
    isSignedIn: Boolean,
    isOnboardingMode: Boolean,
    useEmailLogin: Boolean,
    onSignIn: (String, String) -> Unit,
    onRefreshOrSignOut: () -> Unit,
    onBackOrContinue: () -> Unit,
    initialFocusRequester: FocusRequester
) {
    val focusEmail = useEmailLogin && !isSignedIn
    val focusMainAction = !focusEmail && !uiState.isLoading
    Column(
        modifier = modifier.padding(horizontal = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isSignedIn) {
                stringResource(R.string.auth_qr_synced_data)
            } else if (useEmailLogin) {
                stringResource(R.string.auth_email_instruction)
            } else {
                stringResource(R.string.auth_qr_scan_instruction)
            },
            style = MaterialTheme.typography.bodyLarge.copy(
                color = accountTextSecondary(),
                fontSize = 15.sp,
                lineHeight = 21.sp
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))

        if (isSignedIn && !isOnboardingMode) {
            AccountConnectedStatsStrip(
                stats = uiState.connectedStats,
                isLoading = uiState.isStatsLoading
            )
        } else if (isSignedIn && isOnboardingMode) {
            StatusPill(
                text = stringResource(R.string.auth_qr_finishing),
                containerColor = accountControlColor(),
                contentColor = accountTextSecondary()
            )
        } else if (useEmailLogin) {
            AuthEmailLoginForm(
                uiState = uiState,
                onSignIn = onSignIn,
                initialFocusRequester = initialFocusRequester
            )
        } else {
            AuthQrCodeBlock(uiState = uiState)
        }

        Spacer(modifier = Modifier.height(28.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSignedIn || !useEmailLogin) {
                MoviesButton(
                    onClick = onRefreshOrSignOut,
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .then(
                            if (focusMainAction) {
                                Modifier.focusRequester(initialFocusRequester)
                            } else {
                                Modifier
                            }
                        ),
                    style = if (isSignedIn) {
                        MoviesButtonStyle.Destructive
                    } else {
                        MoviesButtonStyle.Secondary
                    }
                ) {
                    Text(
                        when {
                            isSignedIn -> stringResource(R.string.account_sign_out)
                            uiState.isLoading -> stringResource(R.string.auth_qr_please_wait)
                            else -> stringResource(R.string.auth_qr_refresh)
                        }
                    )
                }
            }
            MoviesButton(
                onClick = onBackOrContinue,
                modifier = Modifier
                    .then(
                        if (!focusEmail && !focusMainAction) {
                            Modifier.focusRequester(initialFocusRequester)
                        } else {
                            Modifier
                        }
                    )
            ) {
                Text(
                    if (isOnboardingMode) {
                        if (isSignedIn) stringResource(R.string.auth_qr_continue) else stringResource(R.string.auth_qr_continue_without_account)
                    } else {
                        stringResource(R.string.auth_qr_back)
                    }
                )
            }
        }
    }
}

@Composable
private fun AuthEmailLoginForm(
    uiState: AccountUiState,
    onSignIn: (String, String) -> Unit,
    initialFocusRequester: FocusRequester
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val canSignIn = email.isNotBlank() && password.isNotBlank() && !uiState.isLoading
    val submit = {
        if (canSignIn) {
            onSignIn(email.trim(), password)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.md)
    ) {
        InputField(
            value = email,
            onValueChange = { email = it },
            placeholder = stringResource(R.string.auth_email_placeholder),
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            modifier = Modifier.focusRequester(initialFocusRequester)
        )
        InputField(
            value = password,
            onValueChange = { password = it },
            placeholder = stringResource(R.string.auth_password_placeholder),
            keyboardType = KeyboardType.Password,
            isPassword = true,
            imeAction = ImeAction.Done,
            onImeAction = submit
        )
        MoviesButton(
            onClick = submit,
            enabled = canSignIn,
            modifier = Modifier.fillMaxWidth(),
            style = MoviesButtonStyle.Primary
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (uiState.isLoading) {
                        stringResource(R.string.auth_email_signing_in)
                    } else {
                        stringResource(R.string.auth_email_sign_in)
                    },
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
        uiState.error?.takeIf { it.isNotBlank() }?.let { error ->
            StatusPill(
                text = error,
                containerColor = accountErrorContainer(),
                contentColor = accountErrorContent()
            )
        }
    }
}

@Composable
private fun AuthQrCodeBlock(
    uiState: AccountUiState
) {
    val qrBitmap = remember(uiState.qrLoginBitmap) { uiState.qrLoginBitmap?.asImageBitmap() }
    if (qrBitmap != null) {
        Image(
            bitmap = qrBitmap,
            contentDescription = stringResource(R.string.cd_qr_login),
            modifier = Modifier
                .size(206.dp)
                .background(Color.White, M3eShapes.panel)
                .padding(8.dp),
            contentScale = ContentScale.Fit
        )
    } else {
        Box(
            modifier = Modifier
                .size(206.dp)
                .background(
                    accountControlColor(),
                    M3eShapes.panel
                )
                .border(
                    1.dp,
                    accountOutlineColor(),
                    M3eShapes.panel
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (uiState.isLoading) stringResource(R.string.auth_qr_generating) else stringResource(R.string.auth_qr_unavailable),
                color = accountTextSecondary(),
                textAlign = TextAlign.Center
            )
        }
    }

    AuthQrManualCodeDetails(
        verificationUri = uiState.qrLoginVerificationUri,
        qrLoginCode = uiState.qrLoginUserCode ?: uiState.qrLoginCode,
        expiresAtMillis = uiState.qrLoginExpiresAtMillis,
        isLoading = uiState.isLoading
    )


    val statusText = uiState.error ?: uiState.qrLoginStatus
    if (!statusText.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(14.dp))
        if (uiState.error != null) {
            StatusPill(
                text = statusText,
                containerColor = accountErrorContainer(),
                contentColor = accountErrorContent()
            )
        } else {
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = accountTextSecondary(),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AuthQrManualCodeDetails(
    verificationUri: String?,
    qrLoginCode: String?,
    expiresAtMillis: Long?,
    isLoading: Boolean
) {
    val displayUri = verificationUri?.takeIf { it.isNotBlank() }
    val displayCode = qrLoginCode?.takeIf { it.isNotBlank() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (displayUri != null && displayCode != null) {
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.auth_qr_manual_instruction, displayVerificationUri(displayUri)),
                style = MaterialTheme.typography.bodyMedium,
                color = accountTextSecondary(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = formatDeviceLoginCode(displayCode),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = if (displayCode.length == 6) 24.sp else 14.sp,
                    letterSpacing = if (displayCode.length == 6) 3.sp else 0.sp
                ),
                color = accountTextPrimary(),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            if (expiresAtMillis != null) {
                Spacer(modifier = Modifier.height(6.dp))
                AuthQrExpiresText(expiresAtMillis = expiresAtMillis)
            }
        } else if (isLoading) {
            val shimmerBrush = rememberShimmerBrush(backdropAware = true)
            Spacer(modifier = Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                contentAlignment = Alignment.Center
            ) {
                SkeletonBar(width = 238.dp, height = 12.dp, brush = shimmerBrush, cornerRadius = 6.dp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                contentAlignment = Alignment.Center
            ) {
                SkeletonBar(width = 116.dp, height = 22.dp, brush = shimmerBrush, cornerRadius = 8.dp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp),
                contentAlignment = Alignment.Center
            ) {
                SkeletonBar(width = 84.dp, height = 8.dp, brush = shimmerBrush, cornerRadius = 4.dp)
            }
        }
    }
}

// Owns the 1 s ticker so only this line recomposes while the code counts down.
@Composable
private fun AuthQrExpiresText(expiresAtMillis: Long) {
    val nowMillis by produceState(initialValue = System.currentTimeMillis(), key1 = expiresAtMillis) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val remainingMillis = (expiresAtMillis - nowMillis).coerceAtLeast(0L)
    Text(
        text = stringResource(R.string.auth_qr_expires, formatDuration(remainingMillis)),
        style = MaterialTheme.typography.bodySmall,
        color = accountTextSecondary()
    )
}

private fun formatDeviceLoginCode(value: String): String =
    if (value.length == 6) "${value.take(3)}-${value.drop(3)}" else value

private fun displayVerificationUri(value: String): String = value
    .removePrefix("https://")
    .removePrefix("http://")
    .trimEnd('/')

@Composable
private fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor, M3eShapes.panel)
            .padding(horizontal = MoviesTheme.spacing.md, vertical = 10.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.wrapContentHeight()
        )
    }
}

@Composable
private fun AccountConnectedStatsStrip(
    stats: AccountConnectedStats?,
    isLoading: Boolean
) {
    val values = if (isLoading) {
        listOf("...", "...", "...", "...")
    } else {
        listOf(
            (stats?.addons ?: 0).toString(),
            (stats?.plugins ?: 0).toString(),
            (stats?.library ?: 0).toString(),
            (stats?.watchProgress ?: 0).toString()
        )
    }
    val labels = listOf(
        stringResource(R.string.account_stat_addons),
        stringResource(R.string.account_stat_plugins),
        stringResource(R.string.account_stat_library),
        stringResource(R.string.account_stat_progress)
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MoviesTheme.spacing.hairline)
                .background(MoviesTheme.colors.Border.copy(alpha = 0.8f))
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(values.size) { index ->
                AccountStatItem(
                    value = values[index],
                    label = labels[index],
                    modifier = Modifier.weight(1f)
                )
                if (index != values.lastIndex) {
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .width(MoviesTheme.spacing.hairline)
                            .background(MoviesTheme.colors.Border.copy(alpha = 0.75f))
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MoviesTheme.spacing.hairline)
                .background(MoviesTheme.colors.Border.copy(alpha = 0.8f))
        )
    }
}

@Composable
private fun AccountStatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MoviesTheme.colors.TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(MoviesTheme.spacing.xxs))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MoviesTheme.colors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
