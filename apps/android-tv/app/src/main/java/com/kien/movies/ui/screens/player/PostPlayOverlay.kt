@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.screens.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusRequester.Companion.Cancel
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.transformations
import com.kien.movies.R
import com.kien.movies.ui.components.expressive.M3eShapes
import com.kien.movies.ui.components.expressive.MoviesCard
import com.kien.movies.ui.components.expressive.MoviesSurface
import com.kien.movies.ui.theme.MoviesTheme
import com.kien.movies.ui.util.BlurTransformation
import com.kien.movies.ui.util.localizeEpisodeTitle

@Composable
fun PostPlayOverlay(
    mode: PostPlayMode?,
    controlsVisible: Boolean,
    blurUnwatchedEpisodes: Boolean,
    nextEpisodeFocusRequester: FocusRequester,
    progressBarFocusRequester: FocusRequester?,
    leftFocusRequester: FocusRequester?,
    onPlayNext: () -> Unit,
    onContinueStillWatching: () -> Unit,
    onDismissStillWatching: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAutoPlay = mode is PostPlayMode.AutoPlay
    val isAutoPlayPlayable = (mode as? PostPlayMode.AutoPlay)?.nextEpisode?.hasAired == true

    var placedFocused by remember(isAutoPlay, controlsVisible) { mutableStateOf(false) }

    val transition = updateTransition(targetState = mode, label = "PostPlayMode")

    transition.AnimatedVisibility(
        visible = { it != null },
        enter = slideInHorizontally(
            animationSpec = PlayerMotion.spatial(),
            initialOffsetX = { it / 2 }
        ) + fadeIn(animationSpec = PlayerMotion.effects()),
        exit = slideOutHorizontally(
            animationSpec = PlayerMotion.spatial(),
            targetOffsetX = { it / 2 }
        ) + fadeOut(animationSpec = PlayerMotion.effects()),
        modifier = modifier
    ) {
        val onCardClick: () -> Unit = when {
            isAutoPlay && isAutoPlayPlayable -> onPlayNext
            mode is PostPlayMode.StillWatching -> onContinueStillWatching
            else -> ({})
        }
        MoviesSurface(
            onClick = onCardClick,
            shape = PlayerSurfaceStyle.cardShape(),
            resting = PlayerSurfaceStyle.floatingContainer(),
            modifier = Modifier
                .width(PlayerTenFoot.atLeast(420.dp, PostPlayCardWidth))
                .focusRequester(nextEpisodeFocusRequester)
                .onPlaced {
                    if (isAutoPlay && !controlsVisible && !placedFocused) {
                        placedFocused = true
                        runCatching { nextEpisodeFocusRequester.requestFocus() }
                    }
                }
                .then(
                    if (progressBarFocusRequester != null || leftFocusRequester != null) {
                        Modifier.focusProperties {
                            progressBarFocusRequester?.let { down = it }
                            leftFocusRequester?.let { left = it }
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            transition.AnimatedContent(
                transitionSpec = {
                    fadeIn(PlayerMotion.effects()) togetherWith
                        fadeOut(PlayerMotion.effects())
                },
                contentKey = { it?.let { current -> current::class } }
            ) { current ->
                when (current) {
                    is PostPlayMode.AutoPlay -> AutoPlayBody(
                        mode = current,
                        blurUnwatchedEpisodes = blurUnwatchedEpisodes
                    )

                    is PostPlayMode.StillWatching -> StillWatchingBody(
                        mode = current,
                        blurUnwatchedEpisodes = blurUnwatchedEpisodes,
                        onContinue = onContinueStillWatching,
                        onDismiss = onDismissStillWatching
                    )

                    null -> Unit
                }
            }
        }
    }
}

@Composable
private fun AutoPlayBody(mode: PostPlayMode.AutoPlay, blurUnwatchedEpisodes: Boolean) {
    val nextEpisode = mode.nextEpisode
    val isPlayable = nextEpisode.hasAired
    Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NextEpisodeThumbnail(
            thumbnail = nextEpisode.thumbnail,
            contentDescription = stringResource(R.string.cd_next_episode_thumbnail),
            blurred = blurUnwatchedEpisodes
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = stringResource(R.string.next_episode_label),
                color = postPlayInk().copy(alpha = 0.8f),
                fontSize = PostPlayMetaSize,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(MoviesTheme.spacing.xxs))
            Text(
                text = nextEpisodeDisplayLabel(nextEpisode),
                color = postPlayInk(),
                fontSize = PostPlayTitleSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold
            )
            val statusText = when {
                !isPlayable && !nextEpisode.unairedMessage.isNullOrBlank() -> nextEpisode.unairedMessage

                mode.searching -> stringResource(R.string.next_episode_finding_source)

                !mode.sourceName.isNullOrBlank() && mode.countdownSec != null ->
                    stringResource(
                        R.string.next_episode_playing_via,
                        mode.sourceName,
                        mode.countdownSec
                    )

                else -> null
            }
            if (statusText != null) {
                Spacer(modifier = Modifier.height(MoviesTheme.spacing.xxs))
                NextEpisodeStatusLine(text = statusText)
            }
        }

        Row(
            modifier = Modifier
                .padding(start = MoviesTheme.spacing.sm)
                .clip(CircleShape)
                .border(
                    BorderStroke(MoviesTheme.spacing.hairline, postPlayInk().copy(alpha = 0.2f)),
                    CircleShape
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isPlayable) postPlayInk() else postPlayInk().copy(alpha = 0.65f),
                modifier = Modifier.size(PlayerTenFoot.atLeast(14.dp, 20.dp))
            )
            Text(
                text = if (isPlayable) {
                    stringResource(R.string.next_episode_play)
                } else {
                    stringResource(R.string.next_episode_unaired)
                },
                color = if (isPlayable) postPlayInk() else postPlayInk().copy(alpha = 0.72f),
                fontSize = PostPlayLabelSize,
                modifier = Modifier.padding(start = 3.dp)
            )
        }
    }
}

@Composable
private fun StillWatchingBody(
    mode: PostPlayMode.StillWatching,
    blurUnwatchedEpisodes: Boolean,
    onContinue: () -> Unit,
    onDismiss: () -> Unit
) {
    val continueFocusRequester = remember { FocusRequester() }
    val exitFocusRequester = remember { FocusRequester() }
    var continueFocused by remember { mutableStateOf(false) }

    val nextEpisode = mode.nextEpisode
    Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (nextEpisode.thumbnail != null) {
            NextEpisodeThumbnail(
                thumbnail = nextEpisode.thumbnail,
                contentDescription = stringResource(R.string.cd_next_episode_thumbnail),
                blurred = blurUnwatchedEpisodes
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.still_watching_title),
                color = postPlayInk().copy(alpha = 0.8f),
                fontSize = PostPlayMetaSize,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(MoviesTheme.spacing.xxs))
            Text(
                text = nextEpisodeDisplayLabel(nextEpisode),
                color = postPlayInk(),
                fontSize = PostPlayTitleSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold
            )
            if (mode.countdownSec != null) {
                Spacer(modifier = Modifier.height(MoviesTheme.spacing.xxs))
                NextEpisodeStatusLine(
                    text = stringResource(R.string.still_watching_countdown, mode.countdownSec)
                )
            }
        }
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        Row(
            modifier = Modifier.padding(start = MoviesTheme.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PostPlayPillButton(
                icon = Icons.Default.PlayArrow,
                label = stringResource(R.string.still_watching_continue),
                onClick = onContinue,
                focusRequester = continueFocusRequester,
                modifier = Modifier
                    .onPlaced {
                        if (!continueFocused) {
                            continueFocused = true
                            runCatching { continueFocusRequester.requestFocus() }
                        }
                    }
                    .focusProperties {
                        if (isRtl) left = exitFocusRequester else right = exitFocusRequester
                        if (isRtl) right = Cancel else left = Cancel
                    }
            )
            PostPlayPillButton(
                icon = Icons.Default.Close,
                label = stringResource(R.string.still_watching_exit),
                onClick = onDismiss,
                focusRequester = exitFocusRequester,
                modifier = Modifier.focusProperties {
                    if (isRtl) right = continueFocusRequester else left = continueFocusRequester
                    if (isRtl) left = Cancel else right = Cancel
                }
            )
        }
    }
}

/** A pill action inside the card: frosted at rest, white with black content on focus. */
@Composable
private fun PostPlayPillButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    MoviesCard(
        onClick = onClick,
        shape = M3eShapes.capsule,
        modifier = modifier.focusRequester(focusRequester)
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = PillButtonMinHeight)
                .padding(horizontal = MoviesTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(PillButtonIconSize)
            )
            Text(
                text = label,
                fontSize = PostPlayLabelSize,
                modifier = Modifier.padding(start = MoviesTheme.spacing.xs)
            )
        }
    }
}

private val PostPlayCardWidth = 480.dp
private val PostPlayMetaSize = PlayerTenFoot.atLeast(11.sp, PlayerTenFoot.metaText)
private val PostPlayTitleSize = PlayerTenFoot.atLeast(14.sp, PlayerTenFoot.bodyText)
private val PostPlayLabelSize = PlayerTenFoot.atLeast(12.sp, PlayerTenFoot.bodyText)
private val PillButtonMinHeight = 48.dp
private val PillButtonIconSize = 24.dp

@Composable
private fun NextEpisodeThumbnail(
    thumbnail: String?,
    contentDescription: String?,
    blurred: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageRequest = remember(context, thumbnail, blurred) {
        ImageRequest.Builder(context)
            .data(thumbnail)
            .crossfade(true)
            .apply {
                if (blurred) transformations(BlurTransformation())
            }
            .build()
    }
    Box(
        modifier = modifier
            .size(width = 112.dp, height = 64.dp)
            .clip(RoundedCornerShape(9.dp))
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.32f))
                    )
                )
        )
    }
}

@Composable
private fun NextEpisodeStatusLine(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = postPlayInk().copy(alpha = 0.78f),
        fontSize = PostPlayMetaSize,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun nextEpisodeDisplayLabel(nextEpisode: NextEpisodeInfo): String {
    if (nextEpisode.isOtherType) return nextEpisode.title
    val context = LocalContext.current
    val code = stringResource(
        R.string.season_episode_format,
        nextEpisode.season,
        nextEpisode.episode
    )
    val localizedTitle = nextEpisode.title.localizeEpisodeTitle(context)
    return "$code • $localizedTitle"
}

/** The card's content colour: white at rest, black once the focused card turns white. */
@Composable
private fun postPlayInk(): Color = LocalContentColor.current
