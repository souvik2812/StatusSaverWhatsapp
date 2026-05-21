package com.example.statussaver.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Always-dark Material 3 colour scheme that matches our glassmorphism palette.
 * Dynamic colour is intentionally disabled so the branding is consistent across devices.
 */
private val AppDarkColorScheme = darkColorScheme(
    primary              = NeonViolet,
    onPrimary            = OnDark,
    primaryContainer     = NeonVioletDark,
    onPrimaryContainer   = NeonVioletLight,

    secondary            = ElectricBlue,
    onSecondary          = OnDark,
    secondaryContainer   = Color(0xFF1A237E),
    onSecondaryContainer = ElectricBlueLight,

    tertiary             = DeepTeal,
    onTertiary           = DarkBg,

    background           = DarkBg,
    onBackground         = OnDark,

    surface              = DarkSurface,
    onSurface            = OnDark,
    surfaceVariant       = DarkSurfaceVar,
    onSurfaceVariant     = OnDarkVariant,

    error                = ErrorRed,
    onError              = OnDark,
)

@Composable
fun StatusSaverTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppDarkColorScheme,
        typography  = Typography,
        content     = content
    )
}
