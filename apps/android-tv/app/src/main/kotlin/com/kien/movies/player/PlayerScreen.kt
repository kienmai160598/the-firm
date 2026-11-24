package com.kien.movies.player

import android.app.Application
import android.view.KeyEvent as NativeKeyEvent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.Text
import com.kien.movies.R
import com.kien.movies.design.Motion
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvControls
import com.kien.movies.design.TvGlass
import com.kien.movies.design.TvGrid
import com.kien.movies.design.TvType
import com.nuvio.tvos.TvosStreamTarget
import com.nuvio.tvos.rules.PlaybackAdvisory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val PLAYER_SURFACE_TAG = "player-surface"
const val PLAYER_LOADING_TAG = "player-loading"
const val PLAYER_CONTROLS_TAG = "player-controls"

/** The title of the notice Streams shows when playback fails (tvOS PlaybackController). */
const val PLAYBACK_FAILED_TITLE = "Playback failed"

/** The notice message starts with this, then the reason (tvOS PlaybackController). */
const val PLAYER_ERROR_PREFIX = "The player stopped with an error"

/** The three round buttons above the bar (tvOS MpvControl). */
enum class PlayerControl(val label: String, val icon: Int) {
    Subtitles(label = "Subtitles", icon = R.drawable.ic_player_subtitles),
    Audio(label = "Audio", icon = R.drawable.ic_player_audio),
    Info(label = "Info", icon = R.drawable.ic_player_info)
}

fun playerControlTag(control: PlayerControl) = "player-control-${control.name.lowercase()}"

/** What opens above the buttons (tvOS MpvPanel). */
sealed interface PlayerPanel {
    val control: PlayerControl

    data class Tracks(val kind: TrackKind) : PlayerPanel {
        override val control: PlayerControl get() = if (kind == TrackKind.Audio) PlayerControl.Audio else PlayerControl.Subtitles
    }

    data object Info : PlayerPanel {
        override val control: PlayerControl get() = PlayerControl.Info
    }
}

// tvOS MpvPlayerScreen.swift literals, pt / 2.
private const val SKIP_SHOWN_MILLIS = 1_000L
private const val INFO_REFRESH_MILLIS = 1_000L
private const val CONTROLS_ANIMATION_MILLIS = 400
private val BottomInset = 48.dp // 96 pt
private val ControlsGap = 12.dp
private val PanelGap = 12.dp // 24 pt
private val ButtonGap = 13.dp // spec: 26 pt between the hero's buttons
private val TitleGap = 4.dp

// tvOS mpvCircleButtonStyle tints an open control white at 40%.
private val OpenControlFill = Color.White.copy(alpha = 0.4f)
private val InfoRowGap = 20.dp // 40 pt
private val ControlsRise = 14.dp // 28 pt
private val SpinnerSize = 24.dp // ProgressView scaled 1.6
private val SpinnerStroke = 3.dp

/**
 * The player (tvOS MpvPlayerScreen.swift): video, spinner, the controls (title, three round
 * buttons, transport bar) straight over the video at the bottom with no scrim behind them, menus
 * above the buttons as opaque platters, and the advisory banner. Owns the remote: see [PlayerContent].
 */
@Composable
fun PlayerScreen(request: PlaybackRequest, onFailure: (StreamNotice) -> Unit, onExit: () -> Unit) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel = viewModel(key = request.id) { PlayerViewModel(application, request) }
    val controller = viewModel.controller
    val activity = LocalActivity.current
    val state by controller.state.collectAsStateWithLifecycle()

    LaunchedEffect(controller) { controller.start() }
    // Released when the cover leaves (Back, end, failure, Streams popped), not in onCleared: the
    // ViewModel lives in the Streams entry's store. A configuration change keeps playing.
    DisposableEffect(viewModel) {
        onDispose { if (activity?.isChangingConfigurations != true) viewModel.release() }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { controller.enterBackground() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { controller.enterForeground() }
    LaunchedEffect(state.phase) {
        when (state.phase) {
            PlaybackPhase.Failed -> onFailure(StreamNotice(PLAYBACK_FAILED_TITLE, "$PLAYER_ERROR_PREFIX: ${state.failure}"))
            PlaybackPhase.Ended -> onExit()
            else -> Unit
        }
    }
    PlayerContent(controller = controller, state = state, target = request.target, onExit = onExit)
}

/**
 * The remote, as on tvOS:
 * - select toggles pause (or commits a pending scrub); left and right skip 10 s and add up;
 *   holding them scrubs (see [HoldScrub]); down enters the scrub view, up leaves it or goes to the
 *   buttons; the play/pause key toggles and commits.
 * - controls hide after 5 s unless paused, a menu is open or a scrub is pending.
 * - Back: close menu, cancel pending scrub, leave scrubbing, focus the video, hide controls, exit.
 * Streams moves focus into the cover (it lands on the video surface, the first focusable child) and
 * back to the pressed row afterwards; inside, focus never leaves the player.
 */
@Composable
private fun PlayerContent(controller: PlayerController, state: PlayerState, target: TvosStreamTarget, onExit: () -> Unit) {
    val player by controller.player.collectAsStateWithLifecycle()
    val preview by controller.scrubPreview.collectAsStateWithLifecycle()
    val advisory = remember(target) { PlaybackAdvisory.of(target.ageRating, target.genres) }
    val scope = rememberCoroutineScope()
    val surfaceFocus = remember { FocusRequester() }
    val controlFocus = remember { PlayerControl.entries.associateWith { FocusRequester() } }
    var isControlsShown by remember { mutableStateOf(true) }
    var isScrubbing by remember { mutableStateOf(false) }
    var isSurfaceFocused by remember { mutableStateOf(false) }
    var panel by remember { mutableStateOf<PlayerPanel?>(null) }
    var trackOptions by remember { mutableStateOf(emptyList<TrackOption>()) }
    var infoRows by remember { mutableStateOf(emptyList<InfoRow>()) }
    var skip by remember { mutableStateOf<SkipBadge?>(null) }
    var interaction by remember { mutableIntStateOf(0) }
    var lastKeyTime by remember { mutableLongStateOf(0L) }
    var wasScrubPending by remember { mutableStateOf(false) }

    fun showControls() {
        isControlsShown = true
        interaction++
    }

    fun hideControls() {
        surfaceFocus.requestFocus()
        isScrubbing = false
        isControlsShown = false
    }

    fun focusAfterFrame(requester: FocusRequester) {
        scope.launch {
            withFrameNanos {}
            requester.requestFocus()
        }
    }

    fun closePanel() {
        val closing = panel ?: return
        panel = null
        focusAfterFrame(controlFocus.getValue(closing.control))
    }

    fun open(control: PlayerControl) {
        if (panel?.control == control) return closePanel()
        panel = when (control) {
            PlayerControl.Subtitles, PlayerControl.Audio -> {
                val kind = if (control == PlayerControl.Audio) TrackKind.Audio else TrackKind.Subtitles
                trackOptions = controller.trackOptions(kind)
                PlayerPanel.Tracks(kind)
            }

            PlayerControl.Info -> {
                infoRows = controller.mediaInfo()
                PlayerPanel.Info
            }
        }
        showControls()
    }

    fun togglePause() {
        if (controller.isScrubPending) {
            controller.commitScrub()
        } else {
            isScrubbing = false
            controller.togglePause()
        }
        showControls()
    }

    fun moveHorizontally(native: NativeKeyEvent, isForward: Boolean) {
        val sign = if (isForward) 1 else -1
        if (native.repeatCount == 0) {
            if (controller.isScrubPending) {
                controller.moveScrub(sign * HoldScrub.TAP_STEP_SECONDS.toDouble())
            } else {
                isScrubbing = true
                controller.skip(sign)
                skip = skip?.adding(isForward) ?: SkipBadge(isForward, presses = 1)
            }
        } else if (controller.beginScrub()) {
            isScrubbing = true
            val held = native.eventTime - native.downTime
            val sinceLast = native.eventTime - lastKeyTime
            controller.moveScrub(sign * HoldScrub.advanceSeconds(held, sinceLast, controller.clock.value.durationSeconds))
        }
        lastKeyTime = native.eventTime
        showControls()
    }

    fun onSurfaceKey(event: KeyEvent): Boolean {
        val handled = event.key in surfaceKeys
        if (event.type != KeyEventType.KeyDown || !handled) return handled
        val native = event.nativeKeyEvent
        when (event.key) {
            Key.DirectionLeft, Key.DirectionRight -> moveHorizontally(native, isForward = event.key == Key.DirectionRight)

            Key.DirectionDown -> {
                if (isControlsShown) isScrubbing = true
                showControls()
            }

            Key.DirectionUp -> {
                when {
                    !isControlsShown || controller.isScrubPending -> Unit
                    isScrubbing -> isScrubbing = false
                    else -> controlFocus.getValue(PlayerControl.Subtitles).requestFocus()
                }
                showControls()
            }

            else -> if (native.repeatCount == 0) togglePause()
        }
        return true
    }

    fun onMediaKey(event: KeyEvent): Boolean {
        val handled = event.key in mediaKeys
        if (event.type != KeyEventType.KeyDown || !handled || event.nativeKeyEvent.repeatCount > 0) return handled
        when (event.key) {
            Key.MediaPlayPause -> togglePause()
            Key.MediaPlay -> if (controller.isScrubPending) controller.commitScrub() else controller.setPaused(false)
            Key.MediaPause -> controller.setPaused(true)
            Key.MediaFastForward -> moveHorizontally(event.nativeKeyEvent, isForward = true)
            Key.MediaRewind -> moveHorizontally(event.nativeKeyEvent, isForward = false)
        }
        showControls()
        return true
    }

    BackHandler {
        when {
            panel != null -> closePanel()

            controller.isScrubPending -> controller.cancelScrub()

            isScrubbing -> {
                isScrubbing = false
                showControls()
            }

            isControlsShown && !isSurfaceFocused -> surfaceFocus.requestFocus()

            isControlsShown -> isControlsShown = false

            else -> onExit()
        }
    }

    LaunchedEffect(isControlsShown, state.isPaused, panel, preview != null, interaction) {
        if (!isControlsShown) return@LaunchedEffect
        delay(Motion.controlsHideMillis)
        if (!state.isPaused && panel == null && !controller.isScrubPending) hideControls()
    }
    LaunchedEffect(preview != null) {
        // A settled, committed or cancelled scrub leaves the scrub view (tvOS endScrub).
        if (preview == null && wasScrubPending) {
            isScrubbing = false
            showControls()
        }
        wasScrubPending = preview != null
    }
    LaunchedEffect(skip) {
        if (skip == null) return@LaunchedEffect
        delay(SKIP_SHOWN_MILLIS)
        skip = null
    }
    LaunchedEffect(panel) {
        while (panel == PlayerPanel.Info) {
            infoRows = controller.mediaInfo()
            delay(INFO_REFRESH_MILLIS)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent(::onMediaKey)
            .focusProperties {
                // Streams focuses the cover; whatever it lands on first, start on the video.
                onEnter = { surfaceFocus.requestFocus() }
                this.onExit = { cancelFocusChange() }
            }
            .focusGroup()
    ) {
        PlayerSurface(player = player, subtitlesLifted = isControlsShown)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(surfaceFocus)
                .onFocusChanged { focus ->
                    isSurfaceFocused = focus.isFocused
                    if (focus.isFocused && panel != null) panel = null
                }
                .onKeyEvent(::onSurfaceKey)
                .focusable()
                .testTag(PLAYER_SURFACE_TAG)
                .semantics { contentDescription = "Video" }
        )
        if (state.isWaiting) LoadingSpinner(modifier = Modifier.align(Alignment.Center))
        AnimatedVisibility(
            visible = isControlsShown,
            enter = fadeIn(tween(CONTROLS_ANIMATION_MILLIS)) + slideInVertically(tween(CONTROLS_ANIMATION_MILLIS)) { it / CONTROLS_RISE_SHARE },
            exit = fadeOut(tween(CONTROLS_ANIMATION_MILLIS)) + slideOutVertically(tween(CONTROLS_ANIMATION_MILLIS)) { it / CONTROLS_RISE_SHARE },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = TvGrid.sideMargin, end = TvGrid.sideMargin, bottom = BottomInset)
                ) {
                    when (val open = panel) {
                        is PlayerPanel.Tracks -> TrackMenu(kind = open.kind, options = trackOptions, onSelect = { option ->
                            controller.select(open.kind, option)
                            closePanel()
                        })

                        PlayerPanel.Info -> InfoMenu(source = controller.source, rows = infoRows)

                        null -> Unit
                    }
                    if (panel != null) Spacer(modifier = Modifier.size(PanelGap))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(ControlsGap),
                        modifier = Modifier.fillMaxWidth().testTag(PLAYER_CONTROLS_TAG)
                    ) {
                        AnimatedVisibility(visible = !isScrubbing, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                            InfoRow(
                                target = target,
                                panel = panel,
                                controlFocus = controlFocus,
                                onOpen = ::open,
                                onDown = { surfaceFocus.requestFocus() },
                                onControlFocused = { control ->
                                    if (panel != null && panel?.control != control) panel = null
                                    interaction++
                                }
                            )
                        }
                        TransportBar(
                            clock = controller.clock,
                            previewSeconds = preview,
                            isPausedByViewer = state.isPausedByViewer,
                            isScrubbing = isScrubbing,
                            skip = skip
                        )
                    }
                }
            }
        }
        advisory?.let { PlaybackAdvisoryBanner(advisory = it, hasPlaybackStarted = state.hasPlayed) }
    }
}

@Composable
private fun InfoRow(
    target: TvosStreamTarget,
    panel: PlayerPanel?,
    controlFocus: Map<PlayerControl, FocusRequester>,
    onOpen: (PlayerControl) -> Unit,
    onDown: () -> Unit,
    onControlFocused: (PlayerControl) -> Unit
) {
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(TitleGap), modifier = Modifier.weight(1f)) {
            target.subtitle?.let { episodeLine ->
                Text(text = episodeLine, style = TvType.caption1, color = TvColors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(text = target.title, style = TvType.headline, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(modifier = Modifier.width(InfoRowGap))
        // Down goes back to the video (tvOS focus engine); 2D search finds nothing below, the
        // video surface contains the buttons.
        Row(
            horizontalArrangement = Arrangement.spacedBy(ButtonGap),
            modifier = Modifier.onPreviewKeyEvent { event ->
                val isDown = event.key == Key.DirectionDown
                if (isDown && event.type == KeyEventType.KeyDown) onDown()
                isDown
            }
        ) {
            val isTrackMenuOpen = panel is PlayerPanel.Tracks
            PlayerControl.entries.forEach { control ->
                val isOpen = panel?.control == control
                IconButton(
                    onClick = { onOpen(control) },
                    enabled = !isTrackMenuOpen || isOpen,
                    shape = TvGlass.circleShape(),
                    scale = TvGlass.circleScale(),
                    border = TvGlass.liquidCircleBorder(),
                    colors = TvGlass.circleColors(containerColor = if (isOpen) OpenControlFill else TvColors.playerGlass),
                    modifier = Modifier
                        .size(TvControls.circle)
                        .focusRequester(controlFocus.getValue(control))
                        .onFocusChanged { if (it.isFocused) onControlFocused(control) }
                        .testTag(playerControlTag(control))
                ) {
                    Icon(painter = painterResource(control.icon), contentDescription = control.label, modifier = Modifier.size(TvControls.glyph))
                }
            }
        }
    }
}

/** A small spinning arc, drawn and rotated in the draw phase (tv-material has no progress view). */
@Composable
private fun LoadingSpinner(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "spinner")
    val rotation = transition.animateFloat(
        initialValue = 0f,
        targetValue = FULL_TURN,
        animationSpec = infiniteRepeatable(tween(SPIN_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation"
    )
    Canvas(
        modifier = modifier
            .size(SpinnerSize)
            .graphicsLayer { rotationZ = rotation.value }
            .testTag(PLAYER_LOADING_TAG)
            .semantics { contentDescription = "Loading" }
    ) {
        drawArc(
            color = Color.White,
            startAngle = 0f,
            sweepAngle = SPINNER_SWEEP,
            useCenter = false,
            style = Stroke(width = SpinnerStroke.toPx(), cap = StrokeCap.Round)
        )
    }
}

private val surfaceKeys = setOf(Key.DirectionLeft, Key.DirectionRight, Key.DirectionUp, Key.DirectionDown, Key.DirectionCenter, Key.Enter, Key.NumPadEnter)
private val mediaKeys = setOf(Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause, Key.MediaFastForward, Key.MediaRewind)
private const val CONTROLS_RISE_SHARE = 8
private const val FULL_TURN = 360f
private const val SPINNER_SWEEP = 270f
private const val SPIN_MILLIS = 1_000
