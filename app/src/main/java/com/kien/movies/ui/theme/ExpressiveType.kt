package com.kien.movies.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Typography
import com.kien.movies.domain.model.AppFont

@OptIn(ExperimentalTvMaterial3Api::class)
internal fun buildAppTypography(appFont: AppFont): Typography {
    val typography = buildNuvioTypography(getFontFamily(appFont)).withSmallSteps()
    return if (appFont == AppFont.GOOGLE_SANS_FLEX) typography.expressive() else typography
}

// Nuvio's scale skips these two, so tv-material served them in the system font. Its medium
// steps already sit at the sizes those fallbacks rendered (36/44, 24/32), so no screen reflows.
@OptIn(ExperimentalTvMaterial3Api::class)
private fun Typography.withSmallSteps(): Typography = copy(
    displaySmall = displayMedium,
    headlineSmall = headlineMedium
)

/**
 * Material 3 Expressive type on Google Sans Flex: the emphasized weights of Compose Material 3's
 * TypeScaleTokens for display, headline and title (Medium, titles below large Bold), the
 * baseline weights for body (Regular) and labels (Medium), and an optical size per role.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
private fun Typography.expressive(): Typography = copy(
    displayLarge = displayLarge.role(GoogleSansFlexDisplay, FontWeight.Medium),
    displayMedium = displayMedium.role(GoogleSansFlexDisplay, FontWeight.Medium),
    displaySmall = displaySmall.role(GoogleSansFlexDisplay, FontWeight.Medium),
    headlineLarge = headlineLarge.role(GoogleSansFlexHeadline, FontWeight.Medium),
    headlineMedium = headlineMedium.role(GoogleSansFlexHeadline, FontWeight.Medium),
    headlineSmall = headlineSmall.role(GoogleSansFlexHeadline, FontWeight.Medium),
    titleLarge = titleLarge.role(GoogleSansFlexFamily, FontWeight.Medium),
    titleMedium = titleMedium.role(GoogleSansFlexFamily, FontWeight.Bold),
    titleSmall = titleSmall.role(GoogleSansFlexFamily, FontWeight.Bold),
    bodyLarge = bodyLarge.role(GoogleSansFlexFamily, FontWeight.Normal),
    bodyMedium = bodyMedium.role(GoogleSansFlexFamily, FontWeight.Normal),
    bodySmall = bodySmall.role(GoogleSansFlexFamily, FontWeight.Normal),
    labelLarge = labelLarge.role(GoogleSansFlexLabel, FontWeight.Medium),
    labelMedium = labelMedium.role(GoogleSansFlexLabel, FontWeight.Medium),
    labelSmall = labelSmall.role(GoogleSansFlexLabel, FontWeight.Medium)
)

private fun TextStyle.role(family: FontFamily, weight: FontWeight): TextStyle =
    copy(fontFamily = family, fontWeight = weight)
