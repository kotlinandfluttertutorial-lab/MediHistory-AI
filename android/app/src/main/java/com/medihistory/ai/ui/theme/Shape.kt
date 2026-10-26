package com.medihistory.ai.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * MediHistory AI shape scale.
 * Follows Material 3 defaults; named here so all composables reference
 * MaterialTheme.shapes rather than hardcoding corner radii.
 */
val MediHistoryShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),  // Chip, badge, tooltip
    small      = RoundedCornerShape(8.dp),  // Button, text field, menu item
    medium     = RoundedCornerShape(12.dp), // Card, dialog, bottom sheet header (PrescriptionCard, MedicineCard)
    large      = RoundedCornerShape(16.dp), // Bottom sheet, side sheet, navigation drawer
    extraLarge = RoundedCornerShape(28.dp), // FAB, full-screen modal, large card
)

// Alias for compatibility — MediHistoryShapes is the canonical name used by Theme.kt
// (Renaming to `Shapes` would shadow the androidx.compose.material3.Shapes import.)
val MhShapes = MediHistoryShapes
