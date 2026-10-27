package com.medihistory.ai.duplicate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [IngredientNormalizer].
 *
 * Synthetic ingredient names are used where real names are not required for the test.
 * Tests cover the synonym map, casing normalisation, and unknown ingredient fallback.
 */
class IngredientNormalizerTest {

    // --- normalize ---

    @Test
    fun `normalize paracetamol lowercase returns paracetamol`() {
        assertEquals("paracetamol", IngredientNormalizer.normalize("paracetamol"))
    }

    @Test
    fun `normalize Paracetamol mixed case returns paracetamol`() {
        assertEquals("paracetamol", IngredientNormalizer.normalize("Paracetamol"))
    }

    @Test
    fun `normalize PARACETAMOL uppercase returns paracetamol`() {
        assertEquals("paracetamol", IngredientNormalizer.normalize("PARACETAMOL"))
    }

    @Test
    fun `normalize Acetaminophen synonym returns canonical paracetamol`() {
        assertEquals("paracetamol", IngredientNormalizer.normalize("Acetaminophen"))
    }

    @Test
    fun `normalize acetaminophen lowercase returns canonical paracetamol`() {
        assertEquals("paracetamol", IngredientNormalizer.normalize("acetaminophen"))
    }

    @Test
    fun `normalize unknown ingredient returns lowercase trimmed form`() {
        assertEquals("synthamol", IngredientNormalizer.normalize("Synthamol"))
    }

    @Test
    fun `normalize unknown ingredient with leading and trailing spaces trims correctly`() {
        assertEquals("testocillin", IngredientNormalizer.normalize("  Testocillin  "))
    }

    @Test
    fun `normalize metoprolol succinate returns metoprolol succinate`() {
        assertEquals("metoprolol succinate", IngredientNormalizer.normalize("Metoprolol Succinate"))
    }

    @Test
    fun `normalize metoprolol tartrate returns metoprolol tartrate`() {
        assertEquals("metoprolol tartrate", IngredientNormalizer.normalize("Metoprolol Tartrate"))
    }

    @Test
    fun `normalize metformin hydrochloride maps to canonical metformin`() {
        assertEquals("metformin", IngredientNormalizer.normalize("Metformin Hydrochloride"))
    }

    @Test
    fun `normalize amlodipine besylate maps to canonical amlodipine`() {
        assertEquals("amlodipine", IngredientNormalizer.normalize("Amlodipine Besylate"))
    }

    @Test
    fun `normalize acetylsalicylic acid maps to canonical aspirin`() {
        assertEquals("aspirin", IngredientNormalizer.normalize("Acetylsalicylic Acid"))
    }

    // --- isSynonym ---

    @Test
    fun `isSynonym paracetamol and acetaminophen returns true`() {
        assertTrue(IngredientNormalizer.isSynonym("paracetamol", "acetaminophen"))
    }

    @Test
    fun `isSynonym acetaminophen and paracetamol is symmetric`() {
        assertTrue(IngredientNormalizer.isSynonym("acetaminophen", "paracetamol"))
    }

    @Test
    fun `isSynonym paracetamol and paracetamol returns true`() {
        assertTrue(IngredientNormalizer.isSynonym("paracetamol", "paracetamol"))
    }

    @Test
    fun `isSynonym Paracetamol and Acetaminophen mixed case returns true`() {
        assertTrue(IngredientNormalizer.isSynonym("Paracetamol", "Acetaminophen"))
    }

    @Test
    fun `isSynonym paracetamol and metformin returns false`() {
        assertFalse(IngredientNormalizer.isSynonym("paracetamol", "metformin"))
    }

    @Test
    fun `isSynonym metoprolol succinate and metoprolol tartrate returns false`() {
        // Different salts are distinct — NOT synonyms
        assertFalse(
            IngredientNormalizer.isSynonym("metoprolol succinate", "metoprolol tartrate"),
        )
    }

    @Test
    fun `isSynonym unknown ingredients with same lowercase text returns true`() {
        assertTrue(IngredientNormalizer.isSynonym("Synthamol 500mg", "synthamol 500mg"))
    }

    @Test
    fun `isSynonym two different unknown ingredients returns false`() {
        assertFalse(IngredientNormalizer.isSynonym("Synthamol", "Testocillin"))
    }
}
