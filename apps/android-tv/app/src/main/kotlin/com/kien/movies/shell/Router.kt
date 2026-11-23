package com.kien.movies.shell

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One back stack entry. [id] tells two entries of the same route apart (state, ViewModels). */
data class RouteEntry(val id: Long, val route: AppRoute)

/** What a Back press did. */
enum class BackResult {
    /** The top entry was removed. */
    Popped,

    /** At the root of another tab: Home was selected. */
    WentHome,

    /** At the Home root: the app should finish. */
    Exit
}

/**
 * The back stack as state (tvOS Router.swift). The bottom entry is always a tab root; [open]
 * pushes over it, [selectTab] replaces the whole stack. Call from the main thread.
 *
 * Back, as a pure rule: pop while deeper than a tab root, then go Home, then exit. The shell adds
 * one step in front for the sidebar (RootScreen): at a tab root with focus in the content, Back
 * first moves focus to the sidebar.
 */
class Router(initialTab: RootTab = RootTab.Home) {
    private val entries = MutableStateFlow(listOf(tabEntry(initialTab)))
    private var nextId = 1L

    val stack: StateFlow<List<RouteEntry>> = entries.asStateFlow()

    val top: RouteEntry get() = entries.value.last()

    val tab: RootTab get() = (entries.value.first().route as AppRoute.Tab).tab

    val isAtTabRoot: Boolean get() = entries.value.size == 1

    /**
     * Called just before the stack changes, while the current screen is still composed and
     * focused. The shell uses it to save which element had focus (RootScreen).
     */
    var beforeChange: (() -> Unit)? = null

    /** Pushes [route]; a [AppRoute.Tab] selects that tab instead. */
    fun open(route: AppRoute) {
        if (route is AppRoute.Tab) {
            selectTab(route.tab)
            return
        }
        change(entries.value + RouteEntry(nextId++, route))
    }

    /** Shows [tab] at its root and drops everything pushed over the current tab. */
    fun selectTab(tab: RootTab) {
        change(listOf(tabEntry(tab)))
    }

    fun back(): BackResult = when {
        !isAtTabRoot -> {
            change(entries.value.dropLast(1))
            BackResult.Popped
        }

        tab != RootTab.Home -> {
            selectTab(RootTab.Home)
            BackResult.WentHome
        }

        else -> BackResult.Exit
    }

    private fun change(next: List<RouteEntry>) {
        beforeChange?.invoke()
        entries.value = next
    }

    companion object {
        /** Tab roots have fixed negative ids, so a tab keeps its state and ViewModels while away. */
        fun tabEntryId(tab: RootTab): Long = -(tab.ordinal + 1L)

        private fun tabEntry(tab: RootTab) = RouteEntry(tabEntryId(tab), AppRoute.Tab(tab))
    }
}
