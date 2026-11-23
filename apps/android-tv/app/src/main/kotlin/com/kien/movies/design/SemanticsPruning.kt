package com.kien.movies.design

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

// Ported from the old app (ui/util/SemanticsForScreenReaders.kt and
// AccessibilitySemanticsDemand.kt), where it took Home jank on the box from 8.0% to about 4%.

/**
 * Hides [content] from the semantics tree while no enabled accessibility service can read it, so
 * Compose stops rebuilding that tree for services that never look at it (a key remapper, a
 * foreground app tracker). A screen reader turning on brings the tree back without resetting
 * [content]'s state.
 *
 * @param prune false keeps the tree always: debug builds need it for the UI tests, which read
 *   the semantics tree directly. Pass [prunesUnreadSemantics].
 */
@Composable
fun SemanticsForScreenReaders(prune: Boolean, content: @Composable () -> Unit) {
    val semanticsNeeded = !prune || rememberSemanticsNeeded()
    Box(
        modifier = if (semanticsNeeded) Modifier else Modifier.clearAndSetSemantics {},
        propagateMinConstraints = true
    ) {
        content()
    }
}

/** True in the `box` build; false in a debuggable build, whose UI tests read the tree. */
fun Context.prunesUnreadSemantics(): Boolean = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) == 0

/** What an enabled accessibility service can learn about the app's UI. */
data class AccessibilityServiceReach(val canRetrieveWindowContent: Boolean, val eventTypes: Int)

/**
 * Compose builds its semantics tree whenever any accessibility service is enabled. A service that
 * cannot retrieve window content and only listens to window state changes never reads that tree,
 * so building it only costs frames.
 */
object AccessibilitySemanticsDemand {
    private const val WINDOW_STATE_ONLY = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED

    fun isNeeded(touchExplorationEnabled: Boolean, services: List<AccessibilityServiceReach>): Boolean = touchExplorationEnabled || services.any(::readsContent)

    fun reachOf(info: AccessibilityServiceInfo): AccessibilityServiceReach = AccessibilityServiceReach(
        canRetrieveWindowContent = (info.capabilities and AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT) != 0,
        eventTypes = info.eventTypes
    )

    private fun readsContent(service: AccessibilityServiceReach): Boolean = service.canRetrieveWindowContent || (service.eventTypes and WINDOW_STATE_ONLY.inv()) != 0
}

@Composable
private fun rememberSemanticsNeeded(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager }
    var needed by remember(manager) { mutableStateOf(semanticsNeeded(manager)) }

    DisposableEffect(manager) {
        val refresh = { needed = semanticsNeeded(manager) }
        val stateListener = AccessibilityManager.AccessibilityStateChangeListener { refresh() }
        val touchListener = AccessibilityManager.TouchExplorationStateChangeListener { refresh() }
        manager.addAccessibilityStateChangeListener(stateListener)
        manager.addTouchExplorationStateChangeListener(touchListener)
        val removeServicesListener = addServicesListener(context, manager, refresh)
        onDispose {
            manager.removeAccessibilityStateChangeListener(stateListener)
            manager.removeTouchExplorationStateChangeListener(touchListener)
            removeServicesListener()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { needed = semanticsNeeded(manager) }

    return needed
}

private fun addServicesListener(context: Context, manager: AccessibilityManager, refresh: () -> Unit): () -> Unit {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return {}
    val listener = AccessibilityManager.AccessibilityServicesStateChangeListener { refresh() }
    manager.addAccessibilityServicesStateChangeListener(context.mainExecutor, listener)
    return { manager.removeAccessibilityServicesStateChangeListener(listener) }
}

private fun semanticsNeeded(manager: AccessibilityManager): Boolean {
    if (!manager.isEnabled) return false
    val services = manager
        .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        .map(AccessibilitySemanticsDemand::reachOf)
    return AccessibilitySemanticsDemand.isNeeded(manager.isTouchExplorationEnabled, services)
}
