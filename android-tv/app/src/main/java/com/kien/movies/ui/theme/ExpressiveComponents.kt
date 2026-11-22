package com.kien.movies.ui.theme

// Component sizes in the Apple TV manner: media cards lift 1.08 on focus and round to 12dp,
// dialogs to 20dp.
internal object ExpressiveComponents {
    private val cardCorner = ExpressiveRadii.tokens.md
    private val base = MoviesComponents.tokens

    val tokens: MoviesComponentTokens = base.copy(
        posterCard = base.posterCard.copy(
            focusedScale = ExpressiveMotion.EXPRESSIVE_CARD_FOCUS_SCALE
        ),
        backdropCard = base.backdropCard.copy(cornerRadius = cardCorner),
        collectionCard = base.collectionCard.copy(cornerRadius = cardCorner),
        episodeCard = base.episodeCard.copy(cornerRadius = cardCorner),
        dialog = base.dialog.copy(cornerRadius = ExpressiveRadii.tokens.xxl)
    )
}
