@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kien.movies.ui.components

import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api

private val ErrorContainer = Color(0xFF4A0F12)
private val OnErrorContainer = Color(0xFFFFDAD6)

/** A dark red container, for error banners and notices on the expressive build. */
internal val ExpressiveErrorContainer: Color = ErrorContainer
internal val ExpressiveOnErrorContainer: Color = OnErrorContainer
