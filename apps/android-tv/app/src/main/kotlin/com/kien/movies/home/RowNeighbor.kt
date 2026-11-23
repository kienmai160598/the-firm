package com.kien.movies.home

/** Where focus goes when a card leaves a row (tvOS `RowNeighbor` in ContinueWatchingRowView.swift). */
object RowNeighbor {
    /** The key after [key] in [keys], else the one before, else null (also when [key] is absent). */
    fun key(beside: String, keys: List<String>): String? {
        val index = keys.indexOf(beside)
        if (index < 0) return null
        return keys.getOrNull(index + 1) ?: keys.getOrNull(index - 1)
    }
}
