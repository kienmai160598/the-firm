package com.kien.movies.shell

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

/** One entry's screen with its own saved state slot and ViewModel store. */
@Composable
fun EntryContent(
    entry: RouteEntry,
    saveableState: SaveableStateHolder,
    stores: RouteViewModelStores,
    focus: FocusRequester,
    isTarget: Boolean,
    screen: @Composable () -> Unit
) {
    saveableState.SaveableStateProvider(entry.id) {
        CompositionLocalProvider(LocalViewModelStoreOwner provides stores.owner(entry.id)) {
            Box(modifier = Modifier.fillMaxSize().then(entryFocus(focus, isTarget)).focusRestorer().focusGroup()) {
                screen()
            }
        }
    }
}

/** The shell's focus requester goes to the arriving screen only; a leaving one takes no focus. */
private fun entryFocus(focus: FocusRequester, isTarget: Boolean): Modifier = if (isTarget) Modifier.focusRequester(focus) else Modifier.focusProperties { onEnter = { cancelFocusChange() } }

/**
 * Drops the saved state and the ViewModels of entries that left the stack, once [settled]: a popped
 * screen is still drawn while it slides out.
 */
@Composable
fun ForgetRemovedEntries(stack: List<RouteEntry>, saveableState: SaveableStateHolder, stores: RouteViewModelStores, settled: Boolean) {
    val known = remember { mutableSetOf<Long>() }
    DisposableEffect(stack, settled) {
        if (!settled) return@DisposableEffect onDispose {}
        val current = stack.map(RouteEntry::id).toSet()
        val tabIds = RootTab.entries.map(Router::tabEntryId).toSet()
        (known - current - tabIds).forEach(saveableState::removeState)
        known.retainAll(current + tabIds)
        known.addAll(current)
        stores.retainOnly(current)
        onDispose {}
    }
}
