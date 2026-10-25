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
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(8.dp),
    medium     = RoundedCornerShape(12.dp),
    large      = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
