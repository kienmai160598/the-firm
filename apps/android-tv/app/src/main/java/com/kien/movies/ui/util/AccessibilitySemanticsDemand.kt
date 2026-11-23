package com.kien.movies.ui.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent

/** What an enabled accessibility service can learn about the app's UI. */
data class AccessibilityServiceReach(val canRetrieveWindowContent: Boolean, val eventTypes: Int)

// Compose builds its semantics tree whenever any accessibility service is enabled. A service that
// cannot retrieve window content and only listens to window state changes (a key remapper, a
// foreground-app tracker) never reads that tree, so building it only costs frames.
object AccessibilitySemanticsDemand {
    private const val WINDOW_STATE_ONLY = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED

    fun isNeeded(
        touchExplorationEnabled: Boolean,
        services: List<AccessibilityServiceReach>
    ): Boolean = touchExplorationEnabled || services.any(::readsContent)

    fun reachOf(info: AccessibilityServiceInfo): AccessibilityServiceReach = AccessibilityServiceReach(
        canRetrieveWindowContent = (
            info.capabilities and
                AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT
            ) != 0,
        eventTypes = info.eventTypes
    )

    private fun readsContent(service: AccessibilityServiceReach): Boolean = service.canRetrieveWindowContent || (service.eventTypes and WINDOW_STATE_ONLY.inv()) != 0
}
