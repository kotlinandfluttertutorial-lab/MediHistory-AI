package com.medihistory.ai.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// MediHistory AI — Extended colour tokens
// These tokens have no slot in Material 3's built-in ColorScheme.
// All composables that display warning, status, OCR, or confidence UI must
// use these tokens rather than hardcoding colours.
// Access via MaterialTheme.extendedColors (wired in Theme.kt).
//
// Migration note: the accessor was renamed from `mediHistoryColors` to
// `extendedColors` in MA-21. Any feature branch compiled against the base
// branch that calls `MaterialTheme.mediHistoryColors` will get a compile
// error — update the call site to `MaterialTheme.extendedColors`.
// ---------------------------------------------------------------------------

/**
 * Extended colour tokens not covered by Material 3 ColorScheme.
 */
data class MediHistoryExtendedColors(
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    // Status chip colors — separate current/past/unknown medication state visually.
    // onStatus* counterparts ensure badge text never needs a hardcoded colour.
    val statusCurrent: Color,
    val onStatusCurrent: Color,
    val statusPast: Color,
    val onStatusPast: Color,
    val statusUnknown: Color,
    val onStatusUnknown: Color,
    val ocrReview: Color,
    val ocrFailed: Color,
    val confidenceHigh: Color,
    val onConfidenceHigh: Color,
)

val LightExtendedColors =
    MediHistoryExtendedColors(
        warning = MhWarning,
        onWarning = MhOnWarning,
        warningContainer = MhWarningContainer,
        onWarningContainer = MhOnWarningContainer,
        statusCurrent = MhPrimary,
        onStatusCurrent = Color(0xFFFFFFFF), // white on primary teal
        statusPast = MhSecondary,
        onStatusPast = Color(0xFFFFFFFF), // white on secondary
        statusUnknown = MhOutline,
        onStatusUnknown = Color(0xFFFFFFFF), // white on outline grey
        ocrReview = MhWarning,
        ocrFailed = MhError,
        confidenceHigh = MhTertiaryContainer,
        onConfidenceHigh = MhOnTertiaryContainer,
    )

val DarkExtendedColors =
    MediHistoryExtendedColors(
        warning = MhDarkWarning,
        onWarning = MhDarkOnWarning,
        warningContainer = MhDarkWarningContainer,
        onWarningContainer = MhDarkOnWarningContainer,
        statusCurrent = MhDarkPrimary,
        onStatusCurrent = Color(0xFF00363D), // onPrimary dark
        statusPast = MhDarkSecondary,
        onStatusPast = Color(0xFF1B3439), // onSecondary dark
        statusUnknown = MhDarkOutline,
        onStatusUnknown = Color(0xFF171D1E), // onSurface dark
        ocrReview = MhDarkWarning,
        ocrFailed = MhDarkError,
        confidenceHigh = MhDarkTertiaryContainer,
        onConfidenceHigh = MhDarkOnTertiaryContainer,
    )

val LocalMediHistoryColors = staticCompositionLocalOf { LightExtendedColors }
