package com.nuvio.tvos

import co.touchlab.kermit.Logger
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToURL

private const val PAYLOAD_DIRECTORY = "Payloads"
internal const val PAYLOAD_IN_FILE_MARKER = "@payload-file"

internal sealed interface FileResult<out T> {
    data class Done<T>(val value: T) : FileResult<T>
    data class Failed(val reason: String) : FileResult<Nothing>
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal class PayloadFileStore(
    private val directory: NSURL = defaultPayloadDirectory(),
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) {
    private val log = Logger.withTag("PayloadFileStore")

    fun load(key: String): String? {
        val storedValue = defaults.stringForKey(key)
        return when (storedValue) {
            null -> discardOrphanFile(key)
            PAYLOAD_IN_FILE_MARKER -> readFileOrForgetPurged(key)
            else -> migrateFromDefaults(key, storedValue)
        }
    }

    fun save(key: String, payload: String) {
        when (val written = writeFile(key, payload)) {
            is FileResult.Done -> defaults.setObject(PAYLOAD_IN_FILE_MARKER, forKey = key)
            is FileResult.Failed -> {
                log.e { "Keeping $key in NSUserDefaults: ${written.reason}" }
                removeFile(key)
                defaults.setObject(payload, forKey = key)
            }
        }
    }

    private fun discardOrphanFile(key: String): String? {
        if (NSFileManager.defaultManager.fileExistsAtPath(filePath(key))) {
            removeFile(key)
            log.i { "Removed the file for $key because its NSUserDefaults marker is gone" }
        }
        return null
    }

    private fun migrateFromDefaults(key: String, payload: String): String {
        when (val written = writeFile(key, payload)) {
            is FileResult.Done -> {
                defaults.setObject(PAYLOAD_IN_FILE_MARKER, forKey = key)
                log.i { "Moved $key (${payload.length} chars) from NSUserDefaults to a file" }
            }
            is FileResult.Failed -> log.e { "Could not move $key to a file: ${written.reason}" }
        }
        return payload
    }

    private fun readFileOrForgetPurged(key: String): String? {
        if (!NSFileManager.defaultManager.fileExistsAtPath(filePath(key))) {
            log.w { "The file for $key is gone, the system purged Caches; starting empty" }
            defaults.removeObjectForKey(key)
            return null
        }
        return when (val read = readFile(key)) {
            is FileResult.Done -> read.value
            is FileResult.Failed -> {
                log.e { "Could not read the file for $key: ${read.reason}" }
                null
            }
        }
    }

    private fun readFile(key: String): FileResult<String> =
        withFileError { errorPointer ->
            NSString.create(
                contentsOfURL = fileUrl(key),
                encoding = NSUTF8StringEncoding,
                error = errorPointer,
            )?.toString()
        }

    private fun writeFile(key: String, payload: String): FileResult<Unit> {
        val directoryCreated = withFileError { errorPointer ->
            NSFileManager.defaultManager.createDirectoryAtURL(
                url = directory,
                withIntermediateDirectories = true,
                attributes = null,
                error = errorPointer,
            ).succeededOrNull()
        }
        if (directoryCreated is FileResult.Failed) return directoryCreated

        return withFileError { errorPointer ->
            NSString.create(string = payload).writeToURL(
                url = fileUrl(key),
                atomically = true,
                encoding = NSUTF8StringEncoding,
                error = errorPointer,
            ).succeededOrNull()
        }
    }

    private fun removeFile(key: String) {
        if (!NSFileManager.defaultManager.fileExistsAtPath(filePath(key))) return
        val removed = withFileError { errorPointer ->
            NSFileManager.defaultManager.removeItemAtURL(fileUrl(key), error = errorPointer)
                .succeededOrNull()
        }
        if (removed is FileResult.Failed) {
            log.e { "Could not remove ${filePath(key)}: ${removed.reason}" }
        }
    }

    private fun filePath(key: String): String = requireNotNull(fileUrl(key).path)

    private fun fileUrl(key: String): NSURL =
        requireNotNull(directory.URLByAppendingPathComponent("$key.json"))
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun <T : Any> withFileError(
    operation: (CPointer<ObjCObjectVar<NSError?>>) -> T?,
): FileResult<T> = memScoped {
    val error = alloc<ObjCObjectVar<NSError?>>()
    val value = operation(error.ptr)
    value?.let { FileResult.Done(it) }
        ?: FileResult.Failed(error.value?.localizedDescription ?: "unknown file error")
}

private fun Boolean.succeededOrNull(): Unit? = if (this) Unit else null

internal fun defaultPayloadDirectory(): NSURL {
    val caches = NSFileManager.defaultManager
        .URLsForDirectory(NSCachesDirectory, NSUserDomainMask)
        .firstOrNull() as? NSURL
    return requireNotNull(caches?.URLByAppendingPathComponent(PAYLOAD_DIRECTORY))
}

internal val tvosPayloadFiles: PayloadFileStore by lazy { PayloadFileStore() }
