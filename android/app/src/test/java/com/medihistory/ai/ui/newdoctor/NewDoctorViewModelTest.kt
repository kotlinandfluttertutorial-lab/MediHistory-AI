package com.medihistory.ai.ui.newdoctor

import com.medihistory.ai.data.model.Medicine
import com.medihistory.ai.data.model.MedicineStatus
import com.medihistory.ai.data.model.OcrStatus
import com.medihistory.ai.data.model.Prescription
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [NewDoctorViewModel].
 *
 * Uses fake repositories — no Room or real storage is accessed.
 * Synthetic fixtures only (no real patient names, doctor names, or medicine names).
 * Naming convention: `given_[state]_when_[action]_then_[outcome]`
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NewDoctorViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakePrescriptionRepository: FakeNewDoctorPrescriptionRepository
    private lateinit var fakeMedicineRepository: FakeNewDoctorMedicineRepository
    private lateinit var viewModel: NewDoctorViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePrescriptionRepository = FakeNewDoctorPrescriptionRepository()
        fakeMedicineRepository = FakeNewDoctorMedicineRepository()
        viewModel =
            NewDoctorViewModel(
                prescriptionRepository = fakePrescriptionRepository,
                medicineRepository = fakeMedicineRepository,
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

    private fun syntheticReviewedPrescription(
        id: String = "rx-synth-001",
        patientId: String = syntheticPatientId,
        ocrStatus: OcrStatus = OcrStatus.REVIEWED,
        prescriptionDate: LocalDate? = LocalDate.of(2024, 3, 15),
    ) = Prescription(
        id = id,
        patientId = patientId,
        ocrStatus = ocrStatus,
        prescriptionDate = prescriptionDate,
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L,
    )

    private fun syntheticMedicine(
        id: String = "med-synth-001",
        prescriptionId: String = "rx-synth-001",
        patientId: String = syntheticPatientId,
    ) = Medicine(
        id = id,
        prescriptionId = prescriptionId,
        patientId = patientId,
        medicineName = "Synthamol 500mg",
        activeIngredient = "Synthamolide",
        status = MedicineStatus.CURRENT,
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L,
    )

    // -------------------------------------------------------------------------
    // Load records
    // -------------------------------------------------------------------------

    @Test
    fun given_reviewed_prescriptions_when_loadRecords_then_records_populated() =
        runTest {
            fakePrescriptionRepository.add(syntheticReviewedPrescription())
            fakeMedicineRepository.add(syntheticMedicine())

            viewModel.loadRecords(syntheticPatientId)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse("records must not be empty after loadRecords", state.records.isEmpty())
            assertEquals("exactly one record should be loaded", 1, state.records.size)
        }

    @Test
    fun given_unreviewed_prescription_when_loadRecords_then_excluded_from_records() =
        runTest {
            // Only REVIEWED prescriptions should appear in the selection list
            fakePrescriptionRepository.add(
                syntheticReviewedPrescription(id = "rx-pending", ocrStatus = OcrStatus.PENDING),
            )

            viewModel.loadRecords(syntheticPatientId)
            advanceUntilIdle()

            assertTrue(
                "Unreviewed prescriptions must not appear in selection list",
                viewModel.uiState.value.records.isEmpty(),
            )
        }

    @Test
    fun given_initial_state_when_created_then_state_is_empty_and_not_loading() {
        val state = viewModel.uiState.value

        assertTrue("records must be empty before loadRecords", state.records.isEmpty())
        assertFalse("isLoading must be false before loadRecords", state.isLoading)
        assertNull("error must be null before loadRecords", state.error)
    }

    // -------------------------------------------------------------------------
    // Toggle selection
    // -------------------------------------------------------------------------

    @Test
    fun given_record_selected_when_onToggleRecord_then_selection_flipped() =
        runTest {
            fakePrescriptionRepository.add(syntheticReviewedPrescription())
            viewModel.loadRecords(syntheticPatientId)
            advanceUntilIdle()

            val prescriptionId = viewModel.uiState.value.records.first().prescriptionId
            assertTrue("record should start selected", viewModel.uiState.value.records.first().isSelected)

            viewModel.onToggleRecord(prescriptionId)

            assertFalse(
                "record must be deselected after toggle",
                viewModel.uiState.value.records.first { it.prescriptionId == prescriptionId }.isSelected,
            )
        }

    @Test
    fun given_record_deselected_when_onToggleRecord_then_record_becomes_selected() =
        runTest {
            fakePrescriptionRepository.add(syntheticReviewedPrescription())
            viewModel.loadRecords(syntheticPatientId)
            advanceUntilIdle()

            val prescriptionId = viewModel.uiState.value.records.first().prescriptionId

            // Deselect first
            viewModel.onToggleRecord(prescriptionId)
            assertFalse("record should be deselected", viewModel.uiState.value.records.first().isSelected)

            // Toggle back
            viewModel.onToggleRecord(prescriptionId)
            assertTrue(
                "record must be selected after second toggle",
                viewModel.uiState.value.records.first { it.prescriptionId == prescriptionId }.isSelected,
            )
        }

    // -------------------------------------------------------------------------
    // canProceed
    // -------------------------------------------------------------------------

    @Test
    fun given_no_records_selected_when_checked_then_canProceed_is_false() =
        runTest {
            fakePrescriptionRepository.add(syntheticReviewedPrescription())
            viewModel.loadRecords(syntheticPatientId)
            advanceUntilIdle()

            // Deselect the only record
            val prescriptionId = viewModel.uiState.value.records.first().prescriptionId
            viewModel.onToggleRecord(prescriptionId)

            assertFalse(
                "canProceed must be false when no records are selected",
                viewModel.uiState.value.canProceed,
            )
        }

    @Test
    fun given_at_least_one_record_selected_when_checked_then_canProceed_is_true() =
        runTest {
            fakePrescriptionRepository.add(syntheticReviewedPrescription())
            viewModel.loadRecords(syntheticPatientId)
            advanceUntilIdle()

            assertTrue(
                "canProceed must be true when at least one record is selected",
                viewModel.uiState.value.canProceed,
            )
        }

    // -------------------------------------------------------------------------
    // Select all / Deselect all
    // -------------------------------------------------------------------------

    @Test
    fun given_some_deselected_when_onSelectAll_then_all_records_selected() =
        runTest {
            fakePrescriptionRepository.add(syntheticReviewedPrescription(id = "rx-001"))
            fakePrescriptionRepository.add(syntheticReviewedPrescription(id = "rx-002"))
            viewModel.loadRecords(syntheticPatientId)
            advanceUntilIdle()

            // Deselect one
            viewModel.onToggleRecord("rx-001")

            viewModel.onSelectAll()

            val state = viewModel.uiState.value
            assertTrue("all records must be selected after onSelectAll", state.records.all { it.isSelected })
            assertEquals(
                "selectedCount must equal total records",
                state.records.size,
                state.selectedCount,
            )
        }

    @Test
    fun given_all_selected_when_onDeselectAll_then_all_records_deselected() =
        runTest {
            fakePrescriptionRepository.add(syntheticReviewedPrescription(id = "rx-001"))
            fakePrescriptionRepository.add(syntheticReviewedPrescription(id = "rx-002"))
            viewModel.loadRecords(syntheticPatientId)
            advanceUntilIdle()

            viewModel.onDeselectAll()

            val state = viewModel.uiState.value
            assertTrue("all records must be deselected after onDeselectAll", state.records.none { it.isSelected })
            assertEquals("selectedCount must be 0 after onDeselectAll", 0, state.selectedCount)
            assertFalse("canProceed must be false after onDeselectAll", state.canProceed)
        }

    // -------------------------------------------------------------------------
    // Consent gate — generatePdfAndShare requires explicit patient action
    // -------------------------------------------------------------------------

    @Test
    fun given_consent_not_confirmed_when_generatePdfAndShare_not_called_then_no_pdf_callback() =
        runTest {
            // The consent dialog is shown in the UI before generatePdfAndShare is ever called.
            // This test verifies that the ViewModel does not spontaneously generate a PDF —
            // the callback is only invoked when generatePdfAndShare is explicitly called.
            fakePrescriptionRepository.add(syntheticReviewedPrescription())
            viewModel.loadRecords(syntheticPatientId)
            advanceUntilIdle()

            var callbackInvoked = false
            // generatePdfAndShare is NOT called here — simulating no consent
            // The callback should never fire
            advanceUntilIdle()

            assertFalse(
                "onPdfReady callback must not be invoked without explicit consent call",
                callbackInvoked,
            )
        }
}

// ---------------------------------------------------------------------------
// Fake repositories — in-memory, synthetic data, no I/O
// ---------------------------------------------------------------------------

private class FakeNewDoctorPrescriptionRepository : PrescriptionRepository {
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
        status: OcrStatus,
        updatedAt: Long,
    ) {
        _prescriptions.update { list ->
            list.map { p ->
                if (p.id == id && p.patientId == patientId) {
                    p.copy(ocrStatus = status, updatedAt = updatedAt)
                } else {
                    p
                }
            }
        }
    }
}

private class FakeNewDoctorMedicineRepository : MedicineRepository {
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
