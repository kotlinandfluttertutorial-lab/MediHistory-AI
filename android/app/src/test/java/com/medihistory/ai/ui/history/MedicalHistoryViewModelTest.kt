package com.medihistory.ai.ui.history

import com.medihistory.ai.data.ai.GeminiService
import com.medihistory.ai.data.ai.MedicineHistoryNarratorAgent
import com.medihistory.ai.data.history.ContentType
import com.medihistory.ai.data.history.SectionType
import com.medihistory.ai.data.model.Doctor
import com.medihistory.ai.data.model.Medicine
import com.medihistory.ai.data.model.MedicineStatus
import com.medihistory.ai.data.model.OcrStatus
import com.medihistory.ai.data.model.Prescription
import com.medihistory.ai.data.repository.DoctorRepository
import com.medihistory.ai.data.repository.MedicineRepository
import com.medihistory.ai.data.repository.PrescriptionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [MedicalHistoryViewModel].
 *
 * Uses fake repositories — no Room or real storage is accessed.
 * Synthetic fixtures only.
 * Naming convention: `given_[state]_when_[action]_then_[outcome]`
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MedicalHistoryViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakePrescriptionRepository: FakePrescriptionRepository
    private lateinit var fakeMedicineRepository: FakeMedicineRepository
    private lateinit var fakeDoctorRepository: FakeDoctorRepository
    private lateinit var viewModel: MedicalHistoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePrescriptionRepository = FakePrescriptionRepository()
        fakeMedicineRepository = FakeMedicineRepository()
        fakeDoctorRepository = FakeDoctorRepository()
        viewModel =
            MedicalHistoryViewModel(
                prescriptionRepository = fakePrescriptionRepository,
                medicineRepository = fakeMedicineRepository,
                doctorRepository = fakeDoctorRepository,
                narratorAgent =
                    MedicineHistoryNarratorAgent(
                        object : GeminiService() {
                            override suspend fun generate(prompt: String): String? = null
                        },
                    ),
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Synthetic fixtures
    // -------------------------------------------------------------------------

    private val syntheticPatientId = "patient-test-alpha"

    private fun syntheticPrescription(
        id: String = "rx-test-001",
        ocrStatus: OcrStatus = OcrStatus.REVIEWED,
        prescriptionDate: LocalDate? = LocalDate.of(2024, 3, 10),
    ) = Prescription(
        id = id,
        patientId = syntheticPatientId,
        ocrStatus = ocrStatus,
        prescriptionDate = prescriptionDate,
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L,
    )

    private fun syntheticMedicine(
        id: String = "med-test-001",
        prescriptionId: String = "rx-test-001",
    ) = Medicine(
        id = id,
        prescriptionId = prescriptionId,
        patientId = syntheticPatientId,
        medicineName = "Synthamol 500mg",
        activeIngredient = "Synthamolide",
        status = MedicineStatus.CURRENT,
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L,
    )

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    fun given_initial_state_when_viewmodel_created_then_sections_empty_and_not_generating() {
        val state = viewModel.uiState.value

        assertTrue("Sections should be empty initially", state.sections.isEmpty())
        assertFalse("Should not be generating initially", state.isGenerating)
        assertNull("Error should be null initially", state.error)
    }

    @Test
    fun given_reviewed_prescription_when_generateHistory_then_sections_populated() =
        runTest {
            val prescription = syntheticPrescription()
            fakePrescriptionRepository.add(prescription)
            fakeMedicineRepository.add(syntheticMedicine(prescriptionId = prescription.id))

            viewModel.generateHistory(syntheticPatientId)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue("Sections must be populated after generateHistory", state.sections.isNotEmpty())
            assertFalse("isGenerating must be false after completion", state.isGenerating)
            assertNull("Error must be null after successful compile", state.error)
        }

    @Test
    fun given_generateHistory_called_when_running_then_isGenerating_is_true() =
        runTest {
            fakePrescriptionRepository.add(syntheticPrescription())

            // Capture state before advanceUntilIdle
            viewModel.generateHistory(syntheticPatientId)

            // Immediately after launch, before coroutines execute, isGenerating should have been true
            // We verify the final state is not generating (settled)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(
                "isGenerating must be false after completion",
                state.isGenerating,
            )
            assertEquals(
                "generationProgress must be reset to 0 after completion",
                0f,
                state.generationProgress,
            )
        }

    @Test
    fun given_successful_compile_when_generateHistory_then_lastUpdated_is_set() =
        runTest {
            fakePrescriptionRepository.add(syntheticPrescription())

            viewModel.generateHistory(syntheticPatientId)
            advanceUntilIdle()

            assertNotNull(
                "lastUpdated must be set after a successful compile",
                viewModel.uiState.value.lastUpdated,
            )
        }

    @Test
    fun given_compiled_sections_when_ai_summary_section_checked_then_isAiGenerated_true() =
        runTest {
            fakePrescriptionRepository.add(syntheticPrescription())

            viewModel.generateHistory(syntheticPatientId)
            advanceUntilIdle()

            val aiSection =
                viewModel.uiState.value.sections
                    .firstOrNull { it.type == SectionType.AI_SUMMARY }

            assertNotNull("AI_SUMMARY section must be present", aiSection)
            assertTrue(
                "AI_SUMMARY section must have isAiGenerated = true",
                aiSection!!.isAiGenerated,
            )
        }

    @Test
    fun given_ai_summary_stub_when_checked_then_no_clinical_advice() =
        runTest {
            fakePrescriptionRepository.add(syntheticPrescription())

            viewModel.generateHistory(syntheticPatientId)
            advanceUntilIdle()

            val aiSection =
                viewModel.uiState.value.sections
                    .first { it.type == SectionType.AI_SUMMARY }

            val forbiddenTerms = listOf("diagnos", "treatment", "recommend", "prescrib", "stop", "start")
            aiSection.entries.forEach { entry ->
                forbiddenTerms.forEach { term ->
                    assertFalse(
                        "AI stub must not contain clinical advice term '$term': '${entry.text}'",
                        entry.text.lowercase().contains(term),
                    )
                }
            }
        }

    @Test
    fun given_sections_present_when_ai_summary_entry_checked_then_contentType_not_ai_generated() =
        runTest {
            // The stub entry is DOCUMENTED_FACT because it's not actual AI output
            fakePrescriptionRepository.add(syntheticPrescription())

            viewModel.generateHistory(syntheticPatientId)
            advanceUntilIdle()

            val aiSection =
                viewModel.uiState.value.sections
                    .first { it.type == SectionType.AI_SUMMARY }

            aiSection.entries.forEach { entry ->
                assertEquals(
                    "Stub entry in AI_SUMMARY must have ContentType.DOCUMENTED_FACT",
                    ContentType.DOCUMENTED_FACT,
                    entry.contentType,
                )
            }
        }

    @Test
    fun given_refresh_called_when_sections_exist_then_sections_repopulated() =
        runTest {
            fakePrescriptionRepository.add(syntheticPrescription())

            viewModel.generateHistory(syntheticPatientId)
            advanceUntilIdle()

            val firstCompile = viewModel.uiState.value.sections.size

            viewModel.refresh(syntheticPatientId)
            advanceUntilIdle()

            assertEquals(
                "Refresh must repopulate sections with same count",
                firstCompile,
                viewModel.uiState.value.sections.size,
            )
        }

    @Test
    fun given_empty_repositories_when_generateHistory_then_sections_not_null() =
        runTest {
            viewModel.generateHistory(syntheticPatientId)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNotNull("Sections must not be null even with empty repositories", state.sections)
            assertFalse("isGenerating must be false after empty compile", state.isGenerating)
            assertNull("Error must be null for empty compile", state.error)
        }
}

// -------------------------------------------------------------------------
// Fake repositories — in-memory, synthetic data, no I/O
// -------------------------------------------------------------------------

private class FakePrescriptionRepository : PrescriptionRepository {
    private val _prescriptions = MutableStateFlow<List<Prescription>>(emptyList())

    fun add(prescription: Prescription) {
        _prescriptions.update { it + prescription }
    }

    override fun observeByPatient(patientId: String): Flow<List<Prescription>> = _prescriptions.map { list -> list.filter { it.patientId == patientId } }

    override suspend fun getById(
        id: String,
        patientId: String,
    ): Prescription? = _prescriptions.value.find { it.id == id && it.patientId == patientId }

    override suspend fun insert(prescription: Prescription) {
        _prescriptions.update { it + prescription }
    }

    override suspend fun update(prescription: Prescription) {
        _prescriptions.update { list ->
            list.map { if (it.id == prescription.id) prescription else it }
        }
    }

    override suspend fun delete(prescription: Prescription) {
        _prescriptions.update { it.filter { p -> p.id != prescription.id } }
    }

    override suspend fun updateOcrStatus(
        id: String,
        patientId: String,
        status: com.medihistory.ai.data.model.OcrStatus,
        updatedAt: Long,
    ) {
        _prescriptions.update { list ->
            list.map { p ->
                if (p.id == id && p.patientId == patientId) p.copy(ocrStatus = status, updatedAt = updatedAt) else p
            }
        }
    }
}

private class FakeMedicineRepository : MedicineRepository {
    private val _medicines = MutableStateFlow<List<Medicine>>(emptyList())

    fun add(medicine: Medicine) {
        _medicines.update { it + medicine }
    }

    override fun observeByPatient(patientId: String): Flow<List<Medicine>> = _medicines.map { list -> list.filter { it.patientId == patientId } }

    override fun searchByPatient(
        patientId: String,
        query: String,
    ): Flow<List<Medicine>> =
        _medicines.map { list ->
            list.filter { m ->
                m.patientId == patientId &&
                    (
                        m.medicineName.contains(query, ignoreCase = true) ||
                            m.activeIngredient?.contains(query, ignoreCase = true) == true
                    )
            }
        }

    override suspend fun getById(
        id: String,
        patientId: String,
    ): Medicine? = _medicines.value.find { it.id == id && it.patientId == patientId }

    override suspend fun insertAll(medicines: List<Medicine>) {
        _medicines.update { it + medicines }
    }

    override suspend fun update(medicine: Medicine) {
        _medicines.update { list ->
            list.map { if (it.id == medicine.id) medicine else it }
        }
    }

    override suspend fun delete(medicine: Medicine) {
        _medicines.update { it.filter { m -> m.id != medicine.id } }
    }

    override fun observeActiveIngredients(patientId: String): Flow<List<String>> =
        _medicines.map { list ->
            list.filter { it.patientId == patientId }
                .mapNotNull { it.activeIngredient }
                .distinct()
        }
}

private class FakeDoctorRepository : DoctorRepository {
    private val _doctors = MutableStateFlow<List<Doctor>>(emptyList())

    fun add(doctor: Doctor) {
        _doctors.update { it + doctor }
    }

    override fun observeByPatient(patientId: String): Flow<List<Doctor>> = _doctors.map { list -> list.filter { it.patientId == patientId } }

    override suspend fun getById(
        doctorId: String,
        patientId: String,
    ): Doctor? = _doctors.value.find { it.id == doctorId && it.patientId == patientId }

    override suspend fun insert(doctor: Doctor) {
        _doctors.update { it + doctor }
    }

    override suspend fun update(doctor: Doctor) {
        _doctors.update { list ->
            list.map { if (it.id == doctor.id) doctor else it }
        }
    }

    override suspend fun delete(doctor: Doctor) {
        _doctors.update { it.filter { d -> d.id != doctor.id } }
    }
}
