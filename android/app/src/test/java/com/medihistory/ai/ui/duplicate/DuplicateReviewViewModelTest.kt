package com.medihistory.ai.ui.duplicate

import com.medihistory.ai.data.dao.DuplicateAlertDao
import com.medihistory.ai.data.model.DuplicateAlert
import com.medihistory.ai.data.model.MatchType
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [DuplicateReviewViewModel].
 *
 * Uses [FakeDuplicateAlertDao] and [FakeMedicineRepository] — no Room or real storage accessed.
 * Synthetic fixtures only (no real medicine brand names or patient names).
 *
 * Naming convention: `given_[state]_when_[action]_then_[outcome]`
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DuplicateReviewViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAlertDao: FakeDuplicateAlertDao
    private lateinit var fakeMedicineRepo: FakeMedicineRepository
    private lateinit var viewModel: DuplicateReviewViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAlertDao = FakeDuplicateAlertDao()
        fakeMedicineRepo = FakeMedicineRepository()
        viewModel = DuplicateReviewViewModel(fakeAlertDao, fakeMedicineRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Fixtures — synthetic data only (no real brand names or patient data)
    // -------------------------------------------------------------------------

    private fun syntheticAlert(
        id: String = "alert-001",
        patientId: String = "test-patient-alpha",
        medicineAId: String = "med-001",
        medicineBId: String = "med-002",
        matchingIngredient: String = "synthamolide",
        matchType: MatchType = MatchType.EXACT,
        isReviewed: Boolean = false,
    ) = DuplicateAlert(
        id = id,
        patientId = patientId,
        medicineAId = medicineAId,
        medicineBId = medicineBId,
        matchingIngredient = matchingIngredient,
        matchType = matchType,
        confidence = 0.95f,
        isReviewed = isReviewed,
        combinationNote = null,
        createdAt = 1_700_000_000_000L,
    )

    private fun syntheticMedicine(
        id: String,
        patientId: String = "test-patient-alpha",
        medicineName: String,
    ) = Medicine(
        id = id,
        prescriptionId = "rx-001",
        patientId = patientId,
        medicineName = medicineName,
        status = MedicineStatus.CURRENT,
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L,
    )

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    fun given_alerts_in_dao_when_loadAlerts_called_then_alerts_list_is_populated() = runTest {
        val alert = syntheticAlert()
        fakeAlertDao.add(alert)
        fakeMedicineRepo.add(syntheticMedicine("med-001", medicineName = "Synthamol 500mg"))
        fakeMedicineRepo.add(syntheticMedicine("med-002", medicineName = "Testocillin 250mg"))

        viewModel.loadAlerts("test-patient-alpha")
        advanceUntilIdle()

        val alerts = viewModel.uiState.value.alerts
        assertEquals("alerts list must contain the inserted alert", 1, alerts.size)
        assertEquals(alert.id, alerts.first().alert.id)
        assertEquals("Synthamol 500mg", alerts.first().medicineAName)
        assertEquals("Testocillin 250mg", alerts.first().medicineBName)
    }

    @Test
    fun given_unreviewed_alert_when_onMarkReviewed_called_then_isReviewed_becomes_true() = runTest {
        val alert = syntheticAlert(isReviewed = false)
        fakeAlertDao.add(alert)
        fakeMedicineRepo.add(syntheticMedicine("med-001", medicineName = "Synthamol 500mg"))
        fakeMedicineRepo.add(syntheticMedicine("med-002", medicineName = "Testocillin 250mg"))

        viewModel.loadAlerts("test-patient-alpha")
        advanceUntilIdle()

        viewModel.onMarkReviewed(alert.id)
        advanceUntilIdle()

        val updatedAlert = viewModel.uiState.value.alerts.firstOrNull { it.alert.id == alert.id }
        assertTrue(
            "alert must be reviewed after onMarkReviewed",
            updatedAlert?.alert?.isReviewed == true,
        )
    }

    @Test
    fun given_collapsed_alert_when_onToggleExpanded_called_then_isExpanded_becomes_true() = runTest {
        val alert = syntheticAlert()
        fakeAlertDao.add(alert)
        fakeMedicineRepo.add(syntheticMedicine("med-001", medicineName = "Synthamol 500mg"))
        fakeMedicineRepo.add(syntheticMedicine("med-002", medicineName = "Testocillin 250mg"))

        viewModel.loadAlerts("test-patient-alpha")
        advanceUntilIdle()

        assertFalse(
            "alert must start collapsed",
            viewModel.uiState.value.alerts.first().isExpanded,
        )

        viewModel.onToggleExpanded(alert.id)
        advanceUntilIdle()

        assertTrue(
            "alert must be expanded after toggle",
            viewModel.uiState.value.alerts.first().isExpanded,
        )
    }

    @Test
    fun given_expanded_alert_when_onToggleExpanded_called_again_then_isExpanded_becomes_false() = runTest {
        val alert = syntheticAlert()
        fakeAlertDao.add(alert)
        fakeMedicineRepo.add(syntheticMedicine("med-001", medicineName = "Synthamol 500mg"))
        fakeMedicineRepo.add(syntheticMedicine("med-002", medicineName = "Testocillin 250mg"))

        viewModel.loadAlerts("test-patient-alpha")
        advanceUntilIdle()

        viewModel.onToggleExpanded(alert.id)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.alerts.first().isExpanded)

        viewModel.onToggleExpanded(alert.id)
        advanceUntilIdle()

        assertFalse(
            "alert must collapse on second toggle",
            viewModel.uiState.value.alerts.first().isExpanded,
        )
    }

    @Test
    fun given_reviewed_alert_when_onMarkReviewed_called_then_unreviewedCount_decreases() = runTest {
        val alertA = syntheticAlert(id = "alert-001", isReviewed = false)
        val alertB = syntheticAlert(
            id = "alert-002",
            medicineAId = "med-003",
            medicineBId = "med-004",
            isReviewed = false,
        )
        fakeAlertDao.add(alertA)
        fakeAlertDao.add(alertB)
        fakeMedicineRepo.add(syntheticMedicine("med-001", medicineName = "Synthamol 500mg"))
        fakeMedicineRepo.add(syntheticMedicine("med-002", medicineName = "Testocillin 250mg"))
        fakeMedicineRepo.add(syntheticMedicine("med-003", medicineName = "Syntheprofen 200mg"))
        fakeMedicineRepo.add(syntheticMedicine("med-004", medicineName = "Fakecillin 100mg"))

        viewModel.loadAlerts("test-patient-alpha")
        advanceUntilIdle()

        assertEquals("unreviewedCount must be 2 before marking", 2, viewModel.uiState.value.unreviewedCount)

        viewModel.onMarkReviewed(alertA.id)
        advanceUntilIdle()

        assertEquals(
            "unreviewedCount must decrease to 1 after marking one reviewed",
            1,
            viewModel.uiState.value.unreviewedCount,
        )
    }

    @Test
    fun given_reviewed_alert_when_showReviewed_false_then_reviewed_alerts_are_filtered() = runTest {
        val unreviewedAlert = syntheticAlert(id = "alert-001", isReviewed = false)
        val reviewedAlert = syntheticAlert(
            id = "alert-002",
            medicineAId = "med-003",
            medicineBId = "med-004",
            isReviewed = true,
        )
        fakeAlertDao.add(unreviewedAlert)
        fakeAlertDao.add(reviewedAlert)
        fakeMedicineRepo.add(syntheticMedicine("med-001", medicineName = "Synthamol 500mg"))
        fakeMedicineRepo.add(syntheticMedicine("med-002", medicineName = "Testocillin 250mg"))
        fakeMedicineRepo.add(syntheticMedicine("med-003", medicineName = "Syntheprofen 200mg"))
        fakeMedicineRepo.add(syntheticMedicine("med-004", medicineName = "Fakecillin 100mg"))

        viewModel.loadAlerts("test-patient-alpha")
        advanceUntilIdle()

        assertEquals("both alerts must show by default", 2, viewModel.uiState.value.alerts.size)

        viewModel.onToggleShowReviewed() // now showReviewedAlerts = false
        advanceUntilIdle()

        val filtered = viewModel.uiState.value.alerts
        assertEquals("only unreviewed alerts must show when filter is off", 1, filtered.size)
        assertEquals(unreviewedAlert.id, filtered.first().alert.id)
    }
}

// ---------------------------------------------------------------------------
// Fake DAO — in-memory, synthetic data, no Room I/O
// ---------------------------------------------------------------------------

private class FakeDuplicateAlertDao : DuplicateAlertDao {

    private val _alerts = MutableStateFlow<List<DuplicateAlert>>(emptyList())

    fun add(alert: DuplicateAlert) {
        _alerts.update { current -> current + alert }
    }

    override fun observeByPatient(patientId: String): Flow<List<DuplicateAlert>> =
        _alerts.map { list -> list.filter { it.patientId == patientId } }

    override fun observeUnreviewedCount(patientId: String): Flow<Int> =
        observeByPatient(patientId).map { alerts -> alerts.count { !it.isReviewed } }

    override suspend fun insert(alert: DuplicateAlert) {
        _alerts.update { current ->
            if (current.any { it.id == alert.id }) current
            else current + alert
        }
    }

    override suspend fun markReviewed(id: String, patientId: String, timestamp: Long) {
        _alerts.update { current ->
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
        _alerts.update { current -> current.filter { it.patientId != patientId } }
    }

    override suspend fun getById(id: String, patientId: String): DuplicateAlert? =
        _alerts.value.find { it.id == id && it.patientId == patientId }
}

// ---------------------------------------------------------------------------
// Fake MedicineRepository — in-memory, synthetic data, no Room I/O
// ---------------------------------------------------------------------------

private class FakeMedicineRepository : MedicineRepository {

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
