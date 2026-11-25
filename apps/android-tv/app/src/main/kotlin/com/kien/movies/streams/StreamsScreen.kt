package com.kien.movies.streams

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.kien.movies.design.Focus
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvControls
import com.kien.movies.design.TvGlass
import com.kien.movies.design.TvGrid
import com.kien.movies.design.TvInsets
import com.kien.movies.design.TvSheet
import com.kien.movies.design.TvType
import com.kien.movies.design.focusLift
import com.kien.movies.design.glassEdge
import com.kien.movies.player.PlaybackRequest
import com.kien.movies.player.PlaybackScreen
import com.kien.movies.player.StreamNotice
import com.kien.movies.shell.AppRoute
import com.kien.movies.shell.Router

/**
 * Streams (tvOS StreamsView.swift): the title column on the left stays in view, and loading, empty
 * or the groups as addons answer fill the column on the right, 20 dp from it, over the receded
 * backdrop of [StreamsBackground]. A press checks the resume point, asks Resume or Start Over when
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
            StreamsBackground(backdropUrl = viewModel.target.backdropUrl)
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
        StreamsResumeDialog(pending = pending, onAnswer = viewModel::answerResume)
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
    // hasRows too: the Android only Try Again of an all failed list leaves when rows arrive.
    val hasRows = (content as? StreamsContent.Groups)?.hasRows == true
    LaunchedEffect(content::class, hasRows) {
        if (restoresRow) return@LaunchedEffect
        withFrameNanos {}
        bodyFocus.requestFocus()
    }
    val groups = content as? StreamsContent.Groups
    Row(modifier = Modifier.fillMaxSize()) {
        StreamsHeader(
            target = viewModel.target,
            resume = state.directResume,
            sourceCount = groups?.sourceCount ?: 0,
            searchingAddonCount = groups?.searchingAddonCount ?: 0,
            isResumingLastLink = state.isResumingLastLink,
            modifier = Modifier.padding(start = TvGrid.sideMargin, top = PAGE_TOP, bottom = TvInsets.verticalMargin)
        )
        // The body runs to the screen edges, its margins go inside it: the list scrolls under the
        // top edge and a focused row's scale is not clipped. No cross fade between the bodies
        // (tvOS fades 0.35 s): during a fade both are composed, and the focus request above could
        // land in the one that is leaving.
        Box(modifier = Modifier.weight(1f).fillMaxHeight().focusRequester(bodyFocus)) {
            when (content) {
                StreamsContent.Loading -> StreamsLoading(modifier = Modifier.padding(BodyMargins))

                is StreamsContent.Empty -> StreamsEmpty(
                    emptyState = content.emptyState,
                    onRetry = viewModel::reload,
                    modifier = Modifier.padding(BodyMargins)
                )

                is StreamsContent.Groups -> StreamGroupList(
                    content = content,
                    selection = state.selection,
                    listState = listState,
                    restoreKey = restoreKey,
                    onRestored = onRestored,
                    onSelect = { row -> viewModel.select(row.stream) },
                    onRetry = viewModel::reload,
                    contentPadding = BodyMargins
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
 * Resume or Start Over (tvOS confirmationDialog with its title hidden, S5). The labels come from
 * Kotlin. Back closes it without playing; there is no Cancel button, as Android TV asks.
 */
@Composable
private fun StreamsResumeDialog(pending: PendingResume, onAnswer: (Boolean?) -> Unit) {
    val resumeFocus = remember { FocusRequester() }
    Dialog(onDismissRequest = { onAnswer(null) }) {
        LaunchedEffect(pending) {
            withFrameNanos {}
            resumeFocus.requestFocus()
        }
        Column(
            modifier = Modifier
                .width(DIALOG_WIDTH)
                .background(TvColors.dialogPanel, ResumeDialogShape)
                .glassEdge(ResumeDialogShape)
                .padding(DIALOG_PADDING)
                .testTag(STREAMS_RESUME_DIALOG_TAG),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DIALOG_GAP)
        ) {
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
        colors = TvGlass.buttonColors(),
        scale = TvGlass.noButtonScale(),
        shape = TvGlass.pillShape(),
        border = TvGlass.buttonBorder(),
        contentPadding = TvGlass.pillPadding,
        modifier = modifier.fillMaxWidth().height(TvControls.pillHeight).focusLift(Focus.buttonScale)
    ) {
        Text(
            text = text,
            style = TvType.bodyEmphasized,
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

// tvOS literals, pt / 2.
private val PAGE_TOP = 30.dp // the header and the first addon start 60 pt down
private val COLUMN_GAP = 20.dp // HStack spacing 40
private val DIALOG_WIDTH = 320.dp
private val DIALOG_PADDING = 24.dp
private val DIALOG_GAP = 12.dp

/** The right column's margins: 20 dp from the header, the 48 dp side margin, overscan at the bottom. */
private val BodyMargins = PaddingValues(start = COLUMN_GAP, end = TvGrid.sideMargin, top = PAGE_TOP, bottom = TvInsets.verticalMargin)

/** Concentric with its capsule buttons: 16 dp radius + 24 dp padding = 40 dp. */
private val ResumeDialogShape = TvSheet.shapeFor(DIALOG_PADDING)
