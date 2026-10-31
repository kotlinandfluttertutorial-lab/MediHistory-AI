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
// Light colour scheme — full Material 3 slot set
// ---------------------------------------------------------------------------
internal val LightColorScheme =
    lightColorScheme(
        primary = MhPrimary,
        onPrimary = MhOnPrimary,
        primaryContainer = MhPrimaryContainer,
        onPrimaryContainer = MhOnPrimaryContainer,
        secondary = MhSecondary,
        onSecondary = MhOnSecondary,
        secondaryContainer = MhSecondaryContainer,
        onSecondaryContainer = MhOnSecondaryContainer,
        tertiary = MhTertiary,
        onTertiary = MhOnTertiary,
        tertiaryContainer = MhTertiaryContainer,
        onTertiaryContainer = MhOnTertiaryContainer,
        error = MhError,
        onError = MhOnError,
        errorContainer = MhErrorContainer,
        onErrorContainer = MhOnErrorContainer,
        background = MhBackground,
        onBackground = MhOnBackground,
        surface = MhSurface,
        onSurface = MhOnSurface,
        surfaceVariant = MhSurfaceVariant,
        onSurfaceVariant = MhOnSurfaceVariant,
        outline = MhOutline,
        outlineVariant = MhOutlineVariant,
        inverseSurface = MhInverseSurface,
        inverseOnSurface = MhInverseOnSurface,
        inversePrimary = MhInversePrimary,
        scrim = MhScrim,
    )

// ---------------------------------------------------------------------------
// Dark colour scheme — proper dark palette using MhDark* constants
// ---------------------------------------------------------------------------
internal val DarkColorScheme =
    darkColorScheme(
        primary = MhDarkPrimary,
        onPrimary = MhDarkOnPrimary,
        primaryContainer = MhDarkPrimaryContainer,
        onPrimaryContainer = MhDarkOnPrimaryContainer,
        secondary = MhDarkSecondary,
        onSecondary = MhDarkOnSecondary,
        secondaryContainer = MhDarkSecondaryContainer,
        onSecondaryContainer = MhDarkOnSecondaryContainer,
        tertiary = MhDarkTertiary,
        onTertiary = MhDarkOnTertiary,
        tertiaryContainer = MhDarkTertiaryContainer,
        onTertiaryContainer = MhDarkOnTertiaryContainer,
        error = MhDarkError,
        onError = MhDarkOnError,
        errorContainer = MhDarkErrorContainer,
        onErrorContainer = MhDarkOnErrorContainer,
        background = MhDarkBackground,
        onBackground = MhDarkOnBackground,
        surface = MhDarkSurface,
        onSurface = MhDarkOnSurface,
        surfaceVariant = MhDarkSurfaceVariant,
        onSurfaceVariant = MhDarkOnSurfaceVariant,
        outline = MhDarkOutline,
        outlineVariant = MhDarkOutlineVariant,
        inverseSurface = MhDarkInverseSurface,
        inverseOnSurface = MhDarkInverseOnSurface,
        inversePrimary = MhDarkInversePrimary,
        scrim = MhDarkScrim,
    )

/**
 * Convenience accessor for the custom MediHistory extended colour tokens.
 * Usage: MaterialTheme.extendedColors.warning
 */
val MaterialTheme.extendedColors: MediHistoryExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalMediHistoryColors.current

// ---------------------------------------------------------------------------
// Top-level theme composable
// ---------------------------------------------------------------------------

/**
 * MediHistory AI application theme.
 *
 * Wraps all screens. Provides Material 3 theming plus custom extended colour
 * tokens via [LocalMediHistoryColors].
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
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    val extendedColorScheme = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalMediHistoryColors provides extendedColorScheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MediHistoryTypography,
            shapes = MediHistoryShapes,
            content = content,
        )
    }
}
