package com.kien.movies.ui.navigation.toptabbar

/** Where focus lives on a root screen of the top tab bar layout. */
internal enum class TopTabBarFocus { Bar, Content }

/** Remote input the top tab bar layout reacts to. */
internal enum class TopTabBarInput {
    /** Down from a tab, or OK on a tab. */
    EnterContent,

    /** Up that no content item could take. */
    UpFromTop,
    Back
}

internal sealed interface TopTabBarStep {
    data class FocusOn(val focus: TopTabBarFocus) : TopTabBarStep
    data object ExitApp : TopTabBarStep
}

// The tvOS tab bar model (Apple HIG, Tab bars, tvOS): the bar scrolls away with the content,
// so it stays in view until the page is scrolled down,
// stays pinned over split views such as Settings, and Back always brings focus to the bar.
// On Android TV, Back from the bar leaves the app (Android TV design, Navigation on TV).
internal object TopTabBarRules {

    fun step(focus: TopTabBarFocus, input: TopTabBarInput): TopTabBarStep = when (input) {
        TopTabBarInput.EnterContent -> TopTabBarStep.FocusOn(TopTabBarFocus.Content)
        TopTabBarInput.UpFromTop -> TopTabBarStep.FocusOn(TopTabBarFocus.Bar)
        TopTabBarInput.Back -> when (focus) {
            TopTabBarFocus.Content -> TopTabBarStep.FocusOn(TopTabBarFocus.Bar)
            TopTabBarFocus.Bar -> TopTabBarStep.ExitApp
        }
    }

    fun barVisible(focus: TopTabBarFocus, routePinsBar: Boolean, scrolledDown: Boolean): Boolean =
        focus == TopTabBarFocus.Bar || routePinsBar || !scrolledDown
}
