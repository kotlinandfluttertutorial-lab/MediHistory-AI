package com.medihistory.ai.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

// ---------------------------------------------------------------------------
// Light colour scheme
// ---------------------------------------------------------------------------
private val LightColorScheme = lightColorScheme(
    primary             = MhPrimary,
    onPrimary           = MhOnPrimary,
    primaryContainer    = MhPrimaryContainer,
    onPrimaryContainer  = MhOnPrimaryContainer,
    secondary           = MhSecondary,
    secondaryContainer  = MhSecondaryContainer,
    error               = MhError,
    errorContainer      = MhErrorContainer,
    surface             = MhSurface,
    onSurface           = MhOnSurface,
    surfaceVariant      = MhSurfaceVariant,
    outline             = MhOutline,
)

// ---------------------------------------------------------------------------
// Dark colour scheme — mirrors light scheme; full dark palette to be added
// in the design system ticket (MHA-010).
// ---------------------------------------------------------------------------
private val DarkColorScheme = darkColorScheme(
    primary             = MhPrimaryContainer,
    onPrimary           = MhOnPrimaryContainer,
    primaryContainer    = MhPrimary,
    onPrimaryContainer  = MhPrimaryContainer,
    secondary           = MhSecondaryContainer,
    secondaryContainer  = MhSecondary,
    error               = MhErrorContainer,
    errorContainer      = MhError,
)

// ---------------------------------------------------------------------------
// Warning token extension — custom colours not in Material 3 ColorScheme.
// ---------------------------------------------------------------------------
private val LightWarningScheme = MediHistoryColorScheme(
    warning          = MhWarning,
    warningContainer = MhWarningContainer,
)

private val DarkWarningScheme = MediHistoryColorScheme(
    warning          = MhWarningContainer,
    warningContainer = MhWarning,
)

/**
 * Convenience accessor for the custom MediHistory colour extension.
 * Usage: MaterialTheme.mediHistoryColors.warning
 */
val MaterialTheme.mediHistoryColors: MediHistoryColorScheme
    @Composable
    @ReadOnlyComposable
    get() = LocalMediHistoryColorScheme.current

// ---------------------------------------------------------------------------
// Top-level theme composable
// ---------------------------------------------------------------------------

/**
 * MediHistory AI application theme.
 *
 * Wraps all screens. Provides Material 3 theming plus the custom warning
 * colour extension via [LocalMediHistoryColorScheme].
 *
 * @param darkTheme     Follow system dark-mode setting by default.
 * @param dynamicColor  Use Android 12+ dynamic colour if available (default on).
 *                      Disable in preview/test.
 * @param content       The composable content tree.
 */
@Composable
fun MediHistoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val warningScheme = if (darkTheme) DarkWarningScheme else LightWarningScheme

    CompositionLocalProvider(LocalMediHistoryColorScheme provides warningScheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = MediHistoryTypography,
            shapes      = MediHistoryShapes,
            content     = content,
        )
    }
}
