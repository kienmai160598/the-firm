package com.kien.movies.streams

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.kien.movies.design.Backdrop
import com.kien.movies.design.CardSize
import com.kien.movies.design.GlassStyle
import com.kien.movies.design.Palette
import com.kien.movies.design.Spacing
import com.kien.movies.design.TextStyles
import com.kien.movies.player.PlaybackRequest
import com.kien.movies.player.PlaybackScreen
import com.kien.movies.player.StreamNotice
import com.kien.movies.shell.AppRoute
import com.kien.movies.shell.Router

/**
 * Streams (tvOS StreamsView.swift): header, then loading, empty or the groups as addons answer,
 * over the receded backdrop. A press checks the resume point, asks Resume or Start Over when
 * there is one (S5), and presents the player as a full screen cover in this entry, as tvOS does
 * with fullScreenCover. A stream that cannot play opens the info sheet (S4).
 *
 * Focus inside this screen is owned here, the shell only focuses the entry on a route change:
 * the first control when the body changes (loading, empty, list), the player cover when it opens,
 * and the pressed row when the cover closes. While the cover is up the list is not composed, so
 * nothing under the player can take focus or draw.
 */
@Suppress("UNUSED_PARAMETER") // router: frozen entry signature; Streams opens no route in v1.
@Composable
fun StreamsScreen(route: AppRoute.Streams, router: Router) {
    val viewModel = viewModel { StreamsViewModel(route.target, route.resumesDirectly) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var restoreKey by remember { mutableStateOf<String?>(null) }
    val playback = state.playback

    Box(modifier = Modifier.fillMaxSize()) {
        if (playback == null) {
            viewModel.target.backdropUrl?.let { url -> Backdrop(url = url, isReceded = true) }
            StreamsPage(
                viewModel = viewModel,
                state = state,
                listState = listState,
                restoreKey = restoreKey,
                onRestored = { restoreKey = null }
            )
        } else {
            PlayerCover(
                request = playback,
                onFailure = { notice ->
                    restoreKey = playback.stream.key
                    viewModel.playbackFailed(notice)
                },
                onExit = {
                    restoreKey = playback.stream.key
                    viewModel.playbackEnded()
                }
            )
        }
    }
    state.sheet?.let { sheet -> StreamDetailSheet(sheet = sheet, onClose = viewModel::closeSheet) }
    state.pendingResume?.let { pending ->
        StreamsResumeDialog(title = viewModel.target.title, pending = pending, onAnswer = viewModel::answerResume)
    }
}

@Composable
private fun StreamsPage(
    viewModel: StreamsViewModel,
    state: StreamsUiState,
    listState: LazyListState,
    restoreKey: String?,
    onRestored: () -> Unit
) {
    val bodyFocus = remember { FocusRequester() }
    val content = state.content
    val restoresRow = restoreKey != null && content is StreamsContent.Groups
    LaunchedEffect(content::class) {
        if (restoresRow) return@LaunchedEffect
        withFrameNanos {}
        bodyFocus.requestFocus()
    }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(PAGE_GAP)) {
        StreamsHeader(
            target = viewModel.target,
            resume = state.directResume,
            modifier = Modifier.padding(start = HEADER_START, end = Spacing.screenHorizontal, top = Spacing.screenVertical)
        )
        // No cross fade between the bodies (tvOS fades 0.35 s): during a fade both bodies are
        // composed, and the focus request below could land in the one that is leaving.
        Box(modifier = Modifier.weight(1f).fillMaxWidth().focusRequester(bodyFocus)) {
            when (content) {
                StreamsContent.Loading -> StreamsLoading()

                is StreamsContent.Empty -> StreamsEmpty(emptyState = content.emptyState, onRetry = viewModel::reload)

                is StreamsContent.Groups -> StreamGroupList(
                    content = content,
                    selection = state.selection,
                    listState = listState,
                    restoreKey = restoreKey,
                    onRestored = onRestored,
                    onSelect = { row -> viewModel.select(row.stream) },
                    onRetry = viewModel::reload
                )
            }
        }
    }
}

/** The player over Streams; focus moves into it when it opens (it is in this tree, not a window). */
@Composable
private fun PlayerCover(request: PlaybackRequest, onFailure: (StreamNotice) -> Unit, onExit: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(request.id) {
        withFrameNanos {}
        focus.requestFocus()
    }
    Box(modifier = Modifier.fillMaxSize().focusRequester(focus)) {
        PlaybackScreen(request = request, onFailure = onFailure, onExit = onExit)
    }
}

/**
 * Resume or Start Over (tvOS confirmationDialog titled with the title, S5). The labels come from
 * Kotlin. Back closes it without playing; there is no Cancel button, as Android TV asks.
 */
@Composable
private fun StreamsResumeDialog(title: String, pending: PendingResume, onAnswer: (Boolean?) -> Unit) {
    val resumeFocus = remember { FocusRequester() }
    Dialog(onDismissRequest = { onAnswer(null) }) {
        LaunchedEffect(pending) {
            withFrameNanos {}
            resumeFocus.requestFocus()
        }
        Column(
            modifier = Modifier
                .width(DIALOG_WIDTH)
                .background(Palette.homeBackground, RoundedCornerShape(CardSize.cornerRadius))
                .padding(DIALOG_PADDING)
                .testTag(STREAMS_RESUME_DIALOG_TAG),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DIALOG_GAP)
        ) {
            Text(
                text = title,
                style = TextStyles.shelfHeader,
                color = Palette.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            DialogButton(
                text = pending.point.label,
                onClick = { onAnswer(true) },
                modifier = Modifier.focusRequester(resumeFocus).testTag(STREAMS_RESUME_TAG)
            )
            DialogButton(
                text = pending.point.startOverLabel,
                onClick = { onAnswer(false) },
                modifier = Modifier.testTag(STREAMS_START_OVER_TAG)
            )
        }
    }
}

@Composable
private fun DialogButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        colors = GlassStyle.buttonColors(),
        scale = GlassStyle.buttonScale(),
        shape = GlassStyle.buttonShape(),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = TextStyles.button,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

const val STREAMS_RESUME_DIALOG_TAG = "streams-resume-dialog"
const val STREAMS_RESUME_TAG = "streams-resume"
const val STREAMS_START_OVER_TAG = "streams-start-over"

// tvOS literals, pt / 2. The header sits at the 48 dp floor plus tvOS's 32 pt header padding.
private val HEADER_START = Spacing.screenHorizontal + 16.dp
private val PAGE_GAP = 18.dp // VStack spacing 36
private val DIALOG_WIDTH = 320.dp
private val DIALOG_PADDING = 24.dp
private val DIALOG_GAP = 12.dp
