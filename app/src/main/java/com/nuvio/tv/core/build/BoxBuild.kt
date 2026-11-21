package com.nuvio.tv.core.build

import com.nuvio.tv.BuildConfig
import com.nuvio.tv.domain.model.AppFont
import com.nuvio.tv.domain.model.AppTheme

// What the box build changes. It targets 32-bit TV boxes with a small GPU, where a card that
// resizes its row on focus, or a trailer decoding behind the UI, costs frames while browsing; it
// also starts in the Material 3 Expressive look of the box's launcher. Every setting default stays
// changeable in Settings. Its launcher keeps a key-filtering accessibility service on, so the box
// build also skips the semantics tree no screen reader reads. Its navigation is a top tab bar in
// the tvOS style instead of the sidebar.
object BoxBuild {
    private val box = BuildConfig.BOX_UI_DEFAULTS

    val focusedCardExpands: Boolean = !box
    val trailersAutoplay: Boolean = !box
    val appTheme: AppTheme = if (box) AppTheme.EXPRESSIVE else AppTheme.WHITE
    val appFont: AppFont = if (box) AppFont.GOOGLE_SANS_FLEX else AppFont.INTER
    val posterCornerRadiusDp: Int = if (box) 16 else 12
    val pruneUnreadSemantics: Boolean = box
    val topTabBar: Boolean = box
}
