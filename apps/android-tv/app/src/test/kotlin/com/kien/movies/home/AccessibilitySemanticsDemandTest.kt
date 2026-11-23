package com.kien.movies.home

import android.view.accessibility.AccessibilityEvent
import com.kien.movies.design.AccessibilitySemanticsDemand
import com.kien.movies.design.AccessibilityServiceReach
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Port of the old app's test: which enabled services make Home keep its semantics tree. */
class AccessibilitySemanticsDemandTest {
    private val keyFilter = AccessibilityServiceReach(canRetrieveWindowContent = false, eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)

    @Test
    fun noServicesAndNoTouchExplorationNeedNoSemantics() {
        assertFalse(AccessibilitySemanticsDemand.isNeeded(false, emptyList()))
    }

    @Test
    fun aKeyFilterThatOnlyTracksWindowChangesNeedsNoSemantics() {
        assertFalse(AccessibilitySemanticsDemand.isNeeded(false, listOf(keyFilter)))
    }

    @Test
    fun aServiceThatRetrievesWindowContentNeedsSemantics() {
        val reader = keyFilter.copy(canRetrieveWindowContent = true)

        assertTrue(AccessibilitySemanticsDemand.isNeeded(false, listOf(keyFilter, reader)))
    }

    @Test
    fun aServiceThatReceivesViewEventsNeedsSemantics() {
        val listener = keyFilter.copy(eventTypes = AccessibilityEvent.TYPE_VIEW_FOCUSED)

        assertTrue(AccessibilitySemanticsDemand.isNeeded(false, listOf(listener)))
    }

    @Test
    fun touchExplorationNeedsSemanticsWithoutAListedService() {
        assertTrue(AccessibilitySemanticsDemand.isNeeded(true, emptyList()))
    }
}
