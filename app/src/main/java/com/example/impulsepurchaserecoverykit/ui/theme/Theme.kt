package com.example.impulsepurchaserecoverykit.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Light — Soft Midnight + Warm Stone ────────────────────────────────────
private val LightColorScheme = lightColorScheme(

    // Primary — midnight blue
    primary            = Teal700,        // #1C2646
    onPrimary          = IvorySurface,
    primaryContainer   = Teal50,         // barely-blue chip backgrounds
    onPrimaryContainer = Teal900,
    inversePrimary     = Teal200,

    // Secondary — warm stone
    secondary            = Terra500,     // #9E8060
    onSecondary          = IvorySurface,
    secondaryContainer   = Terra50,
    onSecondaryContainer = Terra700,

    // Tertiary — mid blue (used by M3 for complementary tonal role)
    tertiary            = Teal500,
    onTertiary          = IvorySurface,
    tertiaryContainer   = Teal50,
    onTertiaryContainer = Teal700,

    // Error
    error            = Error700,
    onError          = IvorySurface,
    errorContainer   = Error100,
    onErrorContainer = Error700,

    // Backgrounds & surfaces
    // Pure white cards on cool grey — strong depth separation
    background       = IvoryBg,          // #EBEBED cool grey
    onBackground     = Charcoal900,
    surface          = IvorySurface,     // #FFFFFF — visibly lifts off grey bg
    onSurface        = Charcoal900,
    surfaceVariant   = Teal50,           // barely-blue — tinted chip/card surfaces
    onSurfaceVariant = Charcoal700,
    surfaceTint      = Teal700,          // M3 elevation tint

    // M3 Expressive surface container tones
    surfaceContainerLowest  = IvorySurface,
    surfaceContainerLow     = Color(0xFFF4F4F6),
    surfaceContainer        = Color(0xFFEEEEF2),
    surfaceContainerHigh    = Color(0xFFE6E7EC),
    surfaceContainerHighest = Color(0xFFDEDFE6),

    // Inverse
    inverseSurface   = Teal900,
    inverseOnSurface = Teal50,

    // Outlines
    outline        = Charcoal200,
    outlineVariant = Teal100,

    // Scrim
    scrim = Color(0xFF000000),
)

// ── Dark — Soft Midnight + Warm Stone ─────────────────────────────────────
// Deep navy-black backgrounds — richer and more premium than neutral grey.
private val DarkColorScheme = darkColorScheme(

    primary            = Teal200,
    onPrimary          = Teal900,
    primaryContainer   = Teal700,
    onPrimaryContainer = Teal100,
    inversePrimary     = Teal700,

    secondary            = Terra200,
    onSecondary          = Terra700,
    secondaryContainer   = Terra700,
    onSecondaryContainer = Terra50,

    tertiary            = Teal200,
    onTertiary          = Teal900,
    tertiaryContainer   = Teal900,
    onTertiaryContainer = Teal100,

    error            = Color(0xFFFFB4AB),
    onError          = Color(0xFF690005),
    errorContainer   = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    // Deep midnight navy backgrounds — no grey, no purple
    background       = Color(0xFF0A0E1A),   // near-black midnight
    onBackground     = Teal100,
    surface          = Color(0xFF141826),   // dark navy surface
    onSurface        = Teal100,
    surfaceVariant   = Color(0xFF1E2438),
    onSurfaceVariant = Teal200,
    surfaceTint      = Teal200,

    surfaceContainerLowest  = Color(0xFF080C18),
    surfaceContainerLow     = Color(0xFF10141E),
    surfaceContainer        = Color(0xFF181C2A),
    surfaceContainerHigh    = Color(0xFF202434),
    surfaceContainerHighest = Color(0xFF282C3E),

    inverseSurface   = Teal100,
    inverseOnSurface = Teal900,

    outline        = Charcoal700,
    outlineVariant = Color(0xFF2A3050),
    scrim          = Color(0xFF000000),
)

// ── Theme ─────────────────────────────────────────────────────────────────
@Composable
fun ImpulsePurchaseRecoveryKitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,       // false = always use brand palette
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor        = colorScheme.primary.toArgb()
            window.navigationBarColor    = Color.Transparent.toArgb()
            val ctrl = WindowCompat.getInsetsController(window, view)
            ctrl.isAppearanceLightStatusBars     = false   // white icons on dark navy
            ctrl.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        content     = content
    )
}