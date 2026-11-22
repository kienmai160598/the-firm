package com.kien.movies.ui.components

import com.kien.movies.ui.theme.MoviesTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EmptyScreenState(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    height: Dp = 400.dp
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .background(MoviesTheme.colors.FocusBackground, CircleShape)
                    .padding(MoviesTheme.spacing.lg),
                tint = MoviesTheme.colors.FocusContent
            )
            Spacer(modifier = Modifier.height(MoviesTheme.spacing.xl))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MoviesTheme.colors.TextPrimary
        )

        if (subtitle != null) {
            Spacer(modifier = Modifier.height(MoviesTheme.spacing.sm))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MoviesTheme.colors.TextSecondary
            )
        }
    }
}
