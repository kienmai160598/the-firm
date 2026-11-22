package com.kien.movies.core.sync.library

import com.kien.movies.domain.model.LibraryDeltaEvent
import com.kien.movies.domain.model.LibrarySyncKey
import com.kien.movies.domain.model.SavedLibraryItem

interface LibrarySyncRemoteDataSource {
    suspend fun pullSnapshot(
        profileId: Int,
        pageSize: Int
    ): List<SavedLibraryItem>

    suspend fun getDeltaCursor(profileId: Int): Long

    suspend fun pullDelta(
        profileId: Int,
        sinceEventId: Long,
        limit: Int
    ): List<LibraryDeltaEvent>

    suspend fun pushItems(
        profileId: Int,
        items: Collection<SavedLibraryItem>
    )

    suspend fun deleteItems(
        profileId: Int,
        keys: Collection<LibrarySyncKey>
    )
}
