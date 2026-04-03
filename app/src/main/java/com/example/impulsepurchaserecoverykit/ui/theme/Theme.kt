package com.example.impulsepurchaserecoverykit.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary            = Teal700,
    onPrimary          = IvorySurface,
    primaryContainer   = Teal100,
    onPrimaryContainer = Teal900,

    secondary            = Terra500,
    onSecondary          = IvorySurface,
    secondaryContainer   = Terra50,
    onSecondaryContainer = Terra700,

    tertiary            = Teal500,
    onTertiary          = IvorySurface,
    tertiaryContainer   = Teal50,
    onTertiaryContainer = Teal700,

    error            = Error700,
    onError          = IvorySurface,
    errorContainer   = Error100,
    onErrorContainer = Error700,

    background       = IvoryBg,
    onBackground     = Charcoal900,
    surface          = IvorySurface,
    onSurface        = Charcoal900,
    surfaceVariant   = Teal50,
    onSurfaceVariant = Charcoal700,
    outline          = Charcoal200,
    outlineVariant   = Teal200,
)

private val DarkColorScheme = darkColorScheme(
    primary            = Teal200,
    onPrimary          = Teal900,
    primaryContainer   = Teal900,
    onPrimaryContainer = Teal100,
    secondary            = Terra200,
    onSecondary          = Terra700,
    secondaryContainer   = Terra700,
    onSecondaryContainer = Terra50,
    background       = Color(0xFF0F1E1C),
    onBackground     = Teal50,
    surface          = Color(0xFF1A2E2B),
    onSurface        = Teal50,
    surfaceVariant   = Color(0xFF243330),
    onSurfaceVariant = Teal200,
    outline          = Charcoal700,
)

@Composable
fun ImpulsePurchaseRecoveryKitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}