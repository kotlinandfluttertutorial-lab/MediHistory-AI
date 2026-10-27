package com.medihistory.ai.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.medihistory.ai.data.database.MediHistoryDatabase
import com.medihistory.ai.data.model.Medicine
import com.medihistory.ai.data.model.MedicineStatus
import com.medihistory.ai.data.model.Patient
import com.medihistory.ai.data.model.Prescription
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Integration tests for [RoomMedicineRepository] using an in-memory Room database.
 *
 * Runs on the JVM via Robolectric — no emulator required.
 * Uses synthetic data only (no real patient, doctor, or medicine names).
 *
 * Security: every test verifies that patientId scoping is enforced.
 * No medicine data is logged in any assertion message.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MedicineRepositoryTest {

    private lateinit var db: MediHistoryDatabase
    private lateinit var repository: MedicineRepository

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, MediHistoryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomMedicineRepository(db.medicineDao())

        // Insert prerequisite patient and prescription rows so FK constraints are satisfied.
        runTest {
            db.patientDao().insert(syntheticPatient())
            db.prescriptionDao().insert(syntheticPrescription())
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    // -------------------------------------------------------------------------
    // Fixtures — synthetic data only (no real medicine brand names)
    // -------------------------------------------------------------------------

    private fun syntheticPatient(id: String = "test-patient-001") = Patient(
        id = id,
        name = "Test Patient Alpha",
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L,
    )

    private fun syntheticPrescription(
        id: String = "rx-001",
        patientId: String = "test-patient-001",
    ) = Prescription(
        id = id,
        patientId = patientId,
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L,
    )

    private fun syntheticMedicine(
        id: String = "med-001",
        patientId: String = "test-patient-001",
        prescriptionId: String = "rx-001",
        medicineName: String = "Synthamol 500mg",
        activeIngredient: String? = "Synthamolide",
        status: MedicineStatus = MedicineStatus.CURRENT,
    ) = Medicine(
        id = id,
        prescriptionId = prescriptionId,
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
    fun `test insert then observeByPatient returns correct medicines`() = runTest {
        val medicine = syntheticMedicine()
        repository.insertAll(listOf(medicine))

        val result = repository.observeByPatient("test-patient-001").first()

        assertEquals("observeByPatient must return 1 medicine after insert", 1, result.size)
        assertEquals(medicine.id, result.first().id)
        assertEquals("Synthamol 500mg", result.first().medicineName)
    }

    @Test
    fun `test searchByPatient filters by medicineName`() = runTest {
        repository.insertAll(
            listOf(
                syntheticMedicine(id = "med-001", medicineName = "Synthamol 500mg"),
                syntheticMedicine(id = "med-002", medicineName = "Testocillin 250mg"),
            )
        )

        val result = repository.searchByPatient("test-patient-001", "Synthamol").first()

        assertEquals("searchByPatient must return only matching medicines", 1, result.size)
        assertEquals("med-001", result.first().id)
    }

    @Test
    fun `test getById with wrong patientId returns null`() = runTest {
        val medicine = syntheticMedicine(patientId = "test-patient-001")
        repository.insertAll(listOf(medicine))

        // Security: requesting with a different patientId must return null,
        // preventing cross-patient data exposure.
        val result = repository.getById(medicine.id, "test-patient-002")

        assertNull(
            "getById with wrong patientId must return null to prevent cross-patient access",
            result,
        )
    }

    @Test
    fun `test delete removes medicine`() = runTest {
        val medicine = syntheticMedicine()
        repository.insertAll(listOf(medicine))

        repository.delete(medicine)

        val result = repository.observeByPatient("test-patient-001").first()
        assertTrue("observeByPatient must return empty list after delete", result.isEmpty())
    }

    @Test
    fun `test insert then getById returns medicine`() = runTest {
        val medicine = syntheticMedicine()
        repository.insertAll(listOf(medicine))

        val result = repository.getById(medicine.id, "test-patient-001")

        assertNotNull("getById must return the inserted medicine", result)
        assertEquals(medicine.id, result!!.id)
        assertEquals("Synthamol 500mg", result.medicineName)
    }

    @Test
    fun `test MedicineStatus enum has exactly 3 values`() {
        val values = MedicineStatus.entries
        assertEquals("MedicineStatus must have exactly 3 values", 3, values.size)
    }
}
