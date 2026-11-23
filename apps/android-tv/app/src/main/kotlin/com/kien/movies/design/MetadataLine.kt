package com.kien.movies.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text

/** Facts, age rating and IMDb rating on one line (tvOS MetadataLine.swift). */
@Composable
fun MetadataLine(
    facts: List<String>,
    modifier: Modifier = Modifier,
    ageRating: String? = null,
    imdbRating: String? = null
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (facts.isNotEmpty()) FactLine(facts)
        if (ageRating != null) RatingTag(ageRating)
        if (imdbRating != null) ImdbBadge(imdbRating)
    }
}

/** Facts separated by thin dividers. */
@Composable
fun FactLine(facts: List<String>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        facts.forEachIndexed { index, fact ->
            if (index > 0) FactDivider()
            Text(text = fact, maxLines = 1)
        }
    }
}

@Composable
private fun FactDivider() {
    Box(
        modifier = Modifier
            .padding(vertical = 3.dp)
            .width(1.dp)
            .fillMaxHeight()
            .background(Color.White.copy(alpha = 0.35f))
    )
}

/** The yellow IMDb tag and the rating. */
@Composable
fun ImdbBadge(rating: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "IMDb",
            style = TextStyles.badge.copy(fontWeight = FontWeight.Black),
            color = Color.Black,
            modifier = Modifier
                .background(Palette.imdbYellow, badgeShape)
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )
        Text(text = rating, fontWeight = FontWeight.SemiBold, color = Palette.textPrimary)
    }
}

@Composable
private fun RatingTag(text: String) {
    Text(
        text = text,
        style = TextStyles.badge,
        modifier = Modifier
            .border(1.dp, Color.White.copy(alpha = 0.55f), badgeShape)
            .padding(horizontal = 4.dp, vertical = 1.dp)
    )
}

private val badgeShape = RoundedCornerShape(3.dp)
