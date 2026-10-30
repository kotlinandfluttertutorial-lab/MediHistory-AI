package com.medihistory.ai.ui.reminders

import com.medihistory.ai.data.model.DoseRecord
import com.medihistory.ai.data.model.DoseRecordEntity
import com.medihistory.ai.data.model.Medicine
import com.medihistory.ai.data.model.MedicineStatus
import com.medihistory.ai.data.model.Reminder
import com.medihistory.ai.data.reminders.ReminderAdvisorEngine
import com.medihistory.ai.data.repository.MedicineRepository
import com.medihistory.ai.data.repository.ReminderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalTime

/**
 * Unit tests for [ReminderViewModel].
 *
 * Uses [FakeReminderRepository] — no Room or real storage is accessed.
 * Synthetic fixtures only (no real patient, doctor, or medicine names).
 *
 * Naming convention: `should_[outcome]_when_[condition]`
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReminderViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeReminderRepository
    private lateinit var fakeMedicineRepository: FakeMedicineRepositoryForReminders
    private lateinit var viewModel: ReminderViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeReminderRepository()
        fakeMedicineRepository = FakeMedicineRepositoryForReminders()
        viewModel = ReminderViewModel(fakeRepository, fakeMedicineRepository, ReminderAdvisorEngine())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Fixtures — synthetic data only
    // -------------------------------------------------------------------------

    private fun syntheticReminder(
        id: String = "rem-001",
        patientId: String = "test-patient-001",
        medicineName: String = "Synthamol 500mg",
        prescribedInstructions: String? = "1 tablet after meals",
    ) = Reminder(
        id = id,
        patientId = patientId,
        medicineId = "med-001",
        medicineName = medicineName,
        prescribedInstructions = prescribedInstructions,
        reminderTime = LocalTime.of(8, 0),
        isActive = true,
        createdAt = 1_700_000_000_000L,
    )

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    fun should_populate_reminders_when_loadReminders_called() = runTest {
        val reminder = syntheticReminder()
        fakeRepository.add(reminder)

        viewModel.loadReminders("test-patient-001")
        advanceUntilIdle()

        val reminders = viewModel.uiState.value.reminders
        assertEquals("reminders list must contain the inserted reminder", 1, reminders.size)
        assertEquals(reminder.id, reminders.first().id)
    }

    @Test
    fun should_add_to_list_when_createReminder_called() = runTest {
        viewModel.loadReminders("test-patient-001")
        advanceUntilIdle()

        viewModel.createReminder(
            patientId = "test-patient-001",
            medicineId = "med-001",
            medicineName = "Synthamol 500mg",
            prescribedInstructions = "1 tablet after meals",
            reminderTime = LocalTime.of(8, 0),
        )
        advanceUntilIdle()

        val reminders = viewModel.uiState.value.reminders
        assertEquals("createReminder must add to the list", 1, reminders.size)
        assertEquals("Synthamol 500mg", reminders.first().medicineName)
    }

    @Test
    fun should_remove_from_list_when_deleteReminder_called() = runTest {
        val reminder = syntheticReminder()
        fakeRepository.add(reminder)

        viewModel.loadReminders("test-patient-001")
        advanceUntilIdle()

        viewModel.deleteReminder(reminder)
        advanceUntilIdle()

        val reminders = viewModel.uiState.value.reminders
        assertTrue("deleteReminder must remove from the list", reminders.isEmpty())
    }

    @Test
    fun should_not_modify_prescribedInstructions_when_recordDose_called() = runTest {
        val prescribed = "1 tablet after meals"
        val reminder = syntheticReminder(prescribedInstructions = prescribed)
        fakeRepository.add(reminder)

        viewModel.loadReminders("test-patient-001")
        advanceUntilIdle()

        // Record a dose — this must NEVER touch prescribedInstructions
        viewModel.recordDose(
            reminderId = reminder.id,
            patientId = "test-patient-001",
            doseRecord = DoseRecord.TAKEN,
            note = "Felt fine",
        )
        advanceUntilIdle()

        // The reminder's prescribedInstructions must be unchanged
        val currentReminder = viewModel.uiState.value.reminders.firstOrNull()
        assertEquals(
            "prescribedInstructions must be unchanged after recordDose",
            prescribed,
            currentReminder?.prescribedInstructions,
        )

        // Safety: the dose record stored by the fake must have a different patientNote
        val doseRecord = fakeRepository.doseRecords.firstOrNull()
        assertNotEquals(
            "patientNote in dose record must NOT match prescribedInstructions",
            prescribed,
            doseRecord?.patientNote,
        )
    }

    @Test
    fun should_have_distinct_values_for_TAKEN_and_MISSED() {
        // DoseRecord.TAKEN and DoseRecord.MISSED must not be equal
        assertNotEquals(
            "DoseRecord.TAKEN and DoseRecord.MISSED must be distinct values",
            DoseRecord.TAKEN,
            DoseRecord.MISSED,
        )
    }

    @Test
    fun should_return_empty_reminders_when_no_loadReminders_called() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("reminders must be empty before loadReminders", state.reminders.isEmpty())
        assertNull("error must be null before loadReminders", state.error)
    }
}

// ---------------------------------------------------------------------------
// Fake repository — in-memory, synthetic data, no I/O
// ---------------------------------------------------------------------------

private class FakeReminderRepository : ReminderRepository {

    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val doseRecords = mutableListOf<DoseRecordEntity>()

    fun add(reminder: Reminder) {
        _reminders.update { current -> current + reminder }
    }

    override fun observeByPatient(patientId: String): Flow<List<Reminder>> =
        _reminders.map { list -> list.filter { it.patientId == patientId } }

    override suspend fun getById(id: String, patientId: String): Reminder? =
        _reminders.value.find { it.id == id && it.patientId == patientId }

    override suspend fun insert(reminder: Reminder) {
        _reminders.update { current -> current + reminder }
    }

    override suspend fun update(reminder: Reminder) {
        _reminders.update { current ->
            current.map { if (it.id == reminder.id) reminder else it }
        }
    }

    override suspend fun delete(reminder: Reminder) {
        _reminders.update { current -> current.filter { it.id != reminder.id } }
    }

    override fun observeDoseRecords(reminderId: String): Flow<List<DoseRecordEntity>> =
        MutableStateFlow(doseRecords.filter { it.reminderId == reminderId })

    override suspend fun insertDoseRecord(doseRecord: DoseRecordEntity) {
        doseRecords.add(doseRecord)
    }

    override suspend fun getDoseRecordsByDateRange(
        patientId: String,
        from: Long,
        to: Long,
    ): List<DoseRecordEntity> =
        doseRecords.filter {
            it.patientId == patientId && it.scheduledTime in from..to
        }
}

// ---------------------------------------------------------------------------
// Fake MedicineRepository — in-memory, no I/O
// ---------------------------------------------------------------------------

private class FakeMedicineRepositoryForReminders : MedicineRepository {
    private val _medicines = MutableStateFlow<List<Medicine>>(emptyList())

    override fun observeByPatient(patientId: String): kotlinx.coroutines.flow.Flow<List<Medicine>> =
        _medicines.map { it.filter { m -> m.patientId == patientId } }

    override fun searchByPatient(patientId: String, query: String): kotlinx.coroutines.flow.Flow<List<Medicine>> =
        _medicines.map { it.filter { m -> m.patientId == patientId } }

    override suspend fun getById(id: String, patientId: String): Medicine? =
        _medicines.value.find { it.id == id && it.patientId == patientId }

    override suspend fun insertAll(medicines: List<Medicine>) {
        _medicines.update { it + medicines }
    }

    override suspend fun update(medicine: Medicine) {
        _medicines.update { list -> list.map { if (it.id == medicine.id) medicine else it } }
    }

    override suspend fun delete(medicine: Medicine) {
        _medicines.update { it.filter { m -> m.id != medicine.id } }
    }

    override fun observeActiveIngredients(patientId: String): kotlinx.coroutines.flow.Flow<List<String>> =
        _medicines.map { list -> list.filter { it.patientId == patientId }.mapNotNull { it.activeIngredient }.distinct() }
}
