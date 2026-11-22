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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.ui.res.stringResource
import com.kien.movies.R
import com.kien.movies.ui.screens.settings.TenFootTypography
import com.kien.movies.ui.screens.settings.expressiveControl
import com.kien.movies.ui.screens.settings.expressiveControlGrow
import com.kien.movies.ui.screens.settings.expressivePill
import com.kien.movies.ui.screens.settings.expressiveRingless
import com.kien.movies.ui.theme.tvControlHeight
import com.kien.movies.ui.screens.settings.expressivePanelShape

@Composable
fun AuthSignInScreen(
    onBackPress: () -> Unit = {},
    onNavigateToQrSignIn: () -> Unit = {},
    onSuccess: () -> Unit = {}
) = TenFootTypography {
    BackHandler { onBackPress() }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .background(
                    color = MoviesTheme.colors.BackgroundElevated,
                    shape = expressivePanelShape(RoundedCornerShape(20.dp))
                )
                .padding(MoviesTheme.spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.auth_signin_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MoviesTheme.colors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.auth_signin_tv_disabled),
                style = MaterialTheme.typography.bodyMedium,
                color = MoviesTheme.colors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(22.dp))
            Button(
                onClick = onNavigateToQrSignIn,
                colors = ButtonDefaults.colors(
                    containerColor = MoviesTheme.colors.Secondary,
                    focusedContainerColor = MoviesTheme.colors.SecondaryVariant,
                    contentColor = MoviesTheme.colors.OnSecondary,
                    focusedContentColor = MoviesTheme.colors.OnSecondaryVariant
                ).expressiveControl(primary = true),
                shape = ButtonDefaults.shape(RoundedCornerShape(50)).expressivePill(),
                border = ButtonDefaults.border().expressiveRingless(),
                scale = ButtonDefaults.scale().expressiveControlGrow(),
                modifier = Modifier.fillMaxWidth().tvControlHeight()
            ) {
                Text(
                    text = stringResource(R.string.auth_signin_qr_btn),
                    modifier = Modifier.padding(vertical = MoviesTheme.spacing.xs),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
