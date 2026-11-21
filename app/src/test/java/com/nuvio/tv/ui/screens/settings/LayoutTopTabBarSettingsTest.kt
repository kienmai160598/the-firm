package com.nuvio.tv.ui.screens.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LayoutTopTabBarSettingsTest {

    private val allSections =
        visibleLayoutSections(essentialMode = false, focusedPosterHasOptions = true)

    @Test
    fun `sidebar section is hidden with the top tab bar`() {
        val sections = allSections.withoutSidebarForTopTabBar(topTabBar = true)

        assertFalse(LayoutSection.SIDEBAR in sections)
        assertEquals(allSections - LayoutSection.SIDEBAR, sections)
    }

    @Test
    fun `sidebar section stays with the sidebar`() {
        assertEquals(allSections, allSections.withoutSidebarForTopTabBar(topTabBar = false))
    }
}
