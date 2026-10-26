package com.medihistory.ai.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.medihistory.ai.data.database.MediHistoryDatabase
import com.medihistory.ai.data.model.Patient
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
import java.time.LocalDate

/**
 * Integration tests for [RoomPatientRepository] using an in-memory Room database.
 *
 * Runs on the JVM via Robolectric — no emulator required.
 * All fixtures use synthetic data only (no real patient names or personal data).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PatientRepositoryTest {

    private lateinit var db: MediHistoryDatabase
    private lateinit var repository: PatientRepository

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, MediHistoryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomPatientRepository(db.patientDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    // -------------------------------------------------------------------------
    // Fixtures — synthetic data only
    // -------------------------------------------------------------------------

    private fun syntheticPatient(
        id: String = "patient-alpha",
        name: String = "Test Patient Alpha",
        allergies: List<String> = emptyList(),
    ) = Patient(
        id = id,
        name = name,
        dateOfBirth = LocalDate.of(1990, 1, 1),
        bloodGroup = "O+",
        allergies = allergies,
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L,
    )

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    fun `test insert and getById returns inserted patient`() = runTest {
        val patient = syntheticPatient()
        repository.insert(patient)

        val result = repository.getById(patient.id)

        assertNotNull("getById must return the inserted patient", result)
        assertEquals(patient.id, result!!.id)
        assertEquals(patient.name, result.name)
        assertEquals(patient.bloodGroup, result.bloodGroup)
        assertEquals(patient.dateOfBirth, result.dateOfBirth)
    }

    @Test
    fun `test update persists changes`() = runTest {
        val patient = syntheticPatient()
        repository.insert(patient)

        val updated = patient.copy(name = "Test Patient Alpha Updated", updatedAt = 1_800_000_000_000L)
        repository.update(updated)

        val result = repository.getById(patient.id)
        assertNotNull("getById must return the updated patient", result)
        assertEquals("Test Patient Alpha Updated", result!!.name)
        assertEquals(1_800_000_000_000L, result.updatedAt)
    }

    @Test
    fun `test delete removes patient`() = runTest {
        val patient = syntheticPatient()
        repository.insert(patient)

        repository.delete(patient)

        val result = repository.getById(patient.id)
        assertNull("getById must return null after delete", result)
    }

    @Test
    fun `test getById with wrong patientId returns null`() = runTest {
        val patient = syntheticPatient(id = "patient-alpha")
        repository.insert(patient)

        // Security: a different id must return null — no cross-patient leakage.
        val result = repository.getById("patient-beta")

        assertNull(
            "getById with wrong patientId must return null",
            result,
        )
    }

    @Test
    fun `test allergies round-trip through Room converter`() = runTest {
        val allergies = listOf("Synthamol", "Testocillin", "Dust,Mites")
        val patient = syntheticPatient(allergies = allergies)
        repository.insert(patient)

        val result = repository.getById(patient.id)

        assertNotNull("getById must return the patient", result)
        assertEquals(
            "Allergies list must survive a Room round-trip including entries with commas",
            allergies,
            result!!.allergies.orEmpty(),
        )
    }

    @Test
    fun `test observeAll returns all inserted patients`() = runTest {
        repository.insert(syntheticPatient(id = "patient-alpha", name = "Test Patient Alpha"))
        repository.insert(syntheticPatient(id = "patient-beta", name = "Synthetic User Beta"))

        val list = repository.observeAll().first()

        assertEquals("observeAll must return 2 patients", 2, list.size)
    }
}
