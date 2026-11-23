package com.kien.movies.details

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.kien.movies.R
import com.kien.movies.design.Focus
import com.kien.movies.design.ImdbBadge
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvGrid
import com.kien.movies.design.TvShapes
import com.kien.movies.design.TvType
import com.kien.movies.design.glassEdge
import com.nuvio.tvos.TvosMetaDetails
import com.nuvio.tvos.TvosPlayAction
import com.nuvio.tvos.TvosStreamTarget

/**
 * The hero text block of the Apple TV app's Details (docs/tv-look-spec.md section 3, reference
 * `14-details-d1.png`): logo, metadata line, overview, tech line, button row with the resume bar
 * hanging under Play, and "Starring" on the right at the fourth grid column. The block is bottom
 * aligned on the button row; the caller places it.
 *
 * @param onInfo the info circle's action; no circle when null.
 */
@Composable
internal fun DetailsHero(
    details: TvosMetaDetails,
    logo: ImageRequest?,
    playFocus: FocusRequester,
    onOpen: (TvosStreamTarget) -> Unit,
    onInfo: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomStart) {
        Column {
            TitleArtwork(name = details.name, logo = logo)
            Spacer(modifier = Modifier.height(LOGO_GAP))
            MetadataRow(facts = heroMetadata(details), ageRating = details.ageRating)
            details.overview?.let { overview ->
                Spacer(modifier = Modifier.height(OVERVIEW_GAP))
                Text(
                    text = overview,
                    style = TvType.caption2,
                    color = TvColors.textSecondary,
                    maxLines = OVERVIEW_LINES,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = OVERVIEW_WIDTH).testTag(DETAILS_OVERVIEW_TAG)
                )
            }
            Spacer(modifier = Modifier.height(TECH_GAP))
            TechLine(facts = techFacts(details), imdbRating = details.imdbRating)
            Spacer(modifier = Modifier.height(BUTTONS_GAP))
            ButtonRow(action = details.primaryAction, playFocus = playFocus, onOpen = onOpen, onInfo = onInfo)
        }
        CreditsBlock(
            starring = starringNames(details.cast),
            director = directorNames(details.directors),
            modifier = Modifier.padding(start = TvGrid.columnStart(columns = 4, index = 3)).width(TvGrid.itemWidth(4))
        )
    }
}

/** The logo, or the title while the logo loads and when there is none (tvOS TitleArtwork). */
@Composable
internal fun TitleArtwork(
    name: String,
    logo: ImageRequest?,
    modifier: Modifier = Modifier,
    maxWidth: Dp = LOGO_MAX_WIDTH,
    maxHeight: Dp = LOGO_MAX_HEIGHT,
    alignment: Alignment = Alignment.BottomStart
) {
    var showsLogo by remember(logo) { mutableStateOf(false) }
    Box(modifier = modifier.semantics { contentDescription = name }, contentAlignment = alignment) {
        if (!showsLogo) {
            Text(
                text = name,
                style = TITLE_STYLE,
                color = TvColors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = TITLE_MAX_WIDTH)
            )
        }
        if (logo != null) {
            AsyncImage(
                model = logo,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alignment = alignment,
                modifier = Modifier.sizeIn(maxWidth = maxWidth, maxHeight = maxHeight),
                onSuccess = { showsLogo = true },
                onError = { showsLogo = false }
            )
        }
    }
}

/** "TV Show · Drama · Crime" and the outlined age rating. */
@Composable
private fun MetadataRow(facts: List<String>, ageRating: String?) {
    if (facts.isEmpty() && ageRating == null) return
    Row(horizontalArrangement = Arrangement.spacedBy(BADGE_GAP), verticalAlignment = Alignment.CenterVertically) {
        if (facts.isNotEmpty()) {
            Text(text = facts.joinToString(DOT), style = TvType.caption1, color = TvColors.textPrimary, maxLines = 1)
        }
        ageRating?.let { rating -> RatingBadge(rating) }
    }
}

/** "2008 · 47m" and the IMDb rating, where the reference shows its tech badges. */
@Composable
private fun TechLine(facts: List<String>, imdbRating: String?) {
    if (facts.isEmpty() && imdbRating == null) return
    CompositionLocalProvider(LocalContentColor provides TECH_COLOR) {
        ProvideTextStyle(TvType.caption1) {
            Row(horizontalArrangement = Arrangement.spacedBy(BADGE_GAP), verticalAlignment = Alignment.CenterVertically) {
                if (facts.isNotEmpty()) Text(text = facts.joinToString(DOT), maxLines = 1)
                imdbRating?.let { rating -> ImdbBadge(rating) }
            }
        }
    }
}

@Composable
internal fun RatingBadge(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = BADGE_STYLE,
        color = TvColors.textPrimary,
        maxLines = 1,
        modifier = modifier
            .border(BADGE_STROKE, TvColors.textPrimary, BADGE_SHAPE)
            .padding(horizontal = BADGE_PADDING_H)
    )
}

/** Play (white capsule when focused) and the circular info button, 13 dp apart. */
@Composable
private fun ButtonRow(action: TvosPlayAction?, playFocus: FocusRequester, onOpen: (TvosStreamTarget) -> Unit, onInfo: (() -> Unit)?) {
    Row(horizontalArrangement = Arrangement.spacedBy(BUTTON_GAP), verticalAlignment = Alignment.Top) {
        action?.let { PlayAction(it, playFocus, onOpen) }
        onInfo?.let { InfoButton(onClick = it) }
    }
}

/**
 * The play button and, for a part watched title, the bar and label under it (AC D2). The resume
 * block hangs below the button without taking height, where the reference shows its price
 * caption, so the button row stays on its line.
 */
@Composable
private fun PlayAction(action: TvosPlayAction, playFocus: FocusRequester, onOpen: (TvosStreamTarget) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()
    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
        Button(
            onClick = { onOpen(action.target) },
            colors = controlColors(),
            scale = ButtonDefaults.scale(focusedScale = Focus.buttonScale),
            shape = ButtonDefaults.shape(shape = TvShapes.pill),
            contentPadding = PaddingValues(horizontal = PLAY_PADDING_H),
            interactionSource = interaction,
            modifier = Modifier
                .height(BUTTON_HEIGHT)
                .widthIn(min = PLAY_MIN_WIDTH)
                .glassEdge(TvShapes.pill, visible = !isFocused)
                .focusRequester(playFocus)
                .testTag(DETAILS_PLAY_TAG)
        ) {
            Icon(painter = painterResource(R.drawable.ic_details_play), contentDescription = null, modifier = Modifier.size(PLAY_ICON))
            Spacer(modifier = Modifier.width(PLAY_ICON_GAP))
            Text(text = action.label, style = PLAY_LABEL_STYLE, maxLines = 1)
        }
        resumeProgress(action)?.let { resume ->
            ResumeProgressView(resume, Modifier.hangingBelow(RESUME_GAP))
        }
    }
}

@Composable
private fun InfoButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.colors(
            containerColor = CONTROL_FILL,
            contentColor = TvColors.textPrimary,
            focusedContainerColor = TvColors.focusedFill,
            focusedContentColor = TvColors.focusedContent,
            pressedContainerColor = TvColors.focusedFill,
            pressedContentColor = TvColors.focusedContent
        ),
        scale = IconButtonDefaults.scale(focusedScale = Focus.buttonScale),
        shape = IconButtonDefaults.shape(shape = CircleShape),
        interactionSource = interaction,
        modifier = Modifier
            .size(BUTTON_HEIGHT)
            .glassEdge(CircleShape, visible = !isFocused)
            .semantics { contentDescription = INFO_DESCRIPTION }
            .testTag(DETAILS_INFO_BUTTON_TAG)
    ) {
        Icon(painter = painterResource(R.drawable.ic_details_info), contentDescription = null, modifier = Modifier.size(INFO_ICON))
    }
}

/** Dark translucent at rest (one flat layer plus the glass edge), solid white when focused. */
@Composable
private fun controlColors() = ButtonDefaults.colors(
    containerColor = CONTROL_FILL,
    contentColor = TvColors.textPrimary,
    focusedContainerColor = TvColors.focusedFill,
    focusedContentColor = TvColors.focusedContent,
    pressedContainerColor = TvColors.focusedFill,
    pressedContentColor = TvColors.focusedContent
)

@Composable
private fun ResumeProgressView(resume: ResumeProgress, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(RESUME_LINE_GAP)) {
        WatchProgressBar(
            fraction = resume.fraction,
            track = RESUME_TRACK,
            height = RESUME_BAR_HEIGHT,
            modifier = Modifier.fillMaxWidth().testTag(DETAILS_RESUME_BAR_TAG)
        )
        Text(
            text = resume.label,
            style = TvType.caption2.copy(fontWeight = FontWeight.SemiBold),
            color = TvColors.textSecondary,
            maxLines = 1,
            modifier = Modifier.testTag(DETAILS_RESUME_LABEL_TAG)
        )
    }
}

/** "Starring" and "Director", secondary labels before primary names (reference: right of the buttons). */
@Composable
private fun CreditsBlock(starring: String?, director: String?, modifier: Modifier = Modifier) {
    if (starring == null && director == null) return
    Column(modifier = modifier) {
        starring?.let { names -> CreditLine(label = STARRING_LABEL, names = names, maxLines = STARRING_LINES) }
        director?.let { names -> CreditLine(label = DIRECTOR_LABEL, names = names, maxLines = 1) }
    }
}

@Composable
private fun CreditLine(label: String, names: String, maxLines: Int) {
    val text = remember(label, names) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = TvColors.textSecondary)) { append(label) }
            append(' ')
            append(names)
        }
    }
    Text(text = text, style = TvType.caption1, color = TvColors.textPrimary, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

/** Lays the content out [gap] below its slot and reports no height, so the row above keeps its line. */
private fun Modifier.hangingBelow(gap: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minHeight = 0))
    layout(placeable.width, 0) { placeable.place(0, gap.roundToPx()) }
}

const val DETAILS_PLAY_TAG = "details-play"
const val DETAILS_INFO_BUTTON_TAG = "details-info-button"
const val DETAILS_RESUME_BAR_TAG = "details-resume-bar"
const val DETAILS_RESUME_LABEL_TAG = "details-resume-label"
const val DETAILS_OVERVIEW_TAG = "details-overview"

private const val DOT = " · "
private const val OVERVIEW_LINES = 4
private const val STARRING_LINES = 2
private const val STARRING_LABEL = "Starring"
private const val DIRECTOR_LABEL = "Director"
private const val INFO_DESCRIPTION = "About"

// Measured on 14-details-d1.png (pt / 2): logo 474 to 585, metadata 603 to 643, overview from
// 658 (4 lines of 30), tech line 803 to 825, buttons 861 to 926, circles 64 pt, 26 pt apart.
private val LOGO_MAX_WIDTH = 210.dp
private val LOGO_MAX_HEIGHT = 55.dp
private val TITLE_MAX_WIDTH = 430.dp
private val LOGO_GAP = 9.dp
private val OVERVIEW_GAP = 9.dp
private val OVERVIEW_WIDTH = 330.dp
private val TECH_GAP = 10.dp
private val BUTTONS_GAP = 15.dp
private val BUTTON_HEIGHT = 32.dp
private val BUTTON_GAP = 13.dp
private val BADGE_GAP = 6.dp

// tvOS app literals (DetailsHeroView.swift): play min width 300 pt, icon 30 pt, resume 18 / 12 / 10 pt.
private val PLAY_MIN_WIDTH = 150.dp
private val PLAY_PADDING_H = 15.dp
private val PLAY_ICON = 14.dp
private val PLAY_ICON_GAP = 6.dp
private val INFO_ICON = 16.dp
private val RESUME_GAP = 9.dp
private val RESUME_LINE_GAP = 6.dp
private val RESUME_BAR_HEIGHT = 5.dp
private val RESUME_TRACK = Color.White.copy(alpha = 0.28f)

private val PLAY_LABEL_STYLE = TvType.body.copy(fontWeight = FontWeight.SemiBold)

// Title 1 (76 pt bold) when there is no logo.
private val TITLE_STYLE = TvType.body.copy(fontSize = 38.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold)

// Measured: unfocused buttons darken the art under them by about half (13-details-top.png).
private val CONTROL_FILL = Color.Black.copy(alpha = 0.5f)

// Measured: "2022 · 57m" is #BFBFBF on black (14-details-d1.png).
private val TECH_COLOR = Color.White.copy(alpha = 0.75f)

// Outlined rating badge; 11 sp is the badge size floor of the spec.
private val BADGE_STYLE = TvType.overline.copy(fontWeight = FontWeight.Bold)
private val BADGE_STROKE = 1.dp
private val BADGE_SHAPE = RoundedCornerShape(3.dp)
private val BADGE_PADDING_H = 3.dp
