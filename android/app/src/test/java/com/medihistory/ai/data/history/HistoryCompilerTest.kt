package com.medihistory.ai.data.history

import com.medihistory.ai.data.model.Doctor
import com.medihistory.ai.data.model.DoctorSpecialty
import com.medihistory.ai.data.model.Medicine
import com.medihistory.ai.data.model.MedicineStatus
import com.medihistory.ai.data.model.OcrStatus
import com.medihistory.ai.data.model.Prescription
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [HistoryCompiler].
 *
 * Synthetic data only — no real patient names, doctor names, or medicine brand names.
 * Naming convention: `given_[state]_when_[action]_then_[outcome]`
 */
class HistoryCompilerTest {
    // -------------------------------------------------------------------------
    // Synthetic fixtures
    // -------------------------------------------------------------------------

    private fun syntheticPrescription(
        id: String = "rx-test-001",
        patientId: String = "patient-alpha",
        ocrStatus: OcrStatus = OcrStatus.REVIEWED,
        prescriptionDate: LocalDate? = LocalDate.of(2024, 1, 15),
        doctorId: String? = null,
    ) = Prescription(
        id = id,
        patientId = patientId,
        ocrStatus = ocrStatus,
        prescriptionDate = prescriptionDate,
        doctorId = doctorId,
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L,
    )

    private fun syntheticMedicine(
        id: String = "med-test-001",
        prescriptionId: String = "rx-test-001",
        patientId: String = "patient-alpha",
        medicineName: String = "Synthamol 500mg",
        activeIngredient: String? = "Synthamolide",
        status: MedicineStatus = MedicineStatus.CURRENT,
    ) = Medicine(
        id = id,
        prescriptionId = prescriptionId,
        patientId = patientId,
        medicineName = medicineName,
        activeIngredient = activeIngredient,
        status = status,
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L,
    )

    private fun syntheticDoctor(
        id: String = "dr-test-001",
        patientId: String = "patient-alpha",
        prescriptionId: String = "rx-test-001",
    ) = Doctor(
        id = id,
        patientId = patientId,
        name = "Dr. Synthetic",
        specialty = DoctorSpecialty.GENERAL_MEDICINE,
        clinicName = "Synthetic Clinic",
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L,
    )

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    fun given_empty_lists_when_compile_then_sections_have_no_entries() {
        val sections =
            HistoryCompiler.compile(
                prescriptions = emptyList(),
                medicines = emptyList(),
                doctors = emptyList(),
            )

        assertTrue("Should return sections even for empty input", sections.isNotEmpty())
        sections.filter { it.type != SectionType.AI_SUMMARY }.forEach { section ->
            assertTrue(
                "Section '${section.title}' should have no entries for empty input",
                section.entries.isEmpty(),
            )
        }
    }

    @Test
    fun given_mixed_ocr_statuses_when_compile_then_only_reviewed_prescriptions_used() {
        val reviewedPrescription =
            syntheticPrescription(
                id = "rx-reviewed",
                ocrStatus = OcrStatus.REVIEWED,
                prescriptionDate = LocalDate.of(2024, 2, 1),
            )
        val pendingPrescription =
            syntheticPrescription(
                id = "rx-pending",
                ocrStatus = OcrStatus.PENDING,
                prescriptionDate = LocalDate.of(2024, 3, 1),
            )
        val needsReviewPrescription =
            syntheticPrescription(
                id = "rx-needs-review",
                ocrStatus = OcrStatus.NEEDS_REVIEW,
                prescriptionDate = LocalDate.of(2024, 4, 1),
            )

        // Only the reviewed prescription should appear in the timeline
        val reviewedMedicine = syntheticMedicine(prescriptionId = "rx-reviewed")
        val pendingMedicine =
            syntheticMedicine(
                id = "med-pending",
                prescriptionId = "rx-pending",
                medicineName = "Testocillin 250mg",
            )

        val sections =
            HistoryCompiler.compile(
                prescriptions = listOf(reviewedPrescription, pendingPrescription, needsReviewPrescription),
                medicines = listOf(reviewedMedicine, pendingMedicine),
                doctors = emptyList(),
            )

        val timelineSection = sections.first { it.type == SectionType.TIMELINE }
        assertTrue(
            "Timeline should only contain reviewed prescriptions",
            timelineSection.entries.all { entry ->
                entry.sourceReferences.all { it.prescriptionId == "rx-reviewed" }
            },
        )

        val medicinesSection = sections.first { it.type == SectionType.MEDICINES }
        assertTrue(
            "Medicines section should only include medicines from reviewed prescriptions",
            medicinesSection.entries.all { entry ->
                entry.sourceReferences.all { it.prescriptionId == "rx-reviewed" }
            },
        )
    }

    @Test
    fun given_reviewed_prescription_with_medicine_when_compile_then_every_entry_has_source_reference() {
        val prescription = syntheticPrescription()
        val medicine = syntheticMedicine(prescriptionId = prescription.id)

        val sections =
            HistoryCompiler.compile(
                prescriptions = listOf(prescription),
                medicines = listOf(medicine),
                doctors = emptyList(),
            )

        sections.filter { it.type != SectionType.AI_SUMMARY }.forEach { section ->
            section.entries.forEach { entry ->
                assertTrue(
                    "Every entry in section '${section.title}' must have at least one source reference",
                    entry.sourceReferences.isNotEmpty(),
                )
            }
        }
    }

    @Test
    fun given_past_medicine_when_compile_then_excluded_from_active_medicines_section() {
        val prescription = syntheticPrescription()
        val pastMedicine =
            syntheticMedicine(
                id = "med-past",
                medicineName = "Synthamol 500mg",
                status = MedicineStatus.PAST,
                prescriptionId = prescription.id,
            )
        val currentMedicine =
            syntheticMedicine(
                id = "med-current",
                medicineName = "Testocillin 250mg",
                status = MedicineStatus.CURRENT,
                prescriptionId = prescription.id,
            )

        val sections =
            HistoryCompiler.compile(
                prescriptions = listOf(prescription),
                medicines = listOf(pastMedicine, currentMedicine),
                doctors = emptyList(),
            )

        val medicinesSection = sections.first { it.type == SectionType.MEDICINES }
        val medicineTexts = medicinesSection.entries.map { it.text }

        assertFalse(
            "PAST medicine must be excluded from active medicines section",
            medicineTexts.any { it.contains("Synthamol 500mg") },
        )
        assertTrue(
            "CURRENT medicine must be included in active medicines section",
            medicineTexts.any { it.contains("Testocillin 250mg") },
        )
    }

    @Test
    fun given_entries_when_compile_then_no_clinical_terms_in_text() {
        val prescription = syntheticPrescription()
        val medicine = syntheticMedicine(prescriptionId = prescription.id)

        val sections =
            HistoryCompiler.compile(
                prescriptions = listOf(prescription),
                medicines = listOf(medicine),
                doctors = emptyList(),
            )

        val forbiddenTerms = listOf("diagnosis", "treatment", "recommend")

        sections.filter { it.type != SectionType.AI_SUMMARY }.forEach { section ->
            section.entries.forEach { entry ->
                forbiddenTerms.forEach { term ->
                    assertFalse(
                        "Entry text must not contain clinical term '$term': '${entry.text}'",
                        entry.text.lowercase().contains(term),
                    )
                }
            }
        }
    }

    @Test
    fun given_same_input_when_compile_called_twice_then_results_are_identical() {
        val prescription = syntheticPrescription()
        val medicine1 = syntheticMedicine(id = "med-001", prescriptionId = prescription.id)
        val medicine2 =
            syntheticMedicine(
                id = "med-002",
                prescriptionId = prescription.id,
                medicineName = "Testocillin 250mg",
            )
        val doctor = syntheticDoctor()
        val prescriptionWithDoctor = syntheticPrescription(doctorId = doctor.id)

        val firstResult =
            HistoryCompiler.compile(
                prescriptions = listOf(prescriptionWithDoctor),
                medicines = listOf(medicine1, medicine2),
                doctors = listOf(doctor),
            )
        val secondResult =
            HistoryCompiler.compile(
                prescriptions = listOf(prescriptionWithDoctor),
                medicines = listOf(medicine1, medicine2),
                doctors = listOf(doctor),
            )

        assertEquals(
            "HistoryCompiler must be deterministic — same input must produce same output",
            firstResult,
            secondResult,
        )
    }

    @Test
    fun given_ai_summary_section_when_compile_then_isAiGenerated_is_true() {
        val sections =
            HistoryCompiler.compile(
                prescriptions = emptyList(),
                medicines = emptyList(),
                doctors = emptyList(),
            )

        val aiSection = sections.first { it.type == SectionType.AI_SUMMARY }
        assertTrue(
            "AI_SUMMARY section must have isAiGenerated = true",
            aiSection.isAiGenerated,
        )
    }

    @Test
    fun given_deterministic_sections_when_compile_then_isAiGenerated_is_false() {
        val sections =
            HistoryCompiler.compile(
                prescriptions = emptyList(),
                medicines = emptyList(),
                doctors = emptyList(),
            )

        sections.filter { it.type != SectionType.AI_SUMMARY }.forEach { section ->
            assertFalse(
                "Section '${section.title}' must not have isAiGenerated = true",
                section.isAiGenerated,
            )
        }
    }

    @Test
    fun given_prescription_without_date_when_compile_then_excluded_from_timeline() {
        val prescriptionNoDate =
            syntheticPrescription(
                id = "rx-no-date",
                prescriptionDate = null,
            )
        val prescriptionWithDate =
            syntheticPrescription(
                id = "rx-with-date",
                prescriptionDate = LocalDate.of(2024, 5, 10),
            )

        val sections =
            HistoryCompiler.compile(
                prescriptions = listOf(prescriptionNoDate, prescriptionWithDate),
                medicines = emptyList(),
                doctors = emptyList(),
            )

        val timelineSection = sections.first { it.type == SectionType.TIMELINE }
        assertEquals(
            "Only prescriptions with a date should appear in timeline",
            1,
            timelineSection.entries.size,
        )
        assertEquals(
            "Timeline entry should reference the prescription with a date",
            "rx-with-date",
            timelineSection.entries.first().sourceReferences.first().prescriptionId,
        )
    }
}
