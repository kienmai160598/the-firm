package com.nuvio.tvos

import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults

private const val PROGRESS_KEY = "watch_progress_payload_1"
private const val PROGRESS_PAYLOAD = """{"entries":[{"videoId":"tt1:1:2"}]}"""

class PayloadFileStoreTest {
    private val fixture = PayloadStoreFixture()

    @AfterTest
    fun tearDown() = fixture.remove()

    @Test
    fun loadMovesALegacyDefaultsPayloadIntoAFile() {
        fixture.defaults.setObject(PROGRESS_PAYLOAD, forKey = PROGRESS_KEY)

        assertEquals(PROGRESS_PAYLOAD, fixture.store.load(PROGRESS_KEY))
        assertEquals(PAYLOAD_IN_FILE_MARKER, fixture.defaults.stringForKey(PROGRESS_KEY))
        assertTrue(fixture.fileExists(PROGRESS_KEY))
        assertEquals(PROGRESS_PAYLOAD, fixture.freshStore().load(PROGRESS_KEY))
    }

    @Test
    fun saveWritesTheFileAndLeavesOnlyTheMarkerInDefaults() {
        fixture.store.save(PROGRESS_KEY, PROGRESS_PAYLOAD)

        assertEquals(PAYLOAD_IN_FILE_MARKER, fixture.defaults.stringForKey(PROGRESS_KEY))
        assertEquals(PROGRESS_PAYLOAD, fixture.freshStore().load(PROGRESS_KEY))
    }

    @Test
    fun removingTheDefaultsKeyLikeSignOutDropsTheFile() {
        fixture.store.save(PROGRESS_KEY, PROGRESS_PAYLOAD)
        fixture.defaults.removeObjectForKey(PROGRESS_KEY)

        assertNull(fixture.store.load(PROGRESS_KEY))
        assertFalse(fixture.fileExists(PROGRESS_KEY))
    }

    @Test
    fun aFilePurgedFromCachesLoadsNothingAndClearsTheMarker() {
        fixture.store.save(PROGRESS_KEY, PROGRESS_PAYLOAD)
        fixture.removeFile(PROGRESS_KEY)

        assertNull(fixture.freshStore().load(PROGRESS_KEY))
        assertNull(fixture.defaults.stringForKey(PROGRESS_KEY))
        fixture.store.save(PROGRESS_KEY, PROGRESS_PAYLOAD)
        assertEquals(PROGRESS_PAYLOAD, fixture.freshStore().load(PROGRESS_KEY))
    }

    @Test
    fun payloadFilesLiveInTheCachesFolder() {
        val path = defaultPayloadDirectory().path.orEmpty()

        assertTrue(path.endsWith("/Library/Caches/Payloads"), path)
    }

    @Test
    fun missingKeyLoadsNothing() {
        assertNull(fixture.store.load(PROGRESS_KEY))
    }
}

@OptIn(ExperimentalForeignApi::class)
internal class PayloadStoreFixture {
    private val suiteName = "com.nuvio.tvos.test.${Random.nextLong().toULong()}"
    private val directoryPath = "${NSTemporaryDirectory()}payload-test-${Random.nextInt()}"
    val defaults = requireNotNull(NSUserDefaults(suiteName = suiteName))
    val store = freshStore()

    fun freshStore(): PayloadFileStore =
        PayloadFileStore(directory = NSURL.fileURLWithPath(directoryPath), defaults = defaults)

    fun fileExists(key: String): Boolean =
        NSFileManager.defaultManager.fileExistsAtPath("$directoryPath/$key.json")

    fun removeFile(key: String) {
        NSFileManager.defaultManager.removeItemAtPath("$directoryPath/$key.json", error = null)
    }

    fun remove() {
        defaults.removePersistentDomainForName(suiteName)
        NSFileManager.defaultManager.removeItemAtPath(directoryPath, error = null)
    }
}
