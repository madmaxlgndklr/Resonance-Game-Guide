package com.madmaxlgndklr.resonanceguide.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = Crystal80,
    onPrimary = Night,
    primaryContainer = CrystalContainerDark,
    onPrimaryContainer = CrystalContainerLight,
    secondary = Gold80,
    onSecondary = Night,
    secondaryContainer = GoldContainerDark,
    onSecondaryContainer = GoldContainerLight,
    tertiary = Violet80,
    onTertiary = Night,
    background = Night,
    onBackground = Mist,
    surface = Night,
    onSurface = Mist,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = Mist.copy(alpha = 0.8f),
    surfaceContainerLowest = Night,
    surfaceContainerLow = NightSurface,
    surfaceContainer = NightSurface,
    surfaceContainerHigh = NightSurfaceVariant,
    outline = NightOutline,
    error = ErrorRed80,
)

private val LightColors = lightColorScheme(
    primary = Crystal40,
    onPrimary = Parchment,
    primaryContainer = CrystalContainerLight,
    onPrimaryContainer = Ink,
    secondary = Gold40,
    onSecondary = Parchment,
    secondaryContainer = GoldContainerLight,
    onSecondaryContainer = Ink,
    tertiary = Violet40,
    background = Parchment,
    onBackground = Ink,
    surface = Parchment,
    onSurface = Ink,
    surfaceVariant = ParchmentSurfaceVariant,
    onSurfaceVariant = Ink.copy(alpha = 0.8f),
    outline = NightOutline,
    error = ErrorRed40,
)

/**
 * @param dynamicColor use the wallpaper-derived Material You palette (Android 12+)
 *   instead of the Resonance palette. Off by default to keep the game's look.
 */
@Composable
fun ResonanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ResonanceTypography,
        content = content,
    )
}
