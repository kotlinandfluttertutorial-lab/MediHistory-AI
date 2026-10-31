package com.medihistory.ai.ui.medicine

import com.medihistory.ai.data.model.Medicine
import com.medihistory.ai.data.model.MedicineStatus
import com.medihistory.ai.data.repository.MedicineRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [MedicineListViewModel].
 *
 * Uses [FakeMedicineRepository] — no Room or real storage is accessed.
 * Synthetic fixtures only (no real medicine brand names or patient names).
 *
 * Naming convention: `should_[outcome]_when_[condition]`
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MedicineListViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeMedicineRepository
    private lateinit var viewModel: MedicineListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeMedicineRepository()
        viewModel = MedicineListViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Fixtures — synthetic data only
    // -------------------------------------------------------------------------

    private fun syntheticMedicine(
        id: String = "med-001",
        patientId: String = "test-patient-001",
        medicineName: String = "Synthamol 500mg",
        activeIngredient: String? = "Synthamolide",
        status: MedicineStatus = MedicineStatus.CURRENT,
    ) = Medicine(
        id = id,
        prescriptionId = "rx-001",
        patientId = patientId,
        medicineName = medicineName,
        activeIngredient = activeIngredient,
        strength = "500mg",
        formulation = "tablet",
        status = status,
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L,
    )

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    fun should_populate_medicines_when_loadMedicines_called() =
        runTest {
            val medicine = syntheticMedicine()
            fakeRepository.add(medicine)

            viewModel.loadMedicines("test-patient-001")
            advanceUntilIdle()

            val medicines = viewModel.uiState.value.medicines
            assertEquals("medicines list must contain the inserted medicine", 1, medicines.size)
            assertEquals(medicine.id, medicines.first().id)
        }

    @Test
    fun should_filter_results_when_onSearchQueryChange_called() =
        runTest {
            fakeRepository.add(syntheticMedicine(id = "med-001", medicineName = "Synthamol 500mg"))
            fakeRepository.add(
                syntheticMedicine(
                    id = "med-002",
                    medicineName = "Testocillin 250mg",
                    activeIngredient = "Testocillinide",
                ),
            )

            viewModel.loadMedicines("test-patient-001")
            advanceUntilIdle()

            viewModel.onSearchQueryChange("Synthamol")
            advanceUntilIdle()

            val medicines = viewModel.uiState.value.medicines
            assertEquals("search must return only matching medicines", 1, medicines.size)
            assertEquals("med-001", medicines.first().id)
        }

    @Test
    fun should_handle_null_activeIngredient_without_crash() =
        runTest {
            // Medicine with null activeIngredient must not cause a crash.
            val medicine = syntheticMedicine(activeIngredient = null)
            fakeRepository.add(medicine)

            viewModel.loadMedicines("test-patient-001")
            advanceUntilIdle()

            val medicines = viewModel.uiState.value.medicines
            assertEquals("medicines list must contain the medicine with null activeIngredient", 1, medicines.size)
            assertNull(
                "activeIngredient must be null in the returned medicine",
                medicines.first().activeIngredient,
            )
        }

    @Test
    fun should_have_exactly_3_values_when_MedicineStatus_values_queried() {
        assertEquals(
            "MedicineStatus must have exactly 3 values: CURRENT, PAST, UNKNOWN",
            3,
            MedicineStatus.entries.size,
        )
    }

    @Test
    fun should_update_searchQuery_in_uiState_when_onSearchQueryChange_called() =
        runTest {
            viewModel.onSearchQueryChange("Testocillin")
            advanceUntilIdle()

            assertEquals(
                "searchQuery in uiState must reflect the change",
                "Testocillin",
                viewModel.uiState.value.searchQuery,
            )
        }

    @Test
    fun should_return_empty_list_when_no_patientId_loaded() =
        runTest {
            // No call to loadMedicines — ViewModel should stay with empty list, no error.
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue("medicines must be empty when no patientId loaded", state.medicines.isEmpty())
            assertNull("error must be null when no patientId loaded", state.error)
        }
}

// ---------------------------------------------------------------------------
// Fake repository — in-memory, synthetic data, no I/O
// ---------------------------------------------------------------------------

private class FakeMedicineRepository : MedicineRepository {
    private val _medicines = MutableStateFlow<List<Medicine>>(emptyList())

    fun add(medicine: Medicine) {
        _medicines.update { current -> current + medicine }
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
