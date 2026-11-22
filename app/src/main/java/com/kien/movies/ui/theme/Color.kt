package com.kien.movies.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

@Immutable
data class MoviesSurfaceColors(
    val background: Color,
    val raised: Color,
    val card: Color,
    val default: Color,
    val variant: Color,
    val panel: Color,
    val overlay: Color,
    val field: Color,
    val menu: Color,
    val modal: Color,
    val playerOverlay: Color,
    val divider: Color
)

@Immutable
data class MoviesTextColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val disabled: Color,
    val inverse: Color,
    val onAccent: Color,
    val onOverlay: Color,
    val metadata: Color
)

@Immutable
data class MoviesFocusColors(
    val ring: Color,
    val background: Color,
    val content: Color,
    val accent: Color,
    val scrim: Color
)

@Immutable
data class MoviesSelectionColors(
    val background: Color,
    val foreground: Color,
    val border: Color,
    val mutedBackground: Color,
    val mutedForeground: Color
)

@Immutable
data class MoviesMediaColors(
    val heroScrim: Color,
    val imageScrim: Color,
    val posterFallback: Color,
    val videoControlsScrim: Color,
    val glassPanelTop: Color,
    val glassPanelMiddle: Color,
    val glassPanelBottom: Color,
    val glow: Color
)

@Immutable
data class MoviesStatusColors(
    val rating: Color,
    val error: Color,
    val warning: Color,
    val success: Color,
    val info: Color,
    val watched: Color,
    val unwatched: Color,
    val cached: Color,
    val torrent: Color,
    val premium: Color
)

@Immutable
data class MoviesDisabledColors(
    val container: Color,
    val content: Color,
    val border: Color,
    val overlay: Color
)

@Immutable
data class MoviesSourceColors(
    val trakt: Color,
    val tmdb: Color,
    val imdb: Color,
    val mdblist: Color
)

@Immutable
data class MoviesContrastPair(
    val foreground: Color,
    val background: Color
)

class MoviesColorScheme(
    palette: ThemeColorPalette,
    amoledMode: Boolean = false,
    amoledSurfacesMode: Boolean = false
) {
    private val pureBlack = MoviesPrimitives.black
    private val pureBlackSurfaces = amoledMode && amoledSurfacesMode

    val Background = if (amoledMode) pureBlack else palette.background
    val BackgroundElevated = if (pureBlackSurfaces) pureBlack else palette.backgroundElevated
    val BackgroundCard = if (pureBlackSurfaces) pureBlack else palette.backgroundCard
    val Surface = if (pureBlackSurfaces) pureBlack else palette.surface
    val SurfaceVariant = if (pureBlackSurfaces) pureBlack else palette.surfaceVariant
    val Panel = if (pureBlackSurfaces) pureBlack else palette.panel
    val Overlay = palette.overlay
    val Field = if (pureBlackSurfaces) pureBlack else palette.field
    val Menu = if (pureBlackSurfaces) pureBlack else palette.menu
    val Modal = if (pureBlackSurfaces) pureBlack else palette.modal
    val PlayerOverlay = palette.playerOverlay
    val Divider = palette.outlineVariant

    val Primary = palette.primary
    val PrimaryVariant = MoviesPrimitives.neutral650
    val OnPrimary = palette.onPrimary
    val Secondary = palette.secondary
    val SecondaryVariant = palette.secondaryVariant
    val OnSecondary = palette.onSecondary
    val OnSecondaryVariant = palette.onSecondaryVariant

    val TextPrimary = MoviesPrimitives.white
    val TextSecondary = palette.textSecondary
    val TextTertiary = palette.textTertiary
    val TextDisabled = MoviesPrimitives.neutral700
    val TextInverse = MoviesPrimitives.neutral925

    val FocusRing = palette.focusRing
    val FocusBackground = palette.focusBackground
    val FocusContent = MoviesPrimitives.white
    val FocusScrim = MoviesPrimitives.black.copy(alpha = 0.32f)

    val Rating = MoviesPrimitives.rating
    val Error = MoviesPrimitives.error
    val Warning = MoviesPrimitives.warning
    val Success = MoviesPrimitives.success
    val Info = MoviesPrimitives.info
    val Watched = MoviesPrimitives.success
    val Unwatched = MoviesPrimitives.neutral600
    val Cached = MoviesPrimitives.blue300
    val Torrent = MoviesPrimitives.torrent
    val Premium = MoviesPrimitives.premium

    val Border = palette.outlineVariant
    val BorderFocused = FocusRing
    val BorderMuted = Border.copy(alpha = 0.58f)

    val Scrim = MoviesPrimitives.black.copy(alpha = 0.62f)
    val ImageScrim = MoviesPrimitives.black.copy(alpha = 0.58f)
    val VideoControlsScrim = MoviesPrimitives.black.copy(alpha = 0.72f)
    val PosterFallback = BackgroundCard

    val DisabledContainer = SurfaceVariant.copy(alpha = 0.42f)
    val DisabledContent = TextDisabled
    val DisabledBorder = Border.copy(alpha = 0.48f)
    val DisabledOverlay = MoviesPrimitives.black.copy(alpha = 0.42f)

    val surfaces = MoviesSurfaceColors(
        background = Background,
        raised = BackgroundElevated,
        card = BackgroundCard,
        default = Surface,
        variant = SurfaceVariant,
        panel = Panel,
        overlay = Overlay,
        field = Field,
        menu = Menu,
        modal = Modal,
        playerOverlay = PlayerOverlay,
        divider = Divider
    )

    val text = MoviesTextColors(
        primary = TextPrimary,
        secondary = TextSecondary,
        tertiary = TextTertiary,
        disabled = TextDisabled,
        inverse = TextInverse,
        onAccent = OnSecondary,
        onOverlay = MoviesPrimitives.white,
        metadata = TextSecondary
    )

    val focus = MoviesFocusColors(
        ring = FocusRing,
        background = FocusBackground,
        content = FocusContent,
        accent = Secondary,
        scrim = FocusScrim
    )

    val selection = MoviesSelectionColors(
        background = Secondary,
        foreground = OnSecondary,
        border = SecondaryVariant,
        mutedBackground = FocusBackground,
        mutedForeground = TextPrimary
    )

    val media = MoviesMediaColors(
        heroScrim = Scrim,
        imageScrim = ImageScrim,
        posterFallback = PosterFallback,
        videoControlsScrim = VideoControlsScrim,
        glassPanelTop = if (isExpressiveBuild) Color(0xD63A3A3C) else Color(0xD64A4F59),
        glassPanelMiddle = if (isExpressiveBuild) Color(0xCC2C2C2E) else Color(0xCC3F454F),
        glassPanelBottom = if (isExpressiveBuild) Color(0xC62C2C2E) else Color(0xC640474F),
        glow = FocusRing.copy(alpha = 0.32f)
    )

    val status = MoviesStatusColors(
        rating = Rating,
        error = Error,
        warning = Warning,
        success = Success,
        info = Info,
        watched = Watched,
        unwatched = Unwatched,
        cached = Cached,
        torrent = Torrent,
        premium = Premium
    )

    val disabled = MoviesDisabledColors(
        container = DisabledContainer,
        content = DisabledContent,
        border = DisabledBorder,
        overlay = DisabledOverlay
    )

    val source = MoviesSourceColors(
        trakt = MoviesPrimitives.trakt,
        tmdb = MoviesPrimitives.tmdb,
        imdb = MoviesPrimitives.imdb,
        mdblist = MoviesPrimitives.mdblist
    )

    val contrastPairs = listOf(
        MoviesContrastPair(TextPrimary, Background),
        MoviesContrastPair(TextPrimary, BackgroundCard),
        MoviesContrastPair(TextSecondary, Background),
        MoviesContrastPair(OnSecondary, Secondary),
        MoviesContrastPair(FocusContent, FocusBackground),
        MoviesContrastPair(MoviesPrimitives.white, PlayerOverlay)
    )
}

object MoviesColors {
    val Background: Color
        @Composable
        @ReadOnlyComposable
        get() = MoviesTheme.colors.Background

    val BackgroundElevated: Color
        @Composable
        @ReadOnlyComposable
        get() = MoviesTheme.colors.BackgroundElevated

    val BackgroundCard: Color
        @Composable
        @ReadOnlyComposable
        get() = MoviesTheme.colors.BackgroundCard

    val Surface: Color
        @Composable
        @ReadOnlyComposable
        get() = MoviesTheme.colors.Surface

    val SurfaceVariant: Color
        @Composable
        @ReadOnlyComposable
        get() = MoviesTheme.colors.SurfaceVariant

    val Primary = MoviesPrimitives.neutral500
    val PrimaryVariant = MoviesPrimitives.neutral650
    val OnPrimary = MoviesPrimitives.white
    val TextPrimary = MoviesPrimitives.white
    val TextSecondary =
        if (isExpressiveBuild) ExpressiveColors.textSecondary else MoviesPrimitives.neutral400
    val TextTertiary =
        if (isExpressiveBuild) ExpressiveColors.textTertiary else MoviesPrimitives.neutral600
    val TextDisabled = MoviesPrimitives.neutral700
    val Rating = MoviesPrimitives.rating
    val Error = MoviesPrimitives.error
    val Success = MoviesPrimitives.success
    val Warning = MoviesPrimitives.warning
    val Info = MoviesPrimitives.info
    val Border = if (isExpressiveBuild) ExpressiveColors.divider else MoviesPrimitives.neutral750

    val Secondary: Color
        @Composable
        @ReadOnlyComposable
        get() = MoviesTheme.colors.Secondary

    val SecondaryVariant: Color
        @Composable
        @ReadOnlyComposable
        get() = MoviesTheme.colors.SecondaryVariant

    val OnSecondary: Color
        @Composable
        @ReadOnlyComposable
        get() = MoviesTheme.colors.OnSecondary

    val OnSecondaryVariant: Color
        @Composable
        @ReadOnlyComposable
        get() = MoviesTheme.colors.OnSecondaryVariant

    val FocusRing: Color
        @Composable
        @ReadOnlyComposable
        get() = MoviesTheme.colors.FocusRing

    val FocusBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = MoviesTheme.colors.FocusBackground

    val BorderFocused: Color
        @Composable
        @ReadOnlyComposable
        get() = MoviesTheme.colors.BorderFocused
}
