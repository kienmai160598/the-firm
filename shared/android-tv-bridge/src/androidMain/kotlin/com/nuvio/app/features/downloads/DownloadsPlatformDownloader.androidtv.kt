package com.nuvio.app.features.downloads

// The TV app has no downloads (core's actual runs on WorkManager and a foreground job service).
// A start request fails at once, so a download that core restores from storage never hangs.
internal actual object DownloadsPlatformDownloader {
    private const val UNAVAILABLE = "Downloads are not available on this TV."

    actual fun start(
        request: DownloadPlatformRequest,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
        onSuccess: (localFileUri: String, totalBytes: Long?) -> Unit,
        onFailure: (message: String) -> Unit,
        onPaused: () -> Unit,
    ): DownloadsTaskHandle {
        onFailure(UNAVAILABLE)
        return NoTask
    }

    actual fun restoreItem(item: DownloadItem): DownloadItem =
        if (item.status == DownloadStatus.Downloading) item.copy(status = DownloadStatus.Paused) else item

    actual fun removeFile(localFileUri: String?): Boolean = false

    actual fun removePartialFile(destinationFileName: String): Boolean = false

    actual fun resolveLocalFileUri(localFileUri: String?, destinationFileName: String): String? =
        localFileUri

    actual fun openDownloadsDirectory(): Boolean = false

    private object NoTask : DownloadsTaskHandle {
        override fun cancel() = Unit
    }
}
