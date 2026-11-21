package com.nuvio.tv.ui.navigation.toptabbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Room a root screen leaves at its top for the floating top tab bar: the bar's height and
 * margins on root screens of the top tab bar layout, 0 everywhere else. The bar floats over the
 * screen and never moves it, so a screen applies this where its first row would sit under the
 * bar, usually as top content padding of its main scrolling list so content can pass under it.
 */
internal val LocalTopTabBarInset: ProvidableCompositionLocal<Dp> =
    staticCompositionLocalOf { 0.dp }

/**
 * Room the scaffold reserves for the bar. Only [TopTabBarRootFrame] turns it into
 * [LocalTopTabBarInset], so screens that are not root destinations never see it.
 */
internal val LocalTopTabBarSlot: ProvidableCompositionLocal<Dp> =
    staticCompositionLocalOf { 0.dp }

/** True on a root screen the top tab bar floats over. */
@Composable
internal fun topTabBarFloatsOver(): Boolean = LocalTopTabBarInset.current > 0.dp
