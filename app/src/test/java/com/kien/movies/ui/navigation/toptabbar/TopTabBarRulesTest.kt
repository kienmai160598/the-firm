package com.kien.movies.ui.navigation.toptabbar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopTabBarRulesTest {

    @Test
    fun `down or OK from the bar moves focus into the content`() {
        assertEquals(
            TopTabBarStep.FocusOn(TopTabBarFocus.Content),
            TopTabBarRules.step(TopTabBarFocus.Bar, TopTabBarInput.EnterContent)
        )
    }

    @Test
    fun `up past the first content row brings focus back to the bar`() {
        assertEquals(
            TopTabBarStep.FocusOn(TopTabBarFocus.Bar),
            TopTabBarRules.step(TopTabBarFocus.Content, TopTabBarInput.UpFromTop)
        )
    }

    @Test
    fun `back from the content brings focus to the bar`() {
        assertEquals(
            TopTabBarStep.FocusOn(TopTabBarFocus.Bar),
            TopTabBarRules.step(TopTabBarFocus.Content, TopTabBarInput.Back)
        )
    }

    @Test
    fun `back from the bar leaves the app`() {
        assertEquals(
            TopTabBarStep.ExitApp,
            TopTabBarRules.step(TopTabBarFocus.Bar, TopTabBarInput.Back)
        )
    }

    @Test
    fun `the bar stays in view until the page is scrolled down`() {
        assertTrue(
            TopTabBarRules.barVisible(
                focus = TopTabBarFocus.Content,
                routePinsBar = false,
                scrolledDown = false
            )
        )
        assertFalse(
            TopTabBarRules.barVisible(
                focus = TopTabBarFocus.Content,
                routePinsBar = false,
                scrolledDown = true
            )
        )
    }

    @Test
    fun `a focused or pinned bar stays in view on a scrolled page`() {
        assertTrue(
            TopTabBarRules.barVisible(
                focus = TopTabBarFocus.Bar,
                routePinsBar = false,
                scrolledDown = true
            )
        )
        assertTrue(
            TopTabBarRules.barVisible(
                focus = TopTabBarFocus.Content,
                routePinsBar = true,
                scrolledDown = true
            )
        )
    }
}
