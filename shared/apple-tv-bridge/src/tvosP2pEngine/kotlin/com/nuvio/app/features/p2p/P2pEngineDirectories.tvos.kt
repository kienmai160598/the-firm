package com.nuvio.app.features.p2p

import platform.Foundation.NSHomeDirectory

// tvOS keeps only NSUserDefaults as non-purgeable local storage, so engine state lives in Caches.
internal fun p2pEngineDirectories(): P2pEngineDirectories =
    P2pEngineDirectories(
        state = "${NSHomeDirectory()}/Library/Caches/NuvioEngineState",
        cache = "${NSHomeDirectory()}/Library/Caches/NuvioEngine",
    )
