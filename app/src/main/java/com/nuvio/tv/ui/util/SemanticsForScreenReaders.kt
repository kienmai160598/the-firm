package com.nuvio.tv.ui.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/** [setContent] whose tree is wrapped in [SemanticsForScreenReaders]. */
fun ComponentActivity.setContentForScreenReaders(
    pruneUnreadSemantics: Boolean,
    content: @Composable () -> Unit
) = setContent { SemanticsForScreenReaders(pruneUnreadSemantics, content) }

/**
 * Hides [content] from the semantics tree while no enabled accessibility service can read it, so
 * Compose stops rebuilding that tree on every frame for services that never look at it. A screen
 * reader turning on brings the tree back without recomposing [content]'s state away.
 */
@Composable
fun SemanticsForScreenReaders(pruneUnreadSemantics: Boolean, content: @Composable () -> Unit) {
    val semanticsNeeded = !pruneUnreadSemantics || rememberSemanticsNeeded()

    Box(
        modifier = if (semanticsNeeded) Modifier else Modifier.clearAndSetSemantics {},
        propagateMinConstraints = true
    ) {
        content()
    }
}

@Composable
private fun rememberSemanticsNeeded(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    }
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

private fun addServicesListener(
    context: Context,
    manager: AccessibilityManager,
    refresh: () -> Unit
): () -> Unit {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return {}

    val listener = AccessibilityManager.AccessibilityServicesStateChangeListener { refresh() }
    manager.addAccessibilityServicesStateChangeListener(
        ContextCompat.getMainExecutor(context),
        listener
    )
    return { manager.removeAccessibilityServicesStateChangeListener(listener) }
}

private fun semanticsNeeded(manager: AccessibilityManager): Boolean {
    if (!manager.isEnabled) return false

    val services = manager
        .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        .map(AccessibilitySemanticsDemand::reachOf)
    return AccessibilitySemanticsDemand.isNeeded(manager.isTouchExplorationEnabled, services)
}
