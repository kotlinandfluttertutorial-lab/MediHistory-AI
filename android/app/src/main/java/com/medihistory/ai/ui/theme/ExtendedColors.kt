package com.medihistory.ai.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// MediHistory AI — Extended colour tokens
// These tokens have no slot in Material 3's built-in ColorScheme.
// All composables that display warning, status, OCR, or confidence UI must
// use these tokens rather than hardcoding colours.
// Access via MaterialTheme.extendedColors (wired in Theme.kt).
// ---------------------------------------------------------------------------

/**
 * Extended colour tokens not covered by Material 3 ColorScheme.
 */
data class MediHistoryExtendedColors(
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val statusCurrent: Color,
    val statusPast: Color,
    val statusUnknown: Color,
    val ocrReview: Color,
    val ocrFailed: Color,
    val confidenceHigh: Color,
    val onConfidenceHigh: Color,
)

val LightExtendedColors = MediHistoryExtendedColors(
    warning           = Color(0xFF7D5700),
    onWarning         = Color(0xFFFFFFFF),
    warningContainer  = Color(0xFFFFDEA8),
    onWarningContainer = Color(0xFF271900),
    statusCurrent     = Color(0xFF006874),
    statusPast        = Color(0xFF4A6267),
    statusUnknown     = Color(0xFF6F797A),
    ocrReview         = Color(0xFF7D5700),
    ocrFailed         = Color(0xFFBA1A1A),
    confidenceHigh    = Color(0xFFDAE2FF),
    onConfidenceHigh  = Color(0xFF0E1B37),
)

val DarkExtendedColors = MediHistoryExtendedColors(
    warning           = Color(0xFFFFBA3B),
    onWarning         = Color(0xFF000000),
    warningContainer  = Color(0xFF5E4100),
    onWarningContainer = Color(0xFFFFDEA8),
    statusCurrent     = Color(0xFF4BD3E5),
    statusPast        = Color(0xFFB0CBD1),
    statusUnknown     = Color(0xFF899294),
    ocrReview         = Color(0xFFFFBA3B),
    ocrFailed         = Color(0xFFFFB4AB),
    confidenceHigh    = Color(0xFF394778),
    onConfidenceHigh  = Color(0xFFDAE2FF),
)

val LocalMediHistoryColors = staticCompositionLocalOf { LightExtendedColors }
