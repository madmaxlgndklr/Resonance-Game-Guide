package com.madmaxlgndklr.resonanceguide.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

private val Default = Typography()

// Serif headings for a storybook feel; body text stays on the system sans for
// legibility on the dense QHD+ panel.
val ResonanceTypography = Typography(
    displaySmall = Default.displaySmall.copy(fontFamily = FontFamily.Serif),
    headlineLarge = Default.headlineLarge.copy(fontFamily = FontFamily.Serif),
    headlineMedium = Default.headlineMedium.copy(fontFamily = FontFamily.Serif),
    headlineSmall = Default.headlineSmall.copy(fontFamily = FontFamily.Serif),
    titleLarge = Default.titleLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
    titleMedium = Default.titleMedium.copy(fontWeight = FontWeight.SemiBold),
)
