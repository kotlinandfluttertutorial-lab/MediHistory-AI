package com.medihistory.ai.duplicate

import com.medihistory.ai.data.model.MatchType
import com.medihistory.ai.data.model.Medicine
import com.medihistory.ai.data.model.MedicineStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [DuplicateDetector].
 *
 * 100% branch coverage is required on [DuplicateDetector] and [IngredientNormalizer].
 *
 * All fixtures use synthetic data:
 * - Patient IDs: "test-patient-001"
 * - Medicine IDs: "med-001", "med-002", etc.
 * - Medicine names: "Synthamol 500mg", "Testocillin 250mg", etc.
 * - Active ingredients: "paracetamol", "testocillin", etc.
 *
 * No real brand names or real patient names are used.
 */
class DuplicateDetectorTest {
    // --- Fixture builder ---

    private fun medicine(
        id: String,
        medicineName: String,
        activeIngredient: String?,
        status: MedicineStatus = MedicineStatus.CURRENT,
        patientId: String = "test-patient-001",
        prescriptionId: String = "rx-001",
    ) = Medicine(
        id = id,
        prescriptionId = prescriptionId,
        patientId = patientId,
        medicineName = medicineName,
        activeIngredient = activeIngredient,
        status = status,
    )

    // --- Core duplicate detection scenarios ---

    @Test
    fun `should return EXACT match when two medicines have same active ingredient different brands`() {
        val medicines =
            listOf(
                medicine("med-001", "Synthamol 500mg", "paracetamol"),
                medicine("med-002", "Testamol XR", "paracetamol"),
            )
        val result = DuplicateDetector.detect(medicines)

        assertEquals(1, result.size)
        val candidate = result[0]
        assertEquals("med-001", candidate.medicineAId)
        assertEquals("med-002", candidate.medicineBId)
        assertEquals("paracetamol", candidate.matchingIngredient)
        assertEquals(MatchType.EXACT, candidate.matchType)
        assertEquals(0.95f, candidate.confidence)
        assertNull(candidate.combinationNote)
    }

    @Test
    fun `should return no match when medicines have different active ingredients with similar names`() {
        // "testocillin" and "testocillinase" are different — no synonym relationship
        val medicines =
            listOf(
                medicine("med-001", "Testocillin 250mg", "testocillin"),
                medicine("med-002", "Testocillinase 100mg", "testocillinase"),
            )
        val result = DuplicateDetector.detect(medicines)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `should return PARTIAL match when combination product contains ingredient of single medicine`() {
        // med-001 = "synthamol" (single ingredient)
        // med-002 = "synthamol+testocillin" (combination containing synthamol)
        val medicines =
            listOf(
                medicine("med-001", "Synthamol 500mg", "synthamol"),
                medicine("med-002", "Synthacillin-T Combo", "synthamol+testocillin"),
            )
        val result = DuplicateDetector.detect(medicines)

        assertEquals(1, result.size)
        val candidate = result[0]
        assertEquals(MatchType.PARTIAL, candidate.matchType)
        assertEquals("synthamol", candidate.matchingIngredient)
        assertEquals(0.70f, candidate.confidence)
        val note = candidate.combinationNote
        assertNotNull("PARTIAL match must have a combinationNote", note)
        // Combination note must name both medicines and the matching ingredient
        assertTrue(note!!.contains("synthamol"))
        assertTrue(note.contains("Synthamol 500mg"))
        assertTrue(note.contains("Synthacillin-T Combo"))
    }

    @Test
    fun `should return EXACT match when two combination products share all ingredients`() {
        // Both have identical ingredient string — full exact match
        val medicines =
            listOf(
                medicine("med-001", "Synthacillin Combo A", "synthamol+testocillin"),
                medicine("med-002", "Synthacillin Combo B", "synthamol+testocillin"),
            )
        val result = DuplicateDetector.detect(medicines)

        assertEquals(1, result.size)
        assertEquals(MatchType.EXACT, result[0].matchType)
        assertEquals(0.95f, result[0].confidence)
    }

    @Test
    fun `should return EXACT match when paracetamol meets acetaminophen synonym`() {
        val medicines =
            listOf(
                medicine("med-001", "Synthamol 500mg", "paracetamol"),
                medicine("med-002", "Acetamol 500mg", "acetaminophen"),
            )
        val result = DuplicateDetector.detect(medicines)

        assertEquals(1, result.size)
        val candidate = result[0]
        assertEquals(MatchType.EXACT, candidate.matchType)
        // Both normalise to "paracetamol"
        assertEquals("paracetamol", candidate.matchingIngredient)
        assertEquals(0.95f, candidate.confidence)
        assertNull(candidate.combinationNote)
    }

    @Test
    fun `should exclude PAST medicines from detection`() {
        val medicines =
            listOf(
                medicine("med-001", "Synthamol 500mg", "paracetamol", status = MedicineStatus.CURRENT),
                medicine("med-002", "Testamol XR", "paracetamol", status = MedicineStatus.PAST),
            )
        val result = DuplicateDetector.detect(medicines)

        // PAST medicine is excluded — no duplicate found
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should include UNKNOWN status medicines in detection`() {
        val medicines =
            listOf(
                medicine("med-001", "Synthamol 500mg", "paracetamol", status = MedicineStatus.UNKNOWN),
                medicine("med-002", "Testamol XR", "paracetamol", status = MedicineStatus.CURRENT),
            )
        val result = DuplicateDetector.detect(medicines)

        assertEquals(1, result.size)
        assertEquals(MatchType.EXACT, result[0].matchType)
    }

    @Test
    fun `should return empty list when medicines list is empty`() {
        val result = DuplicateDetector.detect(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should return empty list when all medicines have null activeIngredient`() {
        val medicines =
            listOf(
                medicine("med-001", "Synthamol 500mg", null),
                medicine("med-002", "Testamol XR", null),
            )
        val result = DuplicateDetector.detect(medicines)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `should return empty list when all medicines have blank activeIngredient`() {
        val medicines =
            listOf(
                medicine("med-001", "Synthamol 500mg", "   "),
                medicine("med-002", "Testamol XR", ""),
            )
        val result = DuplicateDetector.detect(medicines)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `should return empty list for a single medicine`() {
        val medicines =
            listOf(
                medicine("med-001", "Synthamol 500mg", "paracetamol"),
            )
        val result = DuplicateDetector.detect(medicines)
        assertTrue(result.isEmpty())
    }

    // --- Determinism ---

    @Test
    fun `should produce identical output when called twice with same input`() {
        val medicines =
            listOf(
                medicine("med-001", "Synthamol 500mg", "paracetamol"),
                medicine("med-002", "Acetamol 500mg", "acetaminophen"),
                medicine("med-003", "Ibusynth 200mg", "ibuprofen"),
            )
        val result1 = DuplicateDetector.detect(medicines)
        val result2 = DuplicateDetector.detect(medicines)

        assertEquals(result1, result2)
    }

    @Test
    fun `should produce identical output when input is shuffled`() {
        val medicines =
            listOf(
                medicine("med-001", "Synthamol 500mg", "paracetamol"),
                medicine("med-002", "Acetamol 500mg", "acetaminophen"),
                medicine("med-003", "Ibusynth 200mg", "ibuprofen"),
            )
        val shuffled =
            listOf(
                medicine("med-003", "Ibusynth 200mg", "ibuprofen"),
                medicine("med-001", "Synthamol 500mg", "paracetamol"),
                medicine("med-002", "Acetamol 500mg", "acetaminophen"),
            )
        val result1 = DuplicateDetector.detect(medicines)
        val result2 = DuplicateDetector.detect(shuffled)

        assertEquals(result1, result2)
    }

    // --- Combination product edge cases ---

    @Test
    fun `should return no PARTIAL match when two single-ingredient medicines differ`() {
        // Neither is a combination product — no partial match logic applies
        val medicines =
            listOf(
                medicine("med-001", "Synthamol 500mg", "paracetamol"),
                medicine("med-002", "Ibusynth 200mg", "ibuprofen"),
            )
        val result = DuplicateDetector.detect(medicines)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should return PARTIAL match using slash delimiter in combination product`() {
        val medicines =
            listOf(
                medicine("med-001", "Synthacillin Mono", "synthamol"),
                medicine("med-002", "Synthacillin Combo", "synthamol/testocillin"),
            )
        val result = DuplicateDetector.detect(medicines)

        assertEquals(1, result.size)
        assertEquals(MatchType.PARTIAL, result[0].matchType)
        assertEquals("synthamol", result[0].matchingIngredient)
    }

    @Test
    fun `should return PARTIAL match using comma delimiter in combination product`() {
        val medicines =
            listOf(
                medicine("med-001", "Synthacillin Mono", "synthamol"),
                medicine("med-002", "Synthacillin Combo", "synthamol, testocillin"),
            )
        val result = DuplicateDetector.detect(medicines)

        assertEquals(1, result.size)
        assertEquals(MatchType.PARTIAL, result[0].matchType)
        assertEquals("synthamol", result[0].matchingIngredient)
    }

    @Test
    fun `should return no match for combination ingredients with no overlap`() {
        val medicines =
            listOf(
                medicine("med-001", "Synthacillin Combo", "synthamol+testocillin"),
                medicine("med-002", "Ibusynth Mono", "ibuprofen"),
            )

        assertTrue(DuplicateDetector.detect(medicines).isEmpty())
    }

    @Test
    fun `should ignore empty tokens in combination ingredient strings`() {
        val medicines =
            listOf(
                medicine("med-001", "Synthacillin Mono", "synthamol"),
                medicine("med-002", "Synthacillin Combo", "synthamol++testocillin"),
            )

        val result = DuplicateDetector.detect(medicines)

        assertEquals(1, result.size)
        assertEquals(MatchType.PARTIAL, result.single().matchType)
        assertEquals("synthamol", result.single().matchingIngredient)
    }

    // --- Multiple matches ---

    @Test
    fun `should detect multiple duplicate pairs in a larger medicine list`() {
        val medicines =
            listOf(
                medicine("med-001", "Synthamol 500mg", "paracetamol"),
                medicine("med-002", "Acetamol 500mg", "acetaminophen"), // synonym of med-001
                medicine("med-003", "Ibusynth A", "ibuprofen"),
                medicine("med-004", "Ibusynth B", "ibuprofen"), // duplicate of med-003
            )
        val result = DuplicateDetector.detect(medicines)

        assertEquals(2, result.size)
        // Output is sorted by medicineAId then medicineBId
        assertEquals("med-001", result[0].medicineAId)
        assertEquals("med-002", result[0].medicineBId)
        assertEquals("med-003", result[1].medicineAId)
        assertEquals("med-004", result[1].medicineBId)
    }

    // --- Output ordering ---

    @Test
    fun `should sort output by medicineAId then medicineBId`() {
        val medicines =
            listOf(
                medicine("med-003", "Testamol XR", "paracetamol"),
                medicine("med-001", "Synthamol 500mg", "paracetamol"),
                medicine("med-002", "Acetamol 500mg", "paracetamol"),
            )
        val result = DuplicateDetector.detect(medicines)

        assertEquals(3, result.size)
        // Pairs: (001,002), (001,003), (002,003)
        assertEquals("med-001", result[0].medicineAId)
        assertEquals("med-002", result[0].medicineBId)
        assertEquals("med-001", result[1].medicineAId)
        assertEquals("med-003", result[1].medicineBId)
        assertEquals("med-002", result[2].medicineAId)
        assertEquals("med-003", result[2].medicineBId)
    }

    // --- Medical safety / disclaimer tests ---

    @Test
    fun `DuplicateCandidate combinationNote never contains the word safe`() {
        val medicines =
            listOf(
                medicine("med-001", "Synthamol 500mg", "synthamol"),
                medicine("med-002", "Synthacillin Combo", "synthamol+testocillin"),
            )
        val result = DuplicateDetector.detect(medicines)

        for (candidate in result) {
            val note = candidate.combinationNote
            if (note != null) {
                assertFalse(
                    "combinationNote must not contain 'safe' but found: $note",
                    note.lowercase().contains("safe"),
                )
            }
        }
    }

    @Test
    fun `DuplicateCandidate combinationNote never contains the word approved`() {
        val medicines =
            listOf(
                medicine("med-001", "Synthamol 500mg", "synthamol"),
                medicine("med-002", "Synthacillin Combo", "synthamol+testocillin"),
            )
        val result = DuplicateDetector.detect(medicines)

        for (candidate in result) {
            val note = candidate.combinationNote
            if (note != null) {
                assertFalse(
                    "combinationNote must not contain 'approved' but found: $note",
                    note.lowercase().contains("approved"),
                )
            }
        }
    }

    @Test
    fun `DuplicateCandidate combinationNote never contains the word interchangeable`() {
        val medicines =
            listOf(
                medicine("med-001", "Synthamol 500mg", "synthamol"),
                medicine("med-002", "Synthacillin Combo", "synthamol+testocillin"),
            )
        val result = DuplicateDetector.detect(medicines)

        for (candidate in result) {
            val note = candidate.combinationNote
            if (note != null) {
                assertFalse(
                    "combinationNote must not contain 'interchangeable' but found: $note",
                    note.lowercase().contains("interchangeable"),
                )
            }
        }
    }

    @Test
    fun `DuplicateCandidate matchType is always EXACT or PARTIAL never a clinical term`() {
        val medicines =
            listOf(
                medicine("med-001", "Synthamol 500mg", "paracetamol"),
                medicine("med-002", "Testamol XR", "paracetamol"),
                medicine("med-003", "Synthacillin Mono", "synthamol"),
                medicine("med-004", "Synthacillin Combo", "synthamol+testocillin"),
            )
        val result = DuplicateDetector.detect(medicines)

        assertTrue("Expected at least one result", result.isNotEmpty())
        for (candidate in result) {
            assertTrue(
                "matchType must be EXACT or PARTIAL, was: ${candidate.matchType}",
                candidate.matchType == MatchType.EXACT || candidate.matchType == MatchType.PARTIAL,
            )
        }
    }

    @Test
    fun `EXACT match has no combinationNote`() {
        val medicines =
            listOf(
                medicine("med-001", "Synthamol 500mg", "paracetamol"),
                medicine("med-002", "Testamol XR", "paracetamol"),
            )
        val result = DuplicateDetector.detect(medicines)

        val exactMatches = result.filter { it.matchType == MatchType.EXACT }
        assertTrue(exactMatches.isNotEmpty())
        for (candidate in exactMatches) {
            assertNull(
                "EXACT match must have null combinationNote, but was: ${candidate.combinationNote}",
                candidate.combinationNote,
            )
        }
    }

    @Test
    fun `PARTIAL match has non-null combinationNote`() {
        val medicines =
            listOf(
                medicine("med-001", "Synthamol 500mg", "synthamol"),
                medicine("med-002", "Synthacillin Combo", "synthamol+testocillin"),
            )
        val result = DuplicateDetector.detect(medicines)

        val partialMatches = result.filter { it.matchType == MatchType.PARTIAL }
        assertTrue(partialMatches.isNotEmpty())
        for (candidate in partialMatches) {
            assertNotNull(
                "PARTIAL match must have a combinationNote",
                candidate.combinationNote,
            )
        }
    }

    @Test
    fun `PARTIAL match confidence is less than EXACT match confidence`() {
        val medicines =
            listOf(
                medicine("med-001", "Synthamol 500mg", "paracetamol"),
                medicine("med-002", "Testamol XR", "paracetamol"),
                medicine("med-003", "Synthacillin Mono", "synthamol"),
                medicine("med-004", "Synthacillin Combo", "synthamol+testocillin"),
            )
        val result = DuplicateDetector.detect(medicines)

        val exactConf = result.first { it.matchType == MatchType.EXACT }.confidence
        val partialConf = result.first { it.matchType == MatchType.PARTIAL }.confidence

        assertTrue(
            "PARTIAL confidence ($partialConf) should be less than EXACT confidence ($exactConf)",
            partialConf < exactConf,
        )
    }

    // --- Boundary: PAST medicine excluded, only CURRENT and UNKNOWN included ---

    @Test
    fun `should detect duplicate between two CURRENT medicines but not PAST`() {
        val medicines =
            listOf(
                medicine("med-001", "Synthamol 500mg", "paracetamol", status = MedicineStatus.CURRENT),
                medicine("med-002", "Testamol XR", "paracetamol", status = MedicineStatus.PAST),
                medicine("med-003", "Acetamol Forte", "paracetamol", status = MedicineStatus.CURRENT),
            )
        val result = DuplicateDetector.detect(medicines)

        assertEquals(1, result.size)
        assertEquals("med-001", result[0].medicineAId)
        assertEquals("med-003", result[0].medicineBId)
    }
}
