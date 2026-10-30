package com.medihistory.ai.ui.medicine

import com.medihistory.ai.data.model.Medicine
import com.medihistory.ai.data.model.MedicineStatus
import com.medihistory.ai.data.dao.DuplicateAlertDao
import com.medihistory.ai.data.model.DuplicateAlert
import com.medihistory.ai.data.repository.MedicineRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

/**
 * Unit tests for [MedicineDetailViewModel].
 *
 * Uses [FakeDetailMedicineRepository] — no Room or real storage is accessed.
 * Synthetic fixtures only (no real medicine brand names or patient names).
 *
 * Naming convention: `should_[outcome]_when_[condition]`
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MedicineDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeDetailMedicineRepository
    private lateinit var fakeDuplicateAlertDao: FakeDetailDuplicateAlertDao
    private lateinit var viewModel: MedicineDetailViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeDetailMedicineRepository()
        fakeDuplicateAlertDao = FakeDetailDuplicateAlertDao()
        viewModel = MedicineDetailViewModel(fakeRepository, fakeDuplicateAlertDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Fixtures — synthetic data only
    // -------------------------------------------------------------------------

    private fun syntheticMedicine(
        id: String = "med-detail-001",
        patientId: String = "test-patient-alpha",
        medicineName: String = "Synthamol 500mg",
        activeIngredient: String? = "Synthamolide",
        dosageInstructions: String? = "1 tablet twice daily",
        strength: String? = "500mg",
    ) = Medicine(
        id = id,
        prescriptionId = "rx-detail-001",
        patientId = patientId,
        medicineName = medicineName,
        activeIngredient = activeIngredient,
        strength = strength,
        formulation = "tablet",
        dosageInstructions = dosageInstructions,
        prescribingDoctorId = "dr-synthetic-001",
        status = MedicineStatus.CURRENT,
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L,
    )

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    fun should_have_default_state_when_viewModel_created() {
        val state = viewModel.uiState.value
        assertNull("medicine must be null in default state", state.medicine)
        assertFalse("isLoading must be false in default state", state.isLoading)
        assertNull("error must be null in default state", state.error)
    }

    @Test
    fun should_set_isLoading_true_while_loading() = runTest {
        val medicine = syntheticMedicine()
        // Use a blocking fake that suspends until released so we can inspect the
        // loading state after the coroutine starts but before getById returns.
        val blockingRepo = BlockingFakeDetailMedicineRepository()
        blockingRepo.add(medicine)
        val blockingViewModel =
            MedicineDetailViewModel(blockingRepo, FakeDetailDuplicateAlertDao())

        // Start load — the coroutine is launched but getById is suspended
        val job = launch { blockingViewModel.load(medicineId = medicine.id, patientId = medicine.patientId) }
        advanceUntilIdle() // drive the coroutine to the getById() suspension point

        assertTrue("isLoading must be true while repository call is in progress",
            blockingViewModel.uiState.value.isLoading)

        // Release the suspension so the coroutine finishes
        blockingRepo.release()
        advanceUntilIdle()
        job.join()

        assertFalse("isLoading must be false after load completes",
            blockingViewModel.uiState.value.isLoading)
    }

    @Test
    fun should_set_medicine_when_load_called_with_valid_id() = runTest {
        val medicine = syntheticMedicine()
        fakeRepository.add(medicine)

        viewModel.load(medicineId = medicine.id, patientId = medicine.patientId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull("medicine must be set after successful load", state.medicine)
        assertEquals(medicine.id, state.medicine?.id)
        assertNull("error must be null on success", state.error)
        assertFalse("isLoading must be false after success", state.isLoading)
    }

    @Test
    fun should_set_error_when_load_called_with_unknown_id() = runTest {
        // No medicine is added — repository returns null
        viewModel.load(medicineId = "unknown-id", patientId = "test-patient-alpha")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull("medicine must be null when not found", state.medicine)
        assertNotNull("error must be set when medicine not found", state.error)
        assertFalse("isLoading must be false after error", state.isLoading)
    }

    @Test
    fun should_not_crash_when_activeIngredient_is_null() = runTest {
        val medicine = syntheticMedicine(activeIngredient = null)
        fakeRepository.add(medicine)

        viewModel.load(medicineId = medicine.id, patientId = medicine.patientId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull("medicine must still be set when activeIngredient is null", state.medicine)
        assertNull("activeIngredient must be null in returned medicine", state.medicine?.activeIngredient)
        assertNull("error must be null when load succeeds", state.error)
    }

    @Test
    fun should_not_crash_when_dosageInstructions_is_null() = runTest {
        val medicine = syntheticMedicine(dosageInstructions = null)
        fakeRepository.add(medicine)

        viewModel.load(medicineId = medicine.id, patientId = medicine.patientId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull("medicine must still be set when dosageInstructions is null", state.medicine)
        assertNull("dosageInstructions must be null in returned medicine", state.medicine?.dosageInstructions)
        assertNull("error must be null when load succeeds", state.error)
    }

    @Test
    fun should_set_error_when_cross_patient_access_attempted() = runTest {
        val medicine = syntheticMedicine(patientId = "test-patient-alpha")
        fakeRepository.add(medicine)

        // Query with a different patient ID — repository returns null
        viewModel.load(medicineId = medicine.id, patientId = "different-patient-beta")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull("medicine must be null for cross-patient access", state.medicine)
        assertNotNull("error must be set for cross-patient access", state.error)
    }
}

private class FakeDetailDuplicateAlertDao : DuplicateAlertDao {

    private val alerts = MutableStateFlow<List<DuplicateAlert>>(emptyList())

    override fun observeByPatient(patientId: String): Flow<List<DuplicateAlert>> =
        alerts.map { list -> list.filter { it.patientId == patientId } }

    override fun observeUnreviewedCount(patientId: String): Flow<Int> =
        observeByPatient(patientId).map { list -> list.count { !it.isReviewed } }

    override suspend fun insert(alert: DuplicateAlert) {
        alerts.update { current -> current + alert }
    }

    override suspend fun markReviewed(id: String, patientId: String, timestamp: Long) {
        alerts.update { current ->
            current.map { alert ->
                if (alert.id == id && alert.patientId == patientId) {
                    alert.copy(isReviewed = true, reviewedAt = timestamp)
                } else {
                    alert
                }
            }
        }
    }

    override suspend fun deleteByPatient(patientId: String) {
        alerts.update { current -> current.filterNot { it.patientId == patientId } }
    }

    override suspend fun getById(id: String, patientId: String): DuplicateAlert? =
        alerts.value.find { it.id == id && it.patientId == patientId }

    override suspend fun updateExplanation(id: String, patientId: String, explanation: String) {
        alerts.update { current ->
            current.map { alert ->
                if (alert.id == id && alert.patientId == patientId) {
                    alert.copy(explanation = explanation)
                } else {
                    alert
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Fake repository — in-memory, synthetic data, no I/O
// Named differently from [FakeMedicineRepository] in MedicineListViewModelTest
// to keep test fixtures independent and avoid class name collisions.
// ---------------------------------------------------------------------------

private class FakeDetailMedicineRepository : MedicineRepository {

    private val _medicines = MutableStateFlow<List<Medicine>>(emptyList())

    fun add(medicine: Medicine) {
        _medicines.update { current -> current + medicine }
    }

    override fun observeByPatient(patientId: String): Flow<List<Medicine>> =
        _medicines.map { list -> list.filter { it.patientId == patientId } }

    override fun searchByPatient(patientId: String, query: String): Flow<List<Medicine>> =
        _medicines.map { list ->
            list.filter { m ->
                m.patientId == patientId &&
                    (m.medicineName.contains(query, ignoreCase = true) ||
                        m.activeIngredient?.contains(query, ignoreCase = true) == true)
            }
        }

    override suspend fun getById(id: String, patientId: String): Medicine? =
        _medicines.value.find { it.id == id && it.patientId == patientId }

    override suspend fun insertAll(medicines: List<Medicine>) {
        _medicines.update { current -> current + medicines }
    }

    override suspend fun update(medicine: Medicine) {
        _medicines.update { current ->
            current.map { if (it.id == medicine.id) medicine else it }
        }
    }

    override suspend fun delete(medicine: Medicine) {
        _medicines.update { current -> current.filter { it.id != medicine.id } }
    }

    override fun observeActiveIngredients(patientId: String): Flow<List<String>> =
        _medicines.map { list ->
            list.filter { it.patientId == patientId }
                .mapNotNull { it.activeIngredient }
                .distinct()
        }
}

/**
 * A variant of [FakeDetailMedicineRepository] that suspends [getById] until [release] is called.
 * Used to verify the [MedicineDetailUiState.isLoading] flag while the coroutine is in progress.
 */
private class BlockingFakeDetailMedicineRepository : MedicineRepository {

    private val _medicines = MutableStateFlow<List<Medicine>>(emptyList())
    private val gate = CompletableDeferred<Unit>()

    fun add(medicine: Medicine) {
        _medicines.update { current -> current + medicine }
    }

    /** Unblocks the suspended [getById] call. */
    fun release() {
        gate.complete(Unit)
    }

    override fun observeByPatient(patientId: String): Flow<List<Medicine>> =
        _medicines.map { list -> list.filter { it.patientId == patientId } }

    override fun searchByPatient(patientId: String, query: String): Flow<List<Medicine>> =
        _medicines.map { list ->
            list.filter { m ->
                m.patientId == patientId &&
                    (m.medicineName.contains(query, ignoreCase = true) ||
                        m.activeIngredient?.contains(query, ignoreCase = true) == true)
            }
        }

    override suspend fun getById(id: String, patientId: String): Medicine? {
        gate.await() // suspend here until release() is called
        return _medicines.value.find { it.id == id && it.patientId == patientId }
    }

    override suspend fun insertAll(medicines: List<Medicine>) {
        _medicines.update { current -> current + medicines }
    }

    override suspend fun update(medicine: Medicine) {
        _medicines.update { current ->
            current.map { if (it.id == medicine.id) medicine else it }
        }
    }

    override suspend fun delete(medicine: Medicine) {
        _medicines.update { current -> current.filter { it.id != medicine.id } }
    }

    override fun observeActiveIngredients(patientId: String): Flow<List<String>> =
        _medicines.map { list ->
            list.filter { it.patientId == patientId }
                .mapNotNull { it.activeIngredient }
                .distinct()
        }
}
