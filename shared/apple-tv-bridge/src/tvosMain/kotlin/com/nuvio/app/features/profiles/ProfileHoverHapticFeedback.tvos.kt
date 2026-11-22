package com.nuvio.app.features.profiles

// Apple TV has no haptic engine; UISelectionFeedbackGenerator is unavailable on tvOS.
internal actual object ProfileHoverHapticFeedback {
    actual fun prepare() = Unit

    actual fun perform() = Unit

    actual fun release() = Unit
}
