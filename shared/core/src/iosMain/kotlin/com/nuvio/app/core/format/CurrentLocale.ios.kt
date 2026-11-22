package com.nuvio.app.core.format

import platform.Foundation.NSLocale
import platform.Foundation.canonicalLanguageIdentifierFromString
import platform.Foundation.currentLocale
import platform.Foundation.localeIdentifier
import platform.Foundation.preferredLanguages

actual fun currentLocaleTag(): String {
    val preferred = NSLocale.preferredLanguages.firstOrNull() as? String
    val locale = preferred?.let(::NSLocale) ?: NSLocale.currentLocale
    val identifier = locale.localeIdentifier.substringBefore("@")
    return NSLocale.canonicalLanguageIdentifierFromString(identifier)
}
