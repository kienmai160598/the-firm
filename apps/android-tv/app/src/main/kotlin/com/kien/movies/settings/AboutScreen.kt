package com.kien.movies.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import androidx.tv.material3.Text
import com.kien.movies.design.TvColors
import com.kien.movies.design.TvType

/**
 * The app's name, version, build and licence (tvOS AboutView.swift). The rows are focusable
 * list items that do nothing, so the remote has somewhere to be.
 */
@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val facts = remember { appFacts(context) }
    SettingsSplit(
        title = SettingsPage.About.title,
        preview = { SettingsPreview(caption = SettingsPage.About.caption) { SettingsSymbolArt(SettingsPage.About.icon) } },
        list = {
            Column(verticalArrangement = Arrangement.spacedBy(SettingsMetrics.rowGap)) {
                facts.forEach { (title, value) ->
                    SettingsLinkLabel(title = title, value = value, onClick = {}, trailingIcon = null)
                }
                Text(text = CREDIT, style = TvType.caption1, color = TvColors.textSecondary, modifier = Modifier.padding(horizontal = CREDIT_INSET))
            }
        }
    )
}

private fun appFacts(context: Context): List<Pair<String, String>> {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    val name = context.applicationInfo.loadLabel(context.packageManager).toString()
    return listOf(
        "Name" to name,
        "Version" to info.versionName.orEmpty(),
        "Build" to PackageInfoCompat.getLongVersionCode(info).toString(),
        "Licence" to LICENCE
    )
}

private const val LICENCE = "GPL-3.0"
private const val CREDIT = "Built on NuvioMobile and NuvioTV by NuvioMedia. Torrents play through the Nuvio engine until the Movies engine reaches Android."
private val CREDIT_INSET = 16.dp
