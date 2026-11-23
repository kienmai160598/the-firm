package com.kien.movies.home

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.tv.material3.Text
import com.kien.movies.design.Palette
import com.kien.movies.design.Spacing
import com.kien.movies.design.TextStyles

/**
 * Home with nothing to show once loading ended (tvOS HomeStatusView.swift): the bridge's error,
 * or "No catalogs yet". It sits in the hero slot, so the hero's button stays focused through the
 * change from loading (AC H3); in this state the button opens Addons.
 */
@Composable
fun HomeStatus(errorMessage: String?, modifier: Modifier = Modifier) {
    Text(
        text = errorMessage ?: NO_CATALOGS,
        style = TextStyles.body,
        color = Palette.textSecondary,
        textAlign = TextAlign.Center,
        modifier = modifier.widthIn(max = Spacing.heroTextWidth)
    )
}

const val NO_CATALOGS = "No catalogs yet"
const val OPEN_ADDONS = "Open Addons"
