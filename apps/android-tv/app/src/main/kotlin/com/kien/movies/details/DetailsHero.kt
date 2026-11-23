package com.kien.movies.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.kien.movies.R
import com.kien.movies.design.GlassStyle
import com.kien.movies.design.MetadataLine
import com.kien.movies.design.Palette
import com.kien.movies.design.TextStyles
import com.nuvio.tvos.TvosMetaDetails
import com.nuvio.tvos.TvosPlayAction
import com.nuvio.tvos.TvosStreamTarget

/**
 * Logo or title, the play action with its resume bar, and the summary (tvOS DetailsHeroView.swift).
 * The play label and the resume label come from Kotlin as they are (AC D1, D2).
 */
@Composable
internal fun DetailsHero(
    details: TvosMetaDetails,
    playFocus: FocusRequester,
    onPlayFocusChanged: (Boolean) -> Unit,
    onOpen: (TvosStreamTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HERO_SPACING_40PT, Alignment.Bottom)
    ) {
        TitleArtwork(name = details.name, logoUrl = details.logoUrl)
        Row(horizontalArrangement = Arrangement.spacedBy(ACTIONS_GAP_64PT), verticalAlignment = Alignment.Top) {
            details.primaryAction?.let { action ->
                PlayAction(action, playFocus, onPlayFocusChanged, onOpen)
            }
            Summary(details)
        }
    }
}

/** The logo, or the title while the logo loads and when there is none (tvOS TitleArtwork). */
@Composable
private fun TitleArtwork(name: String, logoUrl: String?) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var showsLogo by remember(logoUrl) { mutableStateOf(false) }
    val request = remember(logoUrl, density) {
        logoUrl?.let { url ->
            val (width, height) = with(density) { LOGO_MAX_WIDTH_720PT.roundToPx() to LOGO_MAX_HEIGHT_220PT.roundToPx() }
            ImageRequest.Builder(context).data(url).size(width, height).build()
        }
    }
    Box(modifier = Modifier.semantics { contentDescription = name }, contentAlignment = Alignment.BottomStart) {
        if (!showsLogo) {
            Text(
                text = name,
                style = TextStyles.detailsTitle,
                color = Palette.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = TITLE_MAX_WIDTH_1100PT)
            )
        }
        if (request != null) {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomStart,
                modifier = Modifier.sizeIn(maxWidth = LOGO_MAX_WIDTH_720PT, maxHeight = LOGO_MAX_HEIGHT_220PT),
                onSuccess = { showsLogo = true },
                onError = { showsLogo = false }
            )
        }
    }
}

/** The play button and, for a part watched title, the bar and label under it (AC D2). */
@Composable
private fun PlayAction(
    action: TvosPlayAction,
    playFocus: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    onOpen: (TvosStreamTarget) -> Unit
) {
    // tvOS fixes the column at its ideal width: the wider of the button and the resume label.
    Column(modifier = Modifier.width(IntrinsicSize.Max), verticalArrangement = Arrangement.spacedBy(ACTION_RESUME_GAP_18PT)) {
        Button(
            onClick = { onOpen(action.target) },
            colors = GlassStyle.prominentButtonColors(),
            scale = GlassStyle.buttonScale(),
            shape = GlassStyle.buttonShape(),
            modifier = Modifier
                .widthIn(min = PLAY_MIN_WIDTH_300PT)
                .focusRequester(playFocus)
                .onFocusChanged { state -> onFocusChanged(state.isFocused) }
                .testTag(DETAILS_PLAY_TAG)
        ) {
            Icon(painter = painterResource(R.drawable.ic_details_play), contentDescription = null, modifier = Modifier.size(PLAY_ICON_30PT))
            Spacer(modifier = Modifier.width(PLAY_ICON_GAP))
            Text(text = action.label, style = PLAY_LABEL_STYLE, maxLines = 1)
        }
        resumeProgress(action)?.let { resume -> ResumeProgressView(resume) }
    }
}

@Composable
private fun ResumeProgressView(resume: ResumeProgress) {
    Column(verticalArrangement = Arrangement.spacedBy(RESUME_GAP_12PT)) {
        WatchProgressBar(
            fraction = resume.fraction,
            track = RESUME_TRACK,
            height = RESUME_BAR_HEIGHT_10PT,
            modifier = Modifier.fillMaxWidth().testTag(DETAILS_RESUME_BAR_TAG)
        )
        Text(
            text = resume.label,
            style = RESUME_LABEL_STYLE,
            color = RESUME_LABEL_COLOR,
            maxLines = 1,
            modifier = Modifier.testTag(DETAILS_RESUME_LABEL_TAG)
        )
    }
}

/** Overview, facts with the rating badges, and the first three cast names (AC D1). */
@Composable
private fun Summary(details: TvosMetaDetails) {
    Column(modifier = Modifier.widthIn(max = DESCRIPTION_WIDTH_820PT), verticalArrangement = Arrangement.spacedBy(SUMMARY_GAP_16PT)) {
        details.overview?.let { overview ->
            Text(
                text = overview,
                style = OVERVIEW_STYLE,
                color = OVERVIEW_COLOR,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag(DETAILS_OVERVIEW_TAG)
            )
        }
        CompositionLocalProvider(LocalContentColor provides Palette.textSecondary) {
            ProvideTextStyle(METADATA_STYLE) {
                MetadataLine(facts = heroFacts(details), ageRating = details.ageRating, imdbRating = details.imdbRating)
            }
        }
        starring(details.cast)?.let { names ->
            Text(text = names, style = TextStyles.small, color = STARRING_COLOR, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

const val DETAILS_PLAY_TAG = "details-play"
const val DETAILS_RESUME_BAR_TAG = "details-resume-bar"
const val DETAILS_RESUME_LABEL_TAG = "details-resume-label"
const val DETAILS_OVERVIEW_TAG = "details-overview"

// tvOS literals, pt / 2.
private val HERO_SPACING_40PT = 20.dp
private val ACTIONS_GAP_64PT = 32.dp
private val LOGO_MAX_WIDTH_720PT = 360.dp
private val LOGO_MAX_HEIGHT_220PT = 110.dp
private val TITLE_MAX_WIDTH_1100PT = 550.dp
private val PLAY_MIN_WIDTH_300PT = 150.dp
private val PLAY_ICON_30PT = 15.dp
private val PLAY_ICON_GAP = 6.dp
private val PLAY_LABEL_STYLE = TextStyles.button.copy(fontWeight = FontWeight.SemiBold)
private val ACTION_RESUME_GAP_18PT = 9.dp
private val RESUME_GAP_12PT = 6.dp
private val RESUME_BAR_HEIGHT_10PT = 5.dp
private val RESUME_TRACK = Color.White.copy(alpha = 0.28f)
private val RESUME_LABEL_STYLE = TextStyles.small.copy(fontWeight = FontWeight.SemiBold)
private val RESUME_LABEL_COLOR = Color.White.copy(alpha = 0.8f)
private val DESCRIPTION_WIDTH_820PT = 410.dp
private val SUMMARY_GAP_16PT = 8.dp
private val OVERVIEW_STYLE = TextStyles.body.copy(fontSize = 14.sp, lineHeight = 19.sp)
private val OVERVIEW_COLOR = Color.White.copy(alpha = 0.9f)
private val METADATA_STYLE = TextStyles.small.copy(fontWeight = FontWeight.Medium)
private val STARRING_COLOR = Color.White.copy(alpha = 0.55f)
