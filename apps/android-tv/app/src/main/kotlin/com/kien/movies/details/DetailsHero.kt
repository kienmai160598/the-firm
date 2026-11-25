package com.kien.movies.details

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.kien.movies.R
import com.kien.movies.design.Focus
import com.kien.movies.design.MetadataLine
import com.kien.movies.design.TitleLogo
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvShapes
import com.kien.movies.design.TvType
import com.kien.movies.design.focusLift
import com.kien.movies.design.glassEdge
import com.nuvio.tvos.TvosMetaDetails
import com.nuvio.tvos.TvosPlayAction
import com.nuvio.tvos.TvosStreamTarget

/**
 * The hero of Details, as tvOS draws it (DetailsHeroView.swift): the logo or title, then a row
 * with the play button (and, for a part watched title, the resume bar and label under it) on the
 * left and the summary on the right: overview and facts line. Bottom aligned; the caller places
 * it. The cast is in Cast & Crew below, so the summary names nobody.
 */
@Composable
internal fun DetailsHero(
    details: TvosMetaDetails,
    logo: ImageRequest?,
    playFocus: FocusRequester,
    onOpen: (TvosStreamTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(LOGO_GAP)) {
        TitleLogo(name = details.name, logo = logo, box = LOGO_BOX, textStyle = TITLE_STYLE, textMaxWidth = TITLE_MAX_WIDTH)
        Row(horizontalArrangement = Arrangement.spacedBy(ACTIONS_GAP), verticalAlignment = Alignment.Top) {
            details.primaryAction?.let { action -> PlayAction(action, playFocus, onOpen) }
            Summary(details)
        }
    }
}

/** Overview and facts line, at most 410 dp wide (tvOS `summary`). */
@Composable
private fun Summary(details: TvosMetaDetails) {
    Column(modifier = Modifier.widthIn(max = SUMMARY_WIDTH), verticalArrangement = Arrangement.spacedBy(SUMMARY_GAP)) {
        details.overview?.let { overview ->
            Text(
                text = overview,
                style = OVERVIEW_STYLE,
                color = OVERVIEW_COLOR,
                maxLines = OVERVIEW_LINES,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag(DETAILS_OVERVIEW_TAG)
            )
        }
        CompositionLocalProvider(LocalContentColor provides FACTS_COLOR) {
            ProvideTextStyle(FACTS_STYLE) {
                MetadataLine(facts = summaryFacts(details), ageRating = details.ageRating, imdbRating = details.imdbRating)
            }
        }
    }
}

/** The play button and, for a part watched title, the bar and label under it (AC D2). */
@Composable
private fun PlayAction(action: TvosPlayAction, playFocus: FocusRequester, onOpen: (TvosStreamTarget) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()
    Column(modifier = Modifier.width(IntrinsicSize.Max), verticalArrangement = Arrangement.spacedBy(RESUME_GAP)) {
        Button(
            onClick = { onOpen(action.target) },
            colors = controlColors(),
            scale = ButtonDefaults.scale(focusedScale = 1f),
            shape = ButtonDefaults.shape(shape = TvShapes.pill),
            contentPadding = PaddingValues(horizontal = PLAY_PADDING_H),
            interactionSource = interaction,
            modifier = Modifier
                .height(PLAY_HEIGHT)
                .widthIn(min = PLAY_MIN_WIDTH)
                .glassEdge(TvShapes.pill, visible = !isFocused)
                .focusRequester(playFocus)
                .testTag(DETAILS_PLAY_TAG)
                .focusLift(Focus.buttonScale)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(painter = painterResource(R.drawable.ic_details_play), contentDescription = null, modifier = Modifier.size(PLAY_ICON))
                Spacer(modifier = Modifier.width(PLAY_ICON_GAP))
                Text(text = action.label, style = PLAY_LABEL_STYLE, maxLines = 1)
            }
        }
        resumeProgress(action)?.let { resume -> ResumeProgressView(resume) }
    }
}

/** Dark translucent at rest (one flat layer plus the glass edge), solid white when focused. */
@Composable
internal fun controlColors() = ButtonDefaults.colors(
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

const val DETAILS_PLAY_TAG = "details-play"
const val DETAILS_RESUME_BAR_TAG = "details-resume-bar"
const val DETAILS_RESUME_LABEL_TAG = "details-resume-label"
const val DETAILS_OVERVIEW_TAG = "details-overview"

// tvOS app literals (DetailsHeroView.swift), pt / 2: logo up to 720 x 220, 40 between the logo and
// the row, 64 between the actions and the summary, summary 820 wide with 16 between its lines.
internal val LOGO_BOX = DpSize(360.dp, 110.dp)
private val TITLE_MAX_WIDTH = 550.dp
private val LOGO_GAP = 20.dp
private val ACTIONS_GAP = 32.dp
private val SUMMARY_WIDTH = 410.dp
private val SUMMARY_GAP = 8.dp
private const val OVERVIEW_LINES = 3

// Play: label 30 pt semibold, min width 300 pt; measured 367 x 81 pt on the simulator, the play
// glyph 25 x 28 pt (the vector's glyph fills 11 x 14 of its 24 box). Resume: 18 / 12 / 10 pt.
private val PLAY_MIN_WIDTH = 183.5.dp
private val PLAY_HEIGHT = 40.5.dp
private val PLAY_PADDING_H = 16.dp
private val PLAY_ICON = 24.dp
private val PLAY_ICON_GAP = 2.dp
private val RESUME_GAP = 9.dp
private val RESUME_LINE_GAP = 6.dp
private val RESUME_BAR_HEIGHT = 5.dp
private val RESUME_TRACK = Color.White.copy(alpha = 0.28f)
private val PLAY_LABEL_STYLE = TvType.body.copy(fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)

// Summary: overview 27 pt at 90% white, facts 23 pt medium at 60%.
private val OVERVIEW_STYLE = TvType.body.copy(fontSize = 13.5.sp, lineHeight = 16.5.sp, fontWeight = FontWeight.Normal)
private val OVERVIEW_COLOR = Color.White.copy(alpha = 0.9f)
private val FACTS_STYLE = TvType.body.copy(fontSize = 11.5.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
private val FACTS_COLOR = Color.White.copy(alpha = 0.6f)

// Title 1 (80 pt bold) when there is no logo.
private val TITLE_STYLE = TvType.body.copy(fontSize = 40.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold)

// Measured: an unfocused button darkens the art under it by about half (13-details-top.png).
private val CONTROL_FILL = Color.Black.copy(alpha = 0.5f)
