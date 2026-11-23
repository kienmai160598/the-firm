package com.nuvio.app.features.downloads

// The TV app has no downloads, so there is no ongoing download notification to update.
internal actual object DownloadsLiveStatusPlatform {
    actual fun onItemsChanged(items: List<DownloadItem>) = Unit
}
