package com.kien.movies.ui.screens.player

import com.kien.movies.ui.theme.MoviesTheme

import android.view.KeyEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onPlaced
import com.kien.movies.ui.screens.detail.requestFocusAfterFrames
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonBorder
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults
import com.kien.movies.ui.theme.ExpressiveFocus
import com.kien.movies.ui.theme.isExpressiveBuild
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kien.movies.R
import com.kien.movies.ui.util.keepLtr
import com.kien.movies.ui.util.languageCodeToName

@Composable
fun StreamInfoOverlay(
    visible: Boolean,
    onClose: () -> Unit,
    data: StreamInfoData?,
    hudEnabled: Boolean,
    hudButtonShown: Boolean,
    onToggleHud: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hudFocusRequester = remember { FocusRequester() }
    var hudFocused by remember(visible) { mutableStateOf(false) }
    var hudPlaced by remember(visible) { mutableStateOf(false) }

    LaunchedEffect(visible, hudButtonShown, hudPlaced) {
        if (!visible || !hudButtonShown || !hudPlaced) return@LaunchedEffect
        hudFocusRequester.requestFocusAfterFrames(frames = 0)
    }
    PlayerOverlayScaffold(
        visible = visible,
        onDismiss = onClose,
        // The scaffold holds focus so the overlay still closes on centre, which leaves nothing
        // pointing at the button; a direction press is the request to reach it, and it also
        // recovers focus if anything else took it while the overlay stayed open.
        modifier = modifier.onPreviewKeyEvent { event ->
            if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
            val isDirection = when (event.nativeKeyEvent.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT -> true
                else -> false
            }
            if (!isDirection || hudFocused || !hudButtonShown) return@onPreviewKeyEvent false
            runCatching { hudFocusRequester.requestFocus() }.isSuccess
        },
        dismissOnCenter = !hudButtonShown,
        contentPadding = PaddingValues(start = MoviesTheme.spacing.xxxl, end = MoviesTheme.spacing.xxxl, top = 36.dp, bottom = 36.dp)
    ) {
        // Someone who never turned the overlay on has no use for a control they cannot interpret.
        if (hudButtonShown) {
            StreamInfoHudButton(
                enabled = hudEnabled,
                onClick = onToggleHud,
                focusRequester = hudFocusRequester,
                onFocusChanged = { hudFocused = it },
                onLaidOut = { hudPlaced = true },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
        if (data != null) {
            Column(
                modifier = Modifier.align(Alignment.BottomStart),
                verticalArrangement = Arrangement.Bottom
            ) {
                StreamInfoContent(data = data)
            }
        }
    }
}

@Composable
private fun StreamInfoContent(data: StreamInfoData) {
    // SOURCE section
    val hasSourceInfo = data.addonName != null || data.streamName != null
    if (hasSourceInfo) {
        SectionLabel(stringResource(R.string.stream_info_section_source))
        Spacer(modifier = Modifier.height(MoviesTheme.spacing.xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!data.addonLogo.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(data.addonLogo)
                        .crossfade(true)
                        .build(),
                    contentDescription = data.addonName,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(MoviesTheme.radii.sm)),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(MoviesTheme.spacing.md))
            }
            Column {
                if (data.addonName != null) {
                    Text(
                        text = data.addonName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (data.streamName != null && data.streamName != data.addonName) {
                    Text(
                        text = data.streamName.replace("\n", " · "),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MoviesTheme.colors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (!data.streamDescription.isNullOrBlank()) {
            Text(
                text = data.streamDescription.replace("\n", " · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MoviesTheme.colors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = MoviesTheme.spacing.xs)
            )
        }
        if (data.playerEngine != null || data.serverPlayback != null) {
            Spacer(modifier = Modifier.height(MoviesTheme.spacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                InfoItem(label = stringResource(R.string.stream_info_player_engine), value = data.playerEngine)
                InfoItem(
                    label = stringResource(R.string.stream_info_server),
                    value = data.serverPlayback,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
        Spacer(modifier = Modifier.height(MoviesTheme.spacing.lg))
    }

    // FILE section
    val hasFileInfo = data.filename != null || data.fileSize != null
    if (hasFileInfo) {
        SectionLabel(stringResource(R.string.stream_info_section_file))
        Spacer(modifier = Modifier.height(MoviesTheme.spacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
            InfoItem(label = stringResource(R.string.stream_info_filename), value = data.filename, modifier = Modifier.weight(1f))
            InfoItem(label = stringResource(R.string.stream_info_size), value = data.fileSize?.let { formatFileSize(it) })
        }
        Spacer(modifier = Modifier.height(MoviesTheme.spacing.lg))
    }

    // VIDEO section
    val hasVideoInfo = data.videoCodec != null || data.videoWidth != null ||
        data.videoFrameRate != null || data.videoBitrate != null || data.fileBitrate != null
    if (hasVideoInfo) {
        SectionLabel(stringResource(R.string.stream_info_section_video))
        Spacer(modifier = Modifier.height(MoviesTheme.spacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
            InfoItem(label = stringResource(R.string.stream_info_codec), value = data.videoCodec)
            InfoItem(
                label = stringResource(R.string.stream_info_resolution),
                value = if (data.videoWidth != null && data.videoHeight != null) {
                    formatResolution(data.videoWidth, data.videoHeight)
                } else null
            )
            InfoItem(
                label = stringResource(R.string.stream_info_frame_rate),
                value = data.videoFrameRate?.let { "%.3f fps".format(it) }
            )
            // A container that declares no track bitrate leaves only the whole file rate, which
            // includes audio and overhead, so it is labelled apart rather than shown as the video rate.
            if (data.videoBitrate != null) {
                InfoItem(
                    label = stringResource(R.string.stream_info_bitrate),
                    value = formatBitrate(data.videoBitrate)
                )
            } else {
                InfoItem(
                    label = stringResource(R.string.stream_info_bitrate_file),
                    value = data.fileBitrate?.let { formatBitrate(it) }
                )
            }
        }
        Spacer(modifier = Modifier.height(MoviesTheme.spacing.lg))
    }

    // AUDIO section
    val hasAudioInfo = data.audioCodec != null || data.audioChannels != null || data.audioLanguage != null || data.audioSampleRate != null
    if (hasAudioInfo) {
        SectionLabel(stringResource(R.string.stream_info_section_audio))
        Spacer(modifier = Modifier.height(MoviesTheme.spacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
            InfoItem(label = stringResource(R.string.stream_info_codec), value = data.audioCodec)
            InfoItem(label = stringResource(R.string.stream_info_channels), value = data.audioChannels)
            InfoItem(
                label = stringResource(R.string.stream_info_sample_rate),
                value = data.audioSampleRate?.let { "${it / 1000} kHz" }
            )
            InfoItem(
                label = stringResource(R.string.stream_info_language),
                value = data.audioLanguage?.let { languageCodeToName(it) }
            )
        }
        Spacer(modifier = Modifier.height(MoviesTheme.spacing.lg))
    }

    // SUBTITLE section
    val hasSubtitleInfo = data.subtitleName != null || data.subtitleCodec != null || data.subtitleLanguage != null
    if (hasSubtitleInfo) {
        SectionLabel(stringResource(R.string.stream_info_section_subtitle))
        Spacer(modifier = Modifier.height(MoviesTheme.spacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
            InfoItem(label = stringResource(R.string.stream_info_name), value = data.subtitleName)
            InfoItem(label = stringResource(R.string.stream_info_codec), value = data.subtitleCodec)
            InfoItem(
                label = stringResource(R.string.stream_info_language),
                value = data.subtitleLanguage?.let { languageCodeToName(it) }
            )
            InfoItem(label = stringResource(R.string.stream_info_source), value = data.subtitleSource)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MoviesTheme.colors.TextTertiary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun InfoItem(label: String, value: String?, modifier: Modifier = Modifier) {
    if (value == null) return
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MoviesTheme.colors.TextTertiary
        )
        Text(
            text = value.keepLtr(),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824L -> stringResource(R.string.unit_size_gb, "%.1f".format(bytes / 1_073_741_824.0))
        bytes >= 1_048_576L -> stringResource(R.string.unit_size_mb, "%.1f".format(bytes / 1_048_576.0))
        bytes >= 1024L -> stringResource(R.string.unit_size_kb, "%.1f".format(bytes / 1024.0))
        else -> stringResource(R.string.unit_size_b, bytes)
    }
}

private fun formatBitrate(bps: Int): String {
    return when {
        bps >= 1_000_000 -> "%.1f Mbps".format(bps / 1_000_000.0)
        bps >= 1_000 -> "%.0f kbps".format(bps / 1_000.0)
        else -> "$bps bps"
    }
}

internal fun formatResolution(width: Int, height: Int): String {
    val maxDim = maxOf(width, height)
    val label = when {
        maxDim >= 3600 -> "4K"
        maxDim >= 2400 -> "1440p"
        maxDim >= 1800 -> "1080p"
        maxDim >= 1200 -> "720p"
        maxDim >= 800 -> "480p"
        else -> "${minOf(width, height)}p"
    }
    return "$width × $height ($label)"
}

@Composable
private fun StreamInfoHudButton(
    enabled: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    onLaidOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Button(
        onClick = onClick,
        // Every direction leads back to this button, so a stray press cannot drop focus onto the
        // transport controls still drawn behind the overlay.
        modifier = modifier
            .focusRequester(focusRequester)
            .onPlaced { onLaidOut() }
            .onFocusChanged {
                isFocused = it.isFocused
                onFocusChanged(it.isFocused)
            }
            .focusProperties {
                up = FocusRequester.Cancel
                down = FocusRequester.Cancel
                left = FocusRequester.Cancel
                right = FocusRequester.Cancel
            }
            .then(
                if (isExpressiveBuild) {
                    Modifier
                        .heightIn(min = HudToggleMinHeight)
                        .playerFocusLift(isFocused)
                } else {
                    Modifier
                }
            ),
        contentPadding = if (isExpressiveBuild) {
            PaddingValues(horizontal = MoviesTheme.spacing.lg, vertical = MoviesTheme.spacing.sm)
        } else {
            PaddingValues(horizontal = 14.dp, vertical = 6.dp)
        },
        colors = if (isExpressiveBuild) {
            hudToggleColors(isFocused, enabled)
        } else ButtonDefaults.colors(
            containerColor = if (enabled) MoviesTheme.colors.Secondary.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
            contentColor = if (enabled) Color.White else Color.White.copy(alpha = 0.7f),
            focusedContainerColor = if (enabled) MoviesTheme.colors.Secondary else Color.White,
            focusedContentColor = if (enabled) MoviesTheme.colors.OnSecondary else Color.Black
        ),
        shape = if (isExpressiveBuild) {
            PlayerControlStyle.capsuleShape
        } else {
            ButtonDefaults.shape(shape = RoundedCornerShape(MoviesTheme.radii.sm))
        },
        scale = if (isExpressiveBuild) {
            PlayerControlStyle.stillScale
        } else {
            ButtonDefaults.scale(focusedScale = 1.05f)
        },
        border = if (isExpressiveBuild) hudToggleNoBorder() else ButtonDefaults.border(
            border = Border(
                border = BorderStroke(
                    1.dp,
                    if (enabled) MoviesTheme.colors.Secondary.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(MoviesTheme.radii.sm)
            ),
            focusedBorder = Border(
                border = MoviesTheme.focusRing.border(MoviesTheme.spacing.xxs),
                shape = RoundedCornerShape(MoviesTheme.radii.sm)
            )
        )
    ) {
        val statusText = if (enabled) {
            stringResource(R.string.diag_value_on)
        } else {
            stringResource(R.string.diag_value_off)
        }
        val hudLabel = stringResource(R.string.stream_info_hud)

        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (enabled) {
                        if (isFocused) MoviesTheme.colors.OnSecondary else MoviesTheme.colors.FocusRing
                    } else {
                        if (isFocused) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.4f)
                    }
                )
        )
        Spacer(modifier = Modifier.width(MoviesTheme.spacing.xs))
        Text(
            text = "$hudLabel: $statusText",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isFocused || enabled) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

private val HudToggleMinHeight = 48.dp

/** On rests in the accent's tonal container, off on the plain surface; focus fills the accent. */
@Composable
private fun hudToggleColors(focused: Boolean, enabled: Boolean): ButtonColors {
    val container = ExpressiveFocus.containerColor(
        focused = focused,
        resting = if (enabled) {
            MoviesTheme.colors.SecondaryVariant
        } else {
            MoviesTheme.colors.BackgroundCard
        }
    )
    val content = ExpressiveFocus.contentColor(
        focused = focused,
        resting = if (enabled) {
            MoviesTheme.colors.OnSecondaryVariant
        } else {
            MoviesTheme.colors.TextPrimary
        }
    )
    return ButtonDefaults.colors(
        containerColor = container,
        contentColor = content,
        focusedContainerColor = container,
        focusedContentColor = content
    )
}

@Composable
private fun hudToggleNoBorder(): ButtonBorder = ButtonDefaults.border(
    border = Border.None,
    focusedBorder = Border.None
)
