package com.nuvio.tv.domain.model

import com.nuvio.tv.BuildConfig

// Defaults for settings the user has never changed. The box build targets 32-bit TV boxes with a
// small GPU, where a card that resizes its row on focus, or a trailer decoding behind the UI,
// costs frames while browsing; it also starts in the Material 3 Expressive look of the box's
// launcher. Everything stays available in Settings. Its launcher keeps a key-filtering
// accessibility service on, so the box build also skips the semantics tree no screen reader reads.
object BoxUiDefaults {
    private val box = BuildConfig.BOX_UI_DEFAULTS

    val focusedCardExpands: Boolean = !box
    val trailersAutoplay: Boolean = !box
    val appTheme: AppTheme = if (box) AppTheme.EXPRESSIVE else AppTheme.WHITE
    val appFont: AppFont = if (box) AppFont.GOOGLE_SANS_FLEX else AppFont.INTER
    val posterCornerRadiusDp: Int = if (box) 16 else 12
    val pruneUnreadSemantics: Boolean = box
}
