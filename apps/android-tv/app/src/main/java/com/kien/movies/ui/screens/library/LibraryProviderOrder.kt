package com.kien.movies.ui.screens.library

import com.kien.movies.domain.model.LibraryEntry
import com.kien.movies.domain.model.LibrarySourceMode

data class LibraryProviderOrder(
    val source: LibrarySourceMode,
    val listKey: String,
    val sortOption: LibrarySortOption,
    val ranks: Map<String, Int>
) {
    fun comparator(): Comparator<LibraryEntry> = compareBy<LibraryEntry> {
        ranks["${it.type}:${it.id}"] ?: Int.MAX_VALUE
    }.thenBy { it.id }
}
