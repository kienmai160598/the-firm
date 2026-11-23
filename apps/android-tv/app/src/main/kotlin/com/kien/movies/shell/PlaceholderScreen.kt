package com.kien.movies.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kien.movies.design.GlassStyle
import com.kien.movies.design.Palette
import com.kien.movies.design.Spacing

/**
 * A screen that is not built yet (tvOS PlaceholderView.swift): Library and Settings, and every
 * feature stub until its slice lands. Holds focus on one control (AC N5).
 */
@Composable
fun PlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier,
    detail: String = COMING_LATER,
    onClick: () -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.textGapLarge)
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineSmall, color = Palette.textPrimary)
            Button(
                onClick = onClick,
                colors = GlassStyle.buttonColors(),
                scale = GlassStyle.buttonScale(),
                modifier = Modifier.testTag(PLACEHOLDER_TAG)
            ) {
                Text(text = detail)
            }
        }
    }
}

const val COMING_LATER = "Coming in a later slice"
const val PLACEHOLDER_TAG = "placeholder-focus"
