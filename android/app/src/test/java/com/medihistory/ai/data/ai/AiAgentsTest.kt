package com.medihistory.ai.data.ai

import com.medihistory.ai.data.history.ContentType
import com.medihistory.ai.data.history.SectionType
import com.medihistory.ai.data.model.Doctor
import com.medihistory.ai.data.model.DoctorSpecialty
import com.medihistory.ai.data.model.DuplicateAlert
import com.medihistory.ai.data.model.ExtractedFieldEntity
import com.medihistory.ai.data.model.MatchType
import com.medihistory.ai.data.model.Medicine
import com.medihistory.ai.data.model.MedicineStatus
import com.medihistory.ai.data.model.Prescription
import com.medihistory.ai.data.ocr.ReviewState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AiAgentsTest {
    private class FakeGeminiService(
        var response: String?,
    ) : GeminiService() {
        var calls = 0
        var prompt = ""

        override suspend fun generate(prompt: String): String? {
            calls++
            this.prompt = prompt
            return response
        }
    }

    @Test
    fun prescription_explanation_uses_reviewed_context_and_matches_response() =
        runTest {
            val gemini = FakeGeminiService("Synthamol: Commonly used for general care.\nUnknown: ignored.")
            val medicines =
                listOf(
                    medicine("Synthamol", strength = "500mg", dosage = "once daily"),
                    medicine("OtherMed"),
                )
            val fields =
                listOf(
                    field("doctor_name", "Dr. Synthetic", ReviewState.REVIEWED),
                    field("prescription_date", "2025-07-01", ReviewState.REVIEWED),
                    field("doctor_name", "Unreviewed name", ReviewState.PENDING_REVIEW),
                )

            val results = PrescriptionUnderstandingAgent(gemini).explain(fields, medicines)

            assertEquals(1, gemini.calls)
            assertTrue(gemini.prompt.contains("Synthamol 500mg (once daily)"))
            assertTrue(gemini.prompt.contains("Prescribing doctor: Dr. Synthetic."))
            assertFalse(gemini.prompt.contains("Unreviewed name"))
            assertEquals("Commonly used for general care.", results[0].note)
            assertFalse(results[0].isAiFallback)
            assertNull(results[1].note)
            assertTrue(results[1].isAiFallback)
        }

    @Test
    fun prescription_explanation_falls_back_when_gemini_unavailable_and_skips_empty_list() =
        runTest {
            val gemini = FakeGeminiService(null)
            val agent = PrescriptionUnderstandingAgent(gemini)

            assertTrue(agent.explain(emptyList(), emptyList()).isEmpty())
            assertEquals(0, gemini.calls)
            val fallback = agent.explain(emptyList(), listOf(medicine("Synthamol")))

            assertEquals(1, gemini.calls)
            assertEquals("Synthamol", fallback.single().medicineName)
            assertNull(fallback.single().note)
            assertTrue(fallback.single().isAiFallback)
        }

    @Test
    fun visit_summary_filters_past_medicines_and_parses_ai_questions() =
        runTest {
            val gemini = FakeGeminiService("- Could you explain this medicine?\n- What should I discuss?")
            val current = medicine("Synthamol", status = MedicineStatus.CURRENT)
            val past = medicine("OldMed", status = MedicineStatus.PAST)
            val alert = alert("Synthamol")

            val summary =
                PrepareForVisitAgent(gemini).prepare(
                    medicines = listOf(current, past),
                    openAlerts = listOf(alert),
                    doctorName = "Dr. Synthetic",
                )

            assertEquals(listOf(current), summary.currentMedicines)
            assertEquals(listOf(alert), summary.openAlerts)
            assertEquals(2, summary.suggestedQuestions.size)
            assertFalse(summary.isAiFallback)
            assertTrue(summary.suggestedQuestions.all { !it.isFallback })
            assertTrue(gemini.prompt.contains("You are visiting Dr. Synthetic."))
            assertTrue(gemini.prompt.contains("Potential duplicate: Synthamol"))
        }

    @Test
    fun visit_summary_uses_rule_fallback_and_empty_inputs_do_not_call_gemini() =
        runTest {
            val gemini = FakeGeminiService(null)
            val agent = PrepareForVisitAgent(gemini)
            val empty = agent.prepare(emptyList(), emptyList())
            assertTrue(empty.suggestedQuestions.isEmpty())
            assertTrue(empty.isAiFallback)
            assertEquals(0, gemini.calls)

            val alert = alert("SharedIngredient")
            val summary = agent.prepare(listOf(medicine("Synthamol")), listOf(alert))
            assertTrue(summary.isAiFallback)
            assertEquals(3, summary.suggestedQuestions.size)
            assertTrue(summary.suggestedQuestions.all { it.isFallback })
            assertTrue(summary.suggestedQuestions[0].text.contains("1 of my current medicines"))
            assertTrue(summary.suggestedQuestions[1].text.contains("1 potential duplicate"))
        }

    @Test
    fun narrator_returns_stub_for_empty_or_unavailable_and_ai_entries_reference_latest_record() =
        runTest {
            val rx = Prescription(id = "rx-1", patientId = "patient-1", prescriptionDate = LocalDate.parse("2025-07-01"))
            val gemini = FakeGeminiService("Reviewed history. It is based on records.")
            val agent = MedicineHistoryNarratorAgent(gemini)

            val empty = agent.generateSummary(emptyList(), emptyList(), emptyList())
            assertEquals(SectionType.AI_SUMMARY, empty.type)
            assertEquals(ContentType.DOCUMENTED_FACT, empty.entries.single().contentType)
            assertEquals(0, gemini.calls)

            val summary = agent.generateSummary(listOf(rx), listOf(medicine("Synthamol", prescriptionId = "rx-1")), emptyList())
            assertEquals(1, gemini.calls)
            assertTrue(gemini.prompt.contains("Unknown doctor prescribed Synthamol"))
            assertEquals(2, summary.entries.size)
            assertTrue(summary.entries.all { it.contentType == ContentType.AI_GENERATED })
            assertEquals("rx-1", summary.entries.first().sourceReferences.single().prescriptionId)

            gemini.response = null
            val fallback = agent.generateSummary(listOf(rx), emptyList(), emptyList())
            assertEquals(ContentType.DOCUMENTED_FACT, fallback.entries.single().contentType)
        }

    @Test
    fun narrator_uses_doctor_and_no_medicine_context_for_reviewed_prescription() =
        runTest {
            val rx = Prescription(id = "rx-2", patientId = "patient-1", doctorId = "doc-1")
            val doctor =
                Doctor(
                    id = "doc-1",
                    patientId = "patient-1",
                    name = "Dr. Synthetic",
                    specialty = DoctorSpecialty.OTHER,
                    createdAt = 1L,
                    updatedAt = 1L,
                )
            val gemini = FakeGeminiService(" ")
            val section = MedicineHistoryNarratorAgent(gemini).generateSummary(listOf(rx), emptyList(), listOf(doctor))

            assertTrue(gemini.prompt.contains("Dr. Synthetic prescribed no medicines recorded"))
            assertEquals(ContentType.DOCUMENTED_FACT, section.entries.single().contentType)
        }

    private fun medicine(
        name: String,
        prescriptionId: String = "rx-1",
        strength: String? = null,
        dosage: String? = null,
        status: MedicineStatus = MedicineStatus.UNKNOWN,
    ) = Medicine(
        id = name,
        prescriptionId = prescriptionId,
        patientId = "patient-1",
        medicineName = name,
        strength = strength,
        dosageInstructions = dosage,
        status = status,
        activeIngredient = null,
    )

    private fun field(
        name: String,
        value: String?,
        state: ReviewState,
    ) = ExtractedFieldEntity(
        id = "$name-${state.name}",
        prescriptionId = "rx-1",
        fieldName = name,
        rawValue = "raw",
        patientValue = value,
        confidence = 0.9f,
        reviewState = state,
        sourceReference = null,
    )

    private fun alert(ingredient: String) =
        DuplicateAlert(
            id = "alert-1",
            patientId = "patient-1",
            medicineAId = "med-a",
            medicineBId = "med-b",
            matchingIngredient = ingredient,
            matchType = MatchType.EXACT,
            confidence = 0.95f,
        )
}
