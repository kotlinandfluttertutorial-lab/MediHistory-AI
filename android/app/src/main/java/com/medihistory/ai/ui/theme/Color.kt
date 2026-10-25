package com.medihistory.ai.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// MediHistory AI — Design token colours
// Source: docs/design/design-system.md
// ---------------------------------------------------------------------------

// Primary palette
val MhPrimary             = Color(0xFF006874)
val MhOnPrimary           = Color(0xFFFFFFFF)
val MhPrimaryContainer    = Color(0xFF97F0FF)
val MhOnPrimaryContainer  = Color(0xFF001F24)

// Secondary palette
val MhSecondary           = Color(0xFF4A6267)
val MhSecondaryContainer  = Color(0xFFCCE8ED)

// Error palette
val MhError               = Color(0xFFBA1A1A)
val MhErrorContainer      = Color(0xFFFFDAD6)

// Warning palette (custom extension — no Material 3 slot)
val MhWarning             = Color(0xFF7D5700)
val MhWarningContainer    = Color(0xFFFFDEA8)

// Surface / neutral palette
val MhSurface             = Color(0xFFF5FAFB)
val MhOnSurface           = Color(0xFF171D1E)
val MhSurfaceVariant      = Color(0xFFDBE4E6)
val MhOutline             = Color(0xFF6F797A)

// ---------------------------------------------------------------------------
// Custom colour scheme extension for warning tokens.
// Access via MaterialTheme.mediHistoryColors (wired in Theme.kt).
// ---------------------------------------------------------------------------

/**
 * Extended colour tokens that have no slot in Material 3's built-in ColorScheme.
 * All composables that display warning UI must use these tokens rather than
 * hardcoding colours.
 */
data class MediHistoryColorScheme(
    val warning: Color,
    val onWarning: Color = Color(0xFFFFFFFF),
    val warningContainer: Color,
    val onWarningContainer: Color = Color(0xFF271900),
)

val LocalMediHistoryColorScheme = staticCompositionLocalOf {
    MediHistoryColorScheme(
        warning = MhWarning,
        warningContainer = MhWarningContainer,
    )
}
