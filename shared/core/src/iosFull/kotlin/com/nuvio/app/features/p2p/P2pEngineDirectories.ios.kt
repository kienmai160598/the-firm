package com.nuvio.app.features.p2p

import platform.Foundation.NSHomeDirectory

internal fun p2pEngineDirectories(): P2pEngineDirectories =
    P2pEngineDirectories(
        state = "${NSHomeDirectory()}/Library/Application Support/NuvioEngine/state",
        cache = "${NSHomeDirectory()}/Library/Caches/NuvioEngine",
    )
