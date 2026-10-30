package com.medihistory.ai.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Unit tests for the MediHistory AI Material 3 theme token correctness.
 *
 * These are plain JVM tests — Color is a pure Kotlin/JVM value type.
 * No Android runtime, Robolectric, or Compose instrumentation required.
 */
class ThemeTest {
    @Test
    fun should_have_correct_light_primary_color() {
        // LightColorScheme.primary maps to MhPrimary = #006874
        assertEquals(Color(0xFF006874), LightColorScheme.primary)
    }

    @Test
    fun should_have_correct_dark_primary_color() {
        // DarkColorScheme.primary maps to MhDarkPrimary = #4BD3E5
        assertEquals(Color(0xFF4BD3E5), DarkColorScheme.primary)
    }

    @Test
    fun should_have_warning_color_different_from_error() {
        // Warning is a distinct semantic from error
        assertNotEquals(LightExtendedColors.warning, LightColorScheme.error)
    }

    @Test
    fun should_have_no_unspecified_colors_in_light_scheme() {
        val scheme = LightColorScheme
        // Check key slots are all specified
        assertNotEquals(Color.Unspecified, scheme.primary)
        assertNotEquals(Color.Unspecified, scheme.secondary)
        assertNotEquals(Color.Unspecified, scheme.tertiary)
        assertNotEquals(Color.Unspecified, scheme.error)
        assertNotEquals(Color.Unspecified, scheme.background)
        assertNotEquals(Color.Unspecified, scheme.surface)
    }

    @Test
    fun should_have_no_unspecified_colors_in_extended_light_colors() {
        val c = LightExtendedColors
        listOf(
            c.warning, c.onWarning, c.warningContainer, c.onWarningContainer,
            c.statusCurrent, c.statusPast, c.statusUnknown,
            c.ocrReview, c.ocrFailed, c.confidenceHigh, c.onConfidenceHigh,
        ).forEachIndexed { i, color ->
            assertNotEquals(
                "Extended light colour field $i must not be Color.Unspecified",
                Color.Unspecified,
                color,
            )
        }
    }

    @Test
    fun should_have_no_unspecified_colors_in_extended_dark_colors() {
        val c = DarkExtendedColors
        listOf(
            c.warning, c.onWarning, c.warningContainer, c.onWarningContainer,
            c.statusCurrent, c.statusPast, c.statusUnknown,
            c.ocrReview, c.ocrFailed, c.confidenceHigh, c.onConfidenceHigh,
        ).forEachIndexed { i, color ->
            assertNotEquals(
                "Extended dark colour field $i must not be Color.Unspecified",
                Color.Unspecified,
                color,
            )
        }
    }
}
