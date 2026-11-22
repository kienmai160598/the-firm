package com.kien.movies.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.domain.model.MemberTier
import com.kien.movies.ui.theme.MoviesTheme

@Composable
internal fun DebugMemberTierCard(
    selectedTier: MemberTier?,
    onTierSelected: (MemberTier?) -> Unit
) {
    val options = listOf(
        null to stringResource(R.string.debug_member_tier_none),
        MemberTier.SUPPORTER to stringResource(R.string.debug_member_tier_one),
        MemberTier.SUPPORTER_PLUS to stringResource(R.string.debug_member_tier_two)
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.md)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.xs)) {
            Text(
                text = stringResource(R.string.debug_member_tier_title),
                style = MaterialTheme.typography.titleMedium,
                color = MoviesTheme.colors.TextPrimary
            )
            Text(
                text = stringResource(R.string.debug_member_tier_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MoviesTheme.colors.TextSecondary
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm)
        ) {
            options.forEach { (tier, label) ->
                val selected = tier == selectedTier
                val shape = RoundedCornerShape(MoviesTheme.radii.sm)
                Card(
                    onClick = { onTierSelected(tier) },
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            role = Role.RadioButton
                            this.selected = selected
                        },
                    colors = CardDefaults.colors(
                        containerColor = if (selected) {
                            MoviesTheme.colors.Secondary.copy(alpha = 0.2f)
                        } else {
                            MoviesTheme.colors.BackgroundCard
                        },
                        focusedContainerColor = MoviesTheme.colors.FocusBackground
                    ),
                    border = CardDefaults.border(
                        border = if (selected) {
                            Border(
                                border = MoviesTheme.focusRing.border(MoviesTheme.spacing.hairline),
                                shape = shape
                            )
                        } else {
                            Border.None
                        },
                        focusedBorder = Border(
                            border = MoviesTheme.focusRing.border(MoviesTheme.spacing.xxs),
                            shape = shape
                        )
                    ).expressiveSelection(selected),
                    shape = CardDefaults.shape(shape).expressiveCard(),
                    scale = CardDefaults.scale(focusedScale = 1.04f).expressiveCardGrow()
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MoviesTheme.colors.TextPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MoviesTheme.spacing.md, vertical = MoviesTheme.spacing.lg),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
