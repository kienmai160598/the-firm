package com.kien.movies.ui.theme

// Component sizes of the box build in the Apple TV manner: media cards lift 1.08 on focus and
// round to 12dp, dialogs to 20dp. Other builds keep upstream's tokens untouched.
internal object ExpressiveComponents {
    private val cardCorner = ExpressiveRadii.forBuild.md
    private val upstream = MoviesComponents.tokens
    private val expressive = upstream.copy(
        posterCard = upstream.posterCard.copy(
            focusedScale = ExpressiveMotion.EXPRESSIVE_CARD_FOCUS_SCALE
        ),
        backdropCard = upstream.backdropCard.copy(cornerRadius = cardCorner),
        collectionCard = upstream.collectionCard.copy(cornerRadius = cardCorner),
        episodeCard = upstream.episodeCard.copy(cornerRadius = cardCorner),
        dialog = upstream.dialog.copy(cornerRadius = ExpressiveRadii.forBuild.xxl)
    )

    val forBuild: MoviesComponentTokens = if (isExpressiveBuild) expressive else upstream
}
