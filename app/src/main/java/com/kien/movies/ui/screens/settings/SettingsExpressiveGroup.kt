@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kien.movies.ui.theme.NuvioTheme

private val GroupRowGap = 6.dp
private val HeaderInset = 4.dp

/**
 * A group of settings rows in the tvOS Settings manner: a section header in white at 64% over
 * frosted rows, with no card or outline around them. The rows carry their own fill.
 */
@Composable
internal fun SettingsExpressiveGroup(
    modifier: Modifier,
    title: String?,
    subtitle: String?,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GroupRowGap)
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = NuvioTheme.colors.TextSecondary,
                modifier = Modifier.padding(start = HeaderInset, top = HeaderInset)
            )
        }
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = NuvioTheme.colors.TextTertiary,
                modifier = Modifier.padding(start = HeaderInset)
            )
        }
        content()
    }
}
