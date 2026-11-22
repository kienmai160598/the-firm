package com.nuvio.app.core.format

import android.os.LocaleList

actual fun currentLocaleTag(): String = LocaleList.getDefault()[0].toLanguageTag()
