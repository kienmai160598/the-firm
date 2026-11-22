package com.nuvio.app.features.watchprogress

import androidx.compose.runtime.Composable
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun computeAirDateBadgeText(
    releasedIso: String?,
    todayIsoDate: String,
): String? =
    when (val badge = airDateBadge(releasedIso, todayIsoDate)) {
        AirDateBadge.Today -> stringResource(Res.string.cw_airs_today)
        AirDateBadge.Tomorrow -> stringResource(Res.string.cw_airs_tomorrow)
        is AirDateBadge.InDays ->
            pluralStringResource(Res.plurals.cw_airs_in_days, badge.days, badge.days)
        is AirDateBadge.OnDate -> stringResource(Res.string.cw_airs_date, badge.formattedDate)
        null -> null
    }
