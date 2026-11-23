package com.kien.movies.shell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.kien.movies.search.TvColors
import com.kien.movies.search.TvType

/**
 * The empty state (docs/tv-look-spec.md section 3, 61-library.png): a Headline bold title and one
 * line of Caption 2 in secondary, centred, on the Search and Library slate. Library and Settings
 * use it until they are built, and the app while its runtime starts.
 *
 * The text block holds focus with no focus look, so the remote is never stranded (AC N5). tvOS
 * leaves focus on the nav pill here; with the floating pill shell this block could hand focus to
 * the pill instead.
 */
@Composable
fun PlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier,
    detail: String = COMING_LATER,
    onClick: () -> Unit = {}
) {
    WindowBackground(TvColors.searchBackground)
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .offset(y = BLOCK_RAISE)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
                .testTag(PLACEHOLDER_TAG),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TITLE_TO_DETAIL)
        ) {
            Text(text = title, style = TvType.headline, color = TvColors.textPrimary, textAlign = TextAlign.Center)
            Text(text = detail, style = TvType.caption2, color = TvColors.textSecondary, textAlign = TextAlign.Center)
        }
    }
}

const val COMING_LATER = "Coming in a later slice"
const val PLACEHOLDER_TAG = "placeholder-focus"

// Measured on 61-library.png: title cap top y 212 dp, detail cap top y 259 dp, so the block sits
// above the screen centre and the baselines are 42 dp apart.
private val BLOCK_RAISE = (-31).dp
private val TITLE_TO_DETAIL = 25.dp
