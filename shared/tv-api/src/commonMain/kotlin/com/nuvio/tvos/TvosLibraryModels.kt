package com.nuvio.tvos

data class TvosLibraryState(
    val rows: List<TvosRow>,
    val isLoading: Boolean,
    val errorMessage: String?,
)

/** Outcome of pressing the Library button on Details. */
enum class TvosLibraryToggle {
    SAVED,
    REMOVED,

    /** Removing would drop the title from a tracker list; ask, then toggle again with the confirmation. */
    NEEDS_CONFIRMATION,
    FAILED,
}
