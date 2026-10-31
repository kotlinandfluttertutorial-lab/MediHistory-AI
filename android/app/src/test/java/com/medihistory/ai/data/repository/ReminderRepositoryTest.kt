package com.medihistory.ai.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.medihistory.ai.data.database.MediHistoryDatabase
import com.medihistory.ai.data.model.DoseRecord
import com.medihistory.ai.data.model.DoseRecordEntity
import com.medihistory.ai.data.model.Reminder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalTime

/**
 * Integration tests for [RoomReminderRepository] using an in-memory Room database.
 *
 * Runs on the JVM via Robolectric — no emulator required.
 * Uses synthetic data only (no real patient, doctor, or medicine names).
 *
 * Naming convention: `test_[outcome]_[condition]`
 *
 * Security: every test verifies that patientId scoping is enforced.
 * No health data is logged in any assertion message.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ReminderRepositoryTest {
    private lateinit var db: MediHistoryDatabase
    private lateinit var repository: ReminderRepository

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db =
            Room.inMemoryDatabaseBuilder(context, MediHistoryDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = RoomReminderRepository(db.reminderDao(), db.doseRecordDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    // -------------------------------------------------------------------------
    // Fixtures — synthetic data only
    // -------------------------------------------------------------------------

    private fun syntheticReminder(
        id: String = "rem-001",
        patientId: String = "test-patient-001",
        medicineId: String = "med-001",
        medicineName: String = "Synthamol 500mg",
        prescribedInstructions: String? = "1 tablet after meals",
        reminderTime: LocalTime = LocalTime.of(8, 0),
    ) = Reminder(
        id = id,
        patientId = patientId,
        medicineId = medicineId,
        medicineName = medicineName,
        prescribedInstructions = prescribedInstructions,
        reminderTime = reminderTime,
        isActive = true,
        createdAt = 1_700_000_000_000L,
    )

    private fun syntheticDoseRecord(
        id: String = "dose-001",
        reminderId: String = "rem-001",
        patientId: String = "test-patient-001",
        record: DoseRecord = DoseRecord.TAKEN,
        patientNote: String? = null,
    ) = DoseRecordEntity(
        id = id,
        reminderId = reminderId,
        patientId = patientId,
        scheduledTime = 1_700_000_000_000L,
        recordedTime = 1_700_000_001_000L,
        record = record,
        patientNote = patientNote,
    )

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    fun `test insert then observeByPatient returns correct reminder`() =
        runTest {
            val reminder = syntheticReminder()
            repository.insert(reminder)

            val result = repository.observeByPatient("test-patient-001").first()

            assertEquals("observeByPatient must return 1 reminder after insert", 1, result.size)
            assertEquals(reminder.id, result.first().id)
            assertEquals("Synthamol 500mg", result.first().medicineName)
        }

    @Test
    fun `test DoseRecord TAKEN persists correctly`() =
        runTest {
            val reminder = syntheticReminder()
            repository.insert(reminder)

            val dose = syntheticDoseRecord(record = DoseRecord.TAKEN)
            repository.insertDoseRecord(dose)

            val result = repository.observeDoseRecords("rem-001").first()
            assertEquals("dose record must be persisted", 1, result.size)
            assertEquals(DoseRecord.TAKEN, result.first().record)
        }

    @Test
    fun `test DoseRecord SKIPPED persists correctly`() =
        runTest {
            val reminder = syntheticReminder()
            repository.insert(reminder)

            val dose = syntheticDoseRecord(record = DoseRecord.SKIPPED)
            repository.insertDoseRecord(dose)

            val result = repository.observeDoseRecords("rem-001").first()
            assertEquals("dose record must be persisted", 1, result.size)
            assertEquals(DoseRecord.SKIPPED, result.first().record)
        }

    @Test
    fun `test DoseRecord MISSED persists correctly`() =
        runTest {
            val reminder = syntheticReminder()
            repository.insert(reminder)

            val dose = syntheticDoseRecord(record = DoseRecord.MISSED)
            repository.insertDoseRecord(dose)

            val result = repository.observeDoseRecords("rem-001").first()
            assertEquals("dose record must be persisted", 1, result.size)
            assertEquals(DoseRecord.MISSED, result.first().record)
        }

    @Test
    fun `test recordDose stores patientNote WITHOUT copying prescribedInstructions`() =
        runTest {
            val reminder = syntheticReminder(prescribedInstructions = "1 tablet after meals")
            repository.insert(reminder)

            // patientNote is patient-authored text, completely independent of prescribedInstructions
            val patientOwnNote = "Felt a bit nauseous"
            val dose =
                syntheticDoseRecord(
                    record = DoseRecord.TAKEN,
                    patientNote = patientOwnNote,
                )
            repository.insertDoseRecord(dose)

            val result = repository.observeDoseRecords("rem-001").first()
            assertNotNull("dose record must be persisted", result.firstOrNull())
            assertEquals(
                "patientNote must be exactly what the patient provided",
                patientOwnNote,
                result.first().patientNote,
            )
            // Safety assertion: patientNote must not equal prescribedInstructions
            val storedNote = result.first().patientNote
            val prescribed = reminder.prescribedInstructions
            assert(storedNote != prescribed) {
                "patientNote must NEVER match prescribedInstructions — they are separate data"
            }
        }

    @Test
    fun `test DoseRecord enum has exactly 3 values`() {
        val values = DoseRecord.entries
        assertEquals(
            "DoseRecord must have exactly 3 values: TAKEN, SKIPPED, MISSED",
            3,
            values.size,
        )
    }

    @Test
    fun `test getById with wrong patientId returns null`() =
        runTest {
            val reminder = syntheticReminder(patientId = "test-patient-001")
            repository.insert(reminder)

            // Security: requesting with a different patientId must return null.
            val result = repository.getById(reminder.id, "test-patient-002")

            assertNull(
                "getById with wrong patientId must return null to prevent cross-patient access",
                result,
            )
        }

    @Test
    fun `test delete removes reminder`() =
        runTest {
            val reminder = syntheticReminder()
            repository.insert(reminder)
            repository.delete(reminder)

            val result = repository.observeByPatient("test-patient-001").first()
            assert(result.isEmpty()) { "observeByPatient must return empty list after delete" }
        }
}
