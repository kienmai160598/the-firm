package com.nuvio.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.ui.theme.NuvioTheme

/**
 * A tonal badge beside a stream name, such as "Too heavy for this device". Its container is
 * the highest surface tone, which stays distinct from a stream card both at rest and focused.
 */
@Composable
internal fun StreamLabelBadge(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .background(NuvioTheme.colors.SurfaceVariant, CircleShape)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelMedium,
        color = NuvioTheme.colors.Secondary,
        maxLines = 1
    )
}
