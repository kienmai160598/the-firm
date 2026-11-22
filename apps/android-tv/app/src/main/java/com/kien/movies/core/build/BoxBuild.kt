package com.kien.movies.core.build

import com.kien.movies.BuildConfig
import com.kien.movies.domain.model.AppFont
import com.kien.movies.domain.model.AppTheme

// What the box build changes. It targets 32-bit TV boxes with a small GPU, where a card that
// resizes its row on focus, or a trailer decoding behind the UI, costs frames while browsing; it
// starts, like every build, in the Material 3 Expressive look. Every setting default stays changeable
// in Settings. Its launcher keeps a key-filtering accessibility service on, so the box
// build also skips the semantics tree no screen reader reads. Its navigation is a top tab bar in
// the tvOS style instead of the sidebar.
object BoxBuild {
    private val box = BuildConfig.BOX_UI_DEFAULTS

    val focusedCardExpands: Boolean = !box
    val trailersAutoplay: Boolean = !box
    val appTheme: AppTheme = AppTheme.EXPRESSIVE
    val appFont: AppFont = AppFont.GOOGLE_SANS_FLEX
    val posterCornerRadiusDp: Int = 12
    val pruneUnreadSemantics: Boolean = box
    val topTabBar: Boolean = box
}
