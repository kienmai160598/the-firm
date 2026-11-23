package com.kien.movies.ui.components

import android.graphics.ColorSpace
import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.colorSpace
import coil3.request.crossfade
import com.kien.movies.R
import com.kien.movies.domain.model.MetaPreview
import com.kien.movies.ui.components.expressive.M3eShapes
import com.kien.movies.ui.theme.ExpressiveFocus
import com.kien.movies.ui.theme.MoviesTheme
import com.kien.movies.ui.theme.bodyFloor
import com.kien.movies.ui.theme.expressiveFocusScale
import com.kien.movies.ui.theme.rememberMetaFloor
import com.kien.movies.ui.util.LocalRecompositionHighlighterEnabled
import com.kien.movies.ui.util.StableList
import com.kien.movies.ui.util.contentTextDirection
import com.kien.movies.ui.util.formatHeroRuntime
import com.kien.movies.ui.util.localizedContentType
import com.kien.movies.ui.util.localizedGenreLabel
import com.kien.movies.ui.util.recompositionHighlighter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

private const val AUTO_ADVANCE_INTERVAL_MS = 10000L
private val YEAR_REGEX = Regex("""\b\d{4}\b""")

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HeroCarousel(
    items: StableList<MetaPreview>,
    onItemClick: (MetaPreview) -> Unit,
    onItemFocus: (MetaPreview) -> Unit = {},
    onActiveItemChanged: (MetaPreview) -> Unit = {},
    focusRequester: FocusRequester? = null,
    showImdbRatings: Boolean = true,
    showBackdrop: Boolean = true,
    fullWidth: Dp = Dp.Unspecified,
    initialActiveIndex: Int = 0,
    mdbListShowOnHero: Boolean = false,
    mdbListRatingOrder: List<String> = com.kien.movies.domain.model.MDBListSettings.DEFAULT_RATING_ORDER,
    descriptionAutoScrollEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val currentOnItemClick by rememberUpdatedState(onItemClick)
    val currentOnItemFocus by rememberUpdatedState(onItemFocus)
    val currentOnActiveItemChanged by rememberUpdatedState(onActiveItemChanged)
    var activeIndex by remember {
        mutableIntStateOf(initialActiveIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)))
    }
    var isFocused by remember { mutableStateOf(false) }
    // Slide whose description is still on its first scroll pass, or -1.
    var descriptionScrollHoldIndex by remember { mutableIntStateOf(-1) }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    LaunchedEffect(activeIndex, isFocused) {
        if (!isFocused) return@LaunchedEffect
        items.getOrNull(activeIndex)?.let { currentOnItemFocus(it) }
    }

    LaunchedEffect(activeIndex, items) {
        items.getOrNull(activeIndex)?.let { currentOnActiveItemChanged(it) }
    }

    // Auto-advance when not focused: delay first advance to 20s so initial GPU load settles
    LaunchedEffect(isFocused, items.size) {
        if (items.size <= 1) return@LaunchedEffect
        delay(AUTO_ADVANCE_INTERVAL_MS * 2) // 20s before first advance
        while (true) {
            delay(AUTO_ADVANCE_INTERVAL_MS)
            // Let a long description finish scrolling before the slide is replaced.
            snapshotFlow { descriptionScrollHoldIndex == activeIndex }.first { !it }
            if (!isFocused) {
                activeIndex = (activeIndex + 1) % items.size
            }
        }
    }

    Box(
        modifier = modifier
            .then(
                if (fullWidth != Dp.Unspecified) {
                    Modifier.requiredWidth(fullWidth)
                } else {
                    Modifier.fillMaxWidth()
                }
            )
            .height(400.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged {
                isFocused = it.hasFocus || it.isFocused
            }
            .semantics { this.contentDescription = items.getOrNull(activeIndex)?.name.orEmpty() }
            .focusable()
            .semantics {
                role = Role.Button
                contentDescription = items[activeIndex].name
                onClick {
                    currentOnItemClick(items[activeIndex])
                    true
                }
            }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (isRtl) {
                                if (activeIndex < items.size - 1) {
                                    activeIndex++
                                    true
                                } else {
                                    false
                                }
                            } else {
                                if (activeIndex > 0) {
                                    activeIndex--
                                    true
                                } else {
                                    false
                                }
                            }
                        }

                        Key.DirectionRight -> {
                            if (isRtl) {
                                if (activeIndex > 0) {
                                    activeIndex--
                                    true
                                } else {
                                    false
                                }
                            } else {
                                if (activeIndex < items.size - 1) {
                                    activeIndex++
                                    true
                                } else {
                                    false
                                }
                            }
                        }

                        else -> false
                    }
                } else if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)
                ) {
                    currentOnItemClick(items[activeIndex])
                    true
                } else {
                    false
                }
            }
    ) {
        // Crossfade between slides
        Crossfade(
            targetState = activeIndex,
            animationSpec = tween(300),
            label = "heroSlide"
        ) { index ->
            val item = items.getOrNull(index) ?: return@Crossfade
            HeroCarouselSlide(
                item = item,
                heroFocused = isFocused,
                showImdbRatings = showImdbRatings,
                showBackdrop = showBackdrop,
                mdbListShowOnHero = mdbListShowOnHero,
                mdbListRatingOrder = mdbListRatingOrder,
                descriptionAutoScrollEnabled = descriptionAutoScrollEnabled,
                onDescriptionScrollHoldChanged = { hold ->
                    // A slide can only clear its own hold, so the outgoing slide in the
                    // crossfade can't release the new one, and a slide without a
                    // description never blocks the advance.
                    when {
                        hold -> descriptionScrollHoldIndex = index
                        descriptionScrollHoldIndex == index -> descriptionScrollHoldIndex = -1
                    }
                }
            )
        }

        // Indicator dots: optimized to minimize recompositions and layout passes
        val dotColorInactive = remember { Color.White.copy(alpha = 0.3f) }
        val dotShape = remember { RoundedCornerShape(percent = 50) }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = MoviesTheme.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm)
        ) {
            repeat(items.size) { index ->
                val isActive = index == activeIndex
                val useGradient = isActive
                val dotColor = when {
                    isActive -> null

                    // use gradient brush
                    else -> dotColorInactive
                }
                val dotWidth = when {
                    isActive -> MoviesTheme.spacing.xxl
                    else -> MoviesTheme.spacing.md
                }
                val dotHeight = if (isActive) 6.dp else MoviesTheme.spacing.xs

                Box(
                    modifier = Modifier
                        .size(width = dotWidth, height = dotHeight)
                        .clip(dotShape)
                        .then(
                            if (useGradient) {
                                Modifier.background(Color.White)
                            } else {
                                Modifier.background(dotColor!!)
                            }
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HeroCarouselSlide(
    item: MetaPreview,
    heroFocused: Boolean,
    showImdbRatings: Boolean,
    showBackdrop: Boolean,
    mdbListShowOnHero: Boolean = false,
    mdbListRatingOrder: List<String> = com.kien.movies.domain.model.MDBListSettings.DEFAULT_RATING_ORDER,
    descriptionAutoScrollEnabled: Boolean = false,
    onDescriptionScrollHoldChanged: (Boolean) -> Unit = {}
) {
    val highlighterEnabled = LocalRecompositionHighlighterEnabled.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val logoRequestWidthPx = remember(density) {
        with(density) { 220.dp.roundToPx() }.coerceAtLeast(1)
    }
    val logoRequestHeightPx =
        remember(density) { with(density) { 100.dp.roundToPx() }.coerceAtLeast(1) }

    val logoModel = remember(context, item.logo, logoRequestWidthPx, logoRequestHeightPx) {
        item.logo?.let {
            ImageRequest.Builder(context)
                .data(it)
                .crossfade(true)
                .size(width = logoRequestWidthPx, height = logoRequestHeightPx)
                .build()
        }
    }
    var logoLoadFailed by remember(item.logo) { mutableStateOf(false) }
    val showLogo = !item.logo.isNullOrBlank() && !logoLoadFailed
    val contentTypeText = remember(context, item.apiType) {
        localizedContentType(context, item.apiType).takeIf { it.isNotBlank() }
    }
    val primaryGenreText = remember(context, item.genres) {
        item.genres.firstOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let { localizedGenreLabel(context, it) }
    }
    val runtimeText = remember(item.runtime) { formatHeroRuntime(item.runtime) }
    val releaseYear = remember(item.releaseInfo) {
        item.releaseInfo?.let { releaseInfo ->
            YEAR_REGEX.find(releaseInfo)?.value ?: releaseInfo.split("-").firstOrNull()
        }?.trim()?.takeIf { it.isNotEmpty() }
    }
    val leadingMetaText = remember(contentTypeText, primaryGenreText) {
        listOfNotNull(contentTypeText, primaryGenreText).joinToString(separator = " • ")
    }
    val trailingMetadata = remember(runtimeText, releaseYear) {
        listOfNotNull(runtimeText, releaseYear)
    }
    val ratingText = remember(item.imdbRating, showImdbRatings) {
        item.imdbRating
            ?.takeIf { showImdbRatings }
            ?.let { String.format("%.1f", it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(if (highlighterEnabled) Modifier.recompositionHighlighter() else Modifier)
    ) {
        if (showBackdrop) {
            HeroCarouselBackdrop(
                item = item,
                fullPage = false,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = MoviesTheme.spacing.xxxl,
                    bottom = MoviesTheme.spacing.xxxl,
                    end = MoviesTheme.spacing.xxxl
                )
                .fillMaxWidth(0.42f),
            verticalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm)
        ) {
            if (showLogo) {
                AsyncImage(
                    model = logoModel,
                    contentDescription = item.name,
                    onError = { logoLoadFailed = true },
                    modifier = Modifier
                        .height(100.dp)
                        .widthIn(min = 100.dp, max = 220.dp)
                        .fillMaxWidth(),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart
                )
            } else {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MoviesTheme.colors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (leadingMetaText.isNotBlank() || trailingMetadata.isNotEmpty() ||
                ratingText != null
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val hasTrailingMeta = trailingMetadata.isNotEmpty() || ratingText != null
                    if (leadingMetaText.isNotBlank()) {
                        Text(
                            text = leadingMetaText,
                            style = MaterialTheme.typography.labelMedium.rememberMetaFloor(),
                            color = MoviesTheme.colors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = if (hasTrailingMeta) {
                                Modifier.weight(1f, fill = false)
                            } else {
                                Modifier
                            }
                        )
                    }
                    if (hasTrailingMeta) {
                        if (leadingMetaText.isNotBlank()) HeroCarouselMetaDivider()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm)
                        ) {
                            trailingMetadata.forEachIndexed { index, value ->
                                if (index > 0) HeroCarouselMetaDivider()
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.labelMedium
                                        .rememberMetaFloor(),
                                    color = MoviesTheme.colors.TextSecondary,
                                    maxLines = 1
                                )
                            }
                            if (ratingText != null) {
                                val mdbRatings = item.mdbListRatings
                                if (mdbListShowOnHero && mdbRatings != null &&
                                    !mdbRatings.isEmpty()
                                ) {
                                } else {
                                    if (trailingMetadata.isNotEmpty()) HeroCarouselMetaDivider()
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(
                                            MoviesTheme.spacing.xs
                                        )
                                    ) {
                                        ImdbRatingSourceLabel(
                                            logoModifier = Modifier.size(MoviesTheme.spacing.xl),
                                            textStyle = MaterialTheme.typography.labelMedium
                                                .rememberMetaFloor(),
                                            textColor = MoviesTheme.colors.TextSecondary
                                        )
                                        Text(
                                            text = ratingText,
                                            style = MaterialTheme.typography.labelMedium
                                                .rememberMetaFloor(),
                                            color = MoviesTheme.colors.TextSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (mdbListShowOnHero && item.mdbListRatings != null &&
                !item.mdbListRatings.isEmpty()
            ) {
                MDBListRatingsRow(
                    ratings = item.mdbListRatings,
                    order = mdbListRatingOrder,
                    maxItems = 6
                )
            }

            item.description?.takeIf { it.isNotBlank() }?.let { description ->
                AutoScrollingText(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDirection = description.contentTextDirection()
                    ),
                    color = MoviesTheme.colors.TextPrimary,
                    maxLines = 4,
                    autoScrollEnabled = descriptionAutoScrollEnabled,
                    onScrollHoldChanged = onDescriptionScrollHoldChanged
                )
            }

            HeroDetailsHint(focused = heroFocused)
        }
    }
}

/**
 * The billboard's action on the box build, drawn but not focusable: the hero itself takes focus
 * and opens details on select, so the capsule turns white with it as a tvOS button would.
 */
@Composable
private fun HeroDetailsHint(focused: Boolean) {
    val container = ExpressiveFocus.frostedContainerColor(focused = focused)
    val content = ExpressiveFocus.frostedContentColor(focused = focused)
    Row(
        modifier = Modifier
            .padding(top = MoviesTheme.spacing.sm)
            .expressiveFocusScale(focused)
            .background(container, M3eShapes.capsule)
            .padding(horizontal = MoviesTheme.spacing.xl, vertical = MoviesTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MoviesTheme.spacing.sm)
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(MoviesTheme.spacing.xl)
        )
        Text(
            text = stringResource(R.string.cw_action_go_to_details),
            style = MoviesTheme.textStyles.button.bodyFloor(),
            color = content,
            maxLines = 1
        )
    }
}

@Composable
private fun HeroCarouselMetaDivider() {
    Box(
        modifier = Modifier
            .size(MoviesTheme.spacing.xs.coerceAtLeast(MoviesTheme.spacing.xxs))
            .clip(RoundedCornerShape(percent = 50))
            .background(MoviesTheme.colors.TextTertiary.copy(alpha = 0.78f))
    )
}

@Composable
internal fun HeroCarouselBackdrop(
    item: MetaPreview,
    fullPage: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val requestWidthPx = remember(configuration.screenWidthDp, density) {
        with(density) { configuration.screenWidthDp.dp.roundToPx() }.coerceAtLeast(1)
    }
    val requestHeightPx = remember(configuration.screenHeightDp, density, fullPage) {
        val height = if (fullPage) configuration.screenHeightDp.dp else 400.dp
        with(density) { height.roundToPx() }.coerceAtLeast(1)
    }
    val backdropUrl = item.backdropUrl
    val backgroundModel =
        remember(context, backdropUrl, requestWidthPx, requestHeightPx, fullPage) {
            ImageRequest.Builder(context)
                .data(backdropUrl)
                .crossfade(false)
                .size(width = requestWidthPx, height = requestHeightPx)
                .apply {
                    if (fullPage && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        colorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
                    }
                }
                .build()
        }
    val bgColor = MoviesTheme.colors.Background
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Box(
        modifier = modifier
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithCache {
                val bottomStartFraction = if (fullPage) 0.55f else 0.30f
                val leftEndFraction = if (fullPage) 0.66f else 0.72f
                val bottomStartY = size.height * bottomStartFraction
                val leftEndX = size.width * leftEndFraction
                val bottomGradient = Brush.verticalGradient(
                    colorStops = if (fullPage) {
                        arrayOf(
                            0.0f to Color.Transparent,
                            0.4222f to bgColor.copy(alpha = 0.32f),
                            0.7778f to bgColor.copy(alpha = 0.62f),
                            1.0f to bgColor.copy(alpha = 0.78f)
                        )
                    } else {
                        arrayOf(
                            0.0f to Color.Transparent,
                            0.4286f to bgColor.copy(alpha = 0.5f),
                            0.7143f to bgColor.copy(alpha = 0.85f),
                            1.0f to bgColor
                        )
                    },
                    startY = bottomStartY,
                    endY = size.height
                )
                val leftGradient = Brush.horizontalGradient(
                    colorStops = if (fullPage) {
                        arrayOf(
                            0.0f to bgColor.copy(alpha = 0.98f),
                            0.2424f to bgColor.copy(alpha = 0.88f),
                            0.5152f to bgColor.copy(alpha = 0.56f),
                            0.7879f to bgColor.copy(alpha = 0.20f),
                            1.0f to Color.Transparent
                        )
                    } else {
                        arrayOf(
                            0.0f to bgColor.copy(alpha = 0.98f),
                            0.2222f to bgColor.copy(alpha = 0.88f),
                            0.4722f to bgColor.copy(alpha = 0.56f),
                            0.7778f to bgColor.copy(alpha = 0.20f),
                            1.0f to Color.Transparent
                        )
                    },
                    startX = if (isRtl) size.width else 0f,
                    endX = if (isRtl) size.width - leftEndX else leftEndX
                )
                onDrawWithContent {
                    drawContent()
                    drawRect(
                        brush = bottomGradient,
                        topLeft = Offset(0f, bottomStartY),
                        size = Size(size.width, size.height - bottomStartY)
                    )
                    drawRect(
                        brush = leftGradient,
                        topLeft = Offset(if (isRtl) size.width - leftEndX else 0f, 0f),
                        size = Size(leftEndX, size.height)
                    )
                }
            }
    ) {
        AsyncImage(
            model = backgroundModel,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter
        )
    }
}
