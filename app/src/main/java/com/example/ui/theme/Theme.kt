package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = BreadBlueDark,
    onPrimary = BreadOnPrimaryDark,
    primaryContainer = BreadBlueContainerDark,
    onPrimaryContainer = BreadOnBlueContainerDark,
    secondary = CrustBrownLight,
    onSecondary = BreadOnPrimaryDark,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = MascotBlushPink,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    outline = OutlineDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = BreadBluePrimary,
    onPrimary = BreadOnPrimary,
    primaryContainer = BreadBlueContainer,
    onPrimaryContainer = BreadOnBlueContainer,
    secondary = CrustBrown,
    onSecondary = BreadOnPrimary,
    secondaryContainer = SurfaceVariantLight,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = MascotBlushPink,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    outline = OutlineLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight
)

@Composable
fun BreadcrumbTheme(
    darkTheme: Boolean = false, // Default to the signature warm cream daylight theme from reference
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
