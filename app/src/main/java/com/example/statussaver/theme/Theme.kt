package com.example.statussaver.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme

private fun getDarkColorScheme(colorTheme: String) = darkColorScheme(
    primary = when (colorTheme) {
        "Red" -> RedPrimary
        "Orange" -> OrangePrimary
        "Blue" -> BluePrimary
        "Yellow" -> YellowPrimary
        "Green" -> GreenPrimary
        "Pink" -> PinkPrimary
        "Dark Green" -> DarkGreenPrimary
        else -> NeonViolet
    },
    onPrimary = OnDark,
    primaryContainer = when (colorTheme) {
        "Red" -> RedDark
        "Orange" -> OrangeDark
        "Blue" -> BlueDark
        "Yellow" -> YellowDark
        "Green" -> GreenDark
        "Pink" -> PinkDark
        "Dark Green" -> DarkGreenDark
        else -> NeonVioletDark
    },
    onPrimaryContainer = when (colorTheme) {
        "Red" -> RedLight
        "Orange" -> OrangeLight
        "Blue" -> BlueLight
        "Yellow" -> YellowLight
        "Green" -> GreenLight
        "Pink" -> PinkLight
        "Dark Green" -> DarkGreenLight
        else -> NeonVioletLight
    },
    secondary = when (colorTheme) {
        "Red" -> RedSecondary
        "Orange" -> OrangeSecondary
        "Blue" -> BlueSecondary
        "Yellow" -> YellowSecondary
        "Green" -> GreenSecondary
        "Pink" -> PinkSecondary
        "Dark Green" -> DarkGreenSecond
        else -> ElectricBlue
    },
    onSecondary = OnDark,
    secondaryContainer = Color(0xFF1A237E), // Keep subtle background for all
    onSecondaryContainer = when (colorTheme) {
        "Red" -> RedLight
        "Orange" -> OrangeLight
        "Blue" -> BlueLight
        "Yellow" -> YellowLight
        "Green" -> GreenLight
        "Pink" -> PinkLight
        "Dark Green" -> DarkGreenLight
        else -> ElectricBlueLight
    },
    tertiary = when (colorTheme) {
        "Red" -> RedTertiary
        "Orange" -> OrangeTertiary
        "Blue" -> BlueTertiary
        "Yellow" -> YellowTertiary
        "Green" -> GreenTertiary
        "Pink" -> PinkTertiary
        "Dark Green" -> DarkGreenTert
        else -> DeepTeal
    },
    onTertiary = DarkBg,
    background = DarkBg,
    onBackground = OnDark,
    surface = DarkSurface,
    onSurface = OnDark,
    surfaceVariant = DarkSurfaceVar,
    onSurfaceVariant = OnDarkVariant,
    error = ErrorRed,
    onError = OnDark,
)

private fun getLightColorScheme(colorTheme: String) = lightColorScheme(
    primary = when (colorTheme) {
        "Red" -> RedPrimary
        "Orange" -> OrangePrimary
        "Blue" -> BluePrimary
        "Yellow" -> YellowDark // Darker yellow for visibility on light bg
        "Green" -> GreenPrimary
        "Pink" -> PinkPrimary
        "Dark Green" -> DarkGreenPrimary
        else -> NeonViolet
    },
    onPrimary = Color.White,
    primaryContainer = when (colorTheme) {
        "Red" -> RedLight
        "Orange" -> OrangeLight
        "Blue" -> BlueLight
        "Yellow" -> YellowLight
        "Green" -> GreenLight
        "Pink" -> PinkLight
        "Dark Green" -> DarkGreenLight
        else -> NeonVioletLight
    },
    onPrimaryContainer = OnLight,
    secondary = when (colorTheme) {
        "Red" -> RedSecondary
        "Orange" -> OrangeSecondary
        "Blue" -> BlueSecondary
        "Yellow" -> YellowSecondary
        "Green" -> GreenSecondary
        "Pink" -> PinkSecondary
        "Dark Green" -> DarkGreenSecond
        else -> ElectricBlue
    },
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3F2FD),
    onSecondaryContainer = OnLight,
    tertiary = when (colorTheme) {
        "Red" -> RedTertiary
        "Orange" -> OrangeTertiary
        "Blue" -> BlueTertiary
        "Yellow" -> YellowTertiary
        "Green" -> GreenTertiary
        "Pink" -> PinkTertiary
        "Dark Green" -> DarkGreenTert
        else -> DeepTeal
    },
    onTertiary = OnLight,
    background = LightBg,
    onBackground = OnLight,
    surface = LightSurface,
    onSurface = OnLight,
    surfaceVariant = LightSurfaceVar,
    onSurfaceVariant = OnLightVariant,
    error = ErrorRed,
    onError = Color.White,
)

@Composable
fun StatusSaverTheme(
    isDarkMode: Boolean = isSystemInDarkTheme(),
    themeColor: String = "Violet",
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkMode) {
        getDarkColorScheme(themeColor)
    } else {
        getLightColorScheme(themeColor)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
