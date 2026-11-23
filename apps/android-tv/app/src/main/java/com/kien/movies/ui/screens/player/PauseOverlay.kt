package com.kien.movies.ui.screens.player

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kien.movies.R
import com.kien.movies.domain.model.MetaCastMember
import com.kien.movies.ui.components.expressive.M3eShapes
import com.kien.movies.ui.components.expressive.MoviesCard
import com.kien.movies.ui.theme.MoviesTheme
import com.kien.movies.ui.util.contentTextDirection
import com.kien.movies.ui.util.localizeEpisodeTitle
import com.kien.movies.ui.util.toAbsoluteAlignment
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun PauseOverlay(
    visible: Boolean,
    onClose: () -> Unit,
    title: String,
    logo: String?,
    episodeTitle: String?,
    season: Int?,
    episode: Int?,
    year: String?,
    type: String?,
    description: String?,
    cast: List<MetaCastMember>,
    modifier: Modifier = Modifier,
    showClock: Boolean = true
) {
    var selectedCastMember by remember { mutableStateOf<MetaCastMember?>(null) }

    PlayerOverlayScaffold(
        visible = visible,
        onDismiss = onClose,
        modifier = modifier,
        captureKeys = false,
        dismissOnBackgroundClick = true,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = MoviesTheme.spacing.huge,
            end = MoviesTheme.spacing.huge,
            top = 40.dp,
            bottom = 120.dp
        ),
        topEndContent = {
            if (showClock) {
                PauseOverlayClock()
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            if (selectedCastMember != null) {
                CastDetailView(
                    member = selectedCastMember!!,
                    onBack = { selectedCastMember = null }
                )
            } else {
                PauseMetadataView(
                    title = title,
                    logo = logo,
                    episodeTitle = episodeTitle,
                    season = season,
                    episode = episode,
                    year = year,
                    type = type,
                    description = description,
                    cast = cast,
                    onCastSelected = { selectedCastMember = it }
                )
            }
        }
    }
}

@Composable
private fun PauseOverlayClock(modifier: Modifier = Modifier) {
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current
    val formatter = remember(context) { DateFormat.getTimeFormat(context) }

    LaunchedEffect(Unit) {
        while (true) {
            val current = System.currentTimeMillis()
            nowMillis = current
            val delayMs = (60_000L - (current % 60_000L)).coerceAtLeast(1_000L)
            delay(delayMs)
        }
    }

    Text(
        text = formatter.format(Date(nowMillis)),
        style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Normal,
            fontSize = 34.sp
        ),
        color = Color.White.copy(alpha = 0.95f),
        modifier = modifier
    )
}

@Composable
private fun PauseMetadataView(
    title: String,
    logo: String?,
    episodeTitle: String?,
    season: Int?,
    episode: Int?,
    year: String?,
    type: String?,
    description: String?,
    cast: List<MetaCastMember>,
    onCastSelected: (MetaCastMember) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.pause_you_are_watching),
                style = MaterialTheme.typography.bodyLarge,
                color = MoviesTheme.colors.TextTertiary
            )

            Spacer(modifier = Modifier.height(MoviesTheme.spacing.md))

            if (!logo.isNullOrBlank()) {
                var logoFailed by remember(logo) { mutableStateOf(false) }
                if (!logoFailed) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(logo)
                            .memoryCacheKey(logo)
                            .crossfade(true)
                            .build(),
                        contentDescription = title,
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.BottomStart,
                        modifier = Modifier.height(96.dp),
                        onError = { logoFailed = true }
                    )
                } else {
                    Text(
                        text = title,
                        modifier = Modifier.align(
                            title.contentTextDirection().toAbsoluteAlignment()
                        ),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = title,
                    modifier = Modifier.align(title.contentTextDirection().toAbsoluteAlignment()),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!year.isNullOrBlank()) {
                val episodeLabel = if (type in listOf("series", "tv") && season != null &&
                    episode != null
                ) {
                    " • " + stringResource(R.string.season_episode_format, season, episode)
                } else {
                    ""
                }

                Text(
                    text = "$year$episodeLabel",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MoviesTheme.colors.TextSecondary,
                    modifier = Modifier.padding(top = MoviesTheme.spacing.sm)
                )
            }

            if (!episodeTitle.isNullOrBlank()) {
                val context = LocalContext.current
                Text(
                    text = episodeTitle.localizeEpisodeTitle(context),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = MoviesTheme.spacing.md)
                )
            }

            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDirection = description.contentTextDirection()
                    ),
                    color = MoviesTheme.colors.TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = MoviesTheme.spacing.lg)
                )
            }

            if (cast.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.pause_cast_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = MoviesTheme.colors.TextTertiary
                )

                Spacer(modifier = Modifier.height(MoviesTheme.spacing.md))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    content = {
                        items(cast.take(8)) { member ->
                            CastChip(member = member, onClick = { onCastSelected(member) })
                        }
                    }
                )
            }
        }
    }
}

/** A cast pill: frosted at rest, white with black text on focus. */
@Composable
private fun CastChip(member: MetaCastMember, onClick: () -> Unit) {
    MoviesCard(
        onClick = onClick,
        modifier = Modifier.heightIn(min = CastChipMinHeight),
        shape = M3eShapes.capsule
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = CastChipMinHeight)
                .padding(horizontal = MoviesTheme.spacing.lg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = member.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private val CastChipMinHeight = 48.dp

@Composable
private fun CastDetailView(member: MetaCastMember, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onBack)
                .semantics { role = Role.Button }
                .padding(bottom = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = MoviesTheme.colors.TextSecondary,
                modifier = Modifier.size(MoviesTheme.spacing.xl)
            )
            Spacer(modifier = Modifier.width(MoviesTheme.spacing.md))
            Text(
                text = stringResource(R.string.pause_back_to_details),
                style = MaterialTheme.typography.bodyMedium,
                color = MoviesTheme.colors.TextSecondary
            )
        }

        Row(
            verticalAlignment = Alignment.Top
        ) {
            if (!member.photo.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(member.photo)
                        .crossfade(true)
                        .build(),
                    contentDescription = member.name,
                    modifier = Modifier
                        .size(width = 160.dp, height = 240.dp)
                        .clip(RoundedCornerShape(MoviesTheme.radii.xl)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(28.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!member.character.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.pause_as_character, member.character ?: ""),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MoviesTheme.colors.TextSecondary,
                        modifier = Modifier.padding(top = MoviesTheme.spacing.sm),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
