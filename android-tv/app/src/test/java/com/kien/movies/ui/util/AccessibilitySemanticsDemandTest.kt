package com.kien.movies.ui.util

import android.view.accessibility.AccessibilityEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilitySemanticsDemandTest {

    private val keyFilter = AccessibilityServiceReach(
        canRetrieveWindowContent = false,
        eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
    )

    @Test
    fun `no services and no touch exploration need no semantics`() {
        assertFalse(AccessibilitySemanticsDemand.isNeeded(false, emptyList()))
    }

    @Test
    fun `a key filter that only tracks window changes needs no semantics`() {
        assertFalse(AccessibilitySemanticsDemand.isNeeded(false, listOf(keyFilter)))
    }

    @Test
    fun `a service that retrieves window content needs semantics`() {
        val reader = keyFilter.copy(canRetrieveWindowContent = true)

        assertTrue(AccessibilitySemanticsDemand.isNeeded(false, listOf(keyFilter, reader)))
    }

    @Test
    fun `a service that receives view events needs semantics`() {
        val listener = keyFilter.copy(eventTypes = AccessibilityEvent.TYPE_VIEW_FOCUSED)

        assertTrue(AccessibilitySemanticsDemand.isNeeded(false, listOf(listener)))
    }

    @Test
    fun `touch exploration needs semantics even without a listed service`() {
        assertTrue(AccessibilitySemanticsDemand.isNeeded(true, emptyList()))
    }
}
