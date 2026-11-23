package com.kien.movies.details

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kien.movies.shell.AppRoute
import com.kien.movies.shell.PlaceholderScreen
import com.kien.movies.shell.Router
import com.nuvio.tvos.TvosStreamTarget

/**
 * Details (tvOS DetailsView.swift). Stub until slice A6; the signature stays. Its one button
 * opens a stub Streams route, so the shell's push and Back can be walked end to end.
 */
@Composable
fun DetailsScreen(route: AppRoute.Details, router: Router) {
    val viewModel = viewModel { DetailsViewModel(route.type, route.id) }
    PlaceholderScreen(
        title = "Details",
        detail = "Open Streams for ${viewModel.id}",
        onClick = { router.open(AppRoute.Streams(stubTarget(route))) }
    )
}

private fun stubTarget(route: AppRoute.Details) = TvosStreamTarget(
    type = route.type,
    videoId = route.id,
    parentMetaId = route.id,
    seasonNumber = null,
    episodeNumber = null,
    title = route.id,
    subtitle = null,
    backdropUrl = null,
    posterUrl = null,
    logoUrl = null,
    episodeTitle = null,
    episodeThumbnailUrl = null
)
