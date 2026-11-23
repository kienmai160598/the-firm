package com.kien.movies.core.sync.library

import com.kien.movies.domain.model.LibraryDeltaApplyResult
import com.kien.movies.domain.model.LibraryDeltaEvent
import com.kien.movies.domain.model.LibrarySnapshotApplyResult
import com.kien.movies.domain.model.LibrarySyncState
import com.kien.movies.domain.model.SavedLibraryItem

interface LibrarySyncLocalStore {
    suspend fun getSyncState(profileId: Int): LibrarySyncState

    suspend fun applyRemoteSnapshot(
        profileId: Int,
        remoteItems: Collection<SavedLibraryItem>,
        cursorEventId: Long
    ): LibrarySnapshotApplyResult

    suspend fun applyRemoteDelta(
        profileId: Int,
        events: Collection<LibraryDeltaEvent>
    ): LibraryDeltaApplyResult

    suspend fun queueAllItemsForPush(profileId: Int): LibrarySyncState

    suspend fun acknowledgePush(profileId: Int, expectedMutationRevision: Long): Boolean
}
