package com.medihistory.ai.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.medihistory.ai.data.database.MediHistoryDatabase
import com.medihistory.ai.data.model.Doctor
import com.medihistory.ai.data.model.DoctorSpecialty
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

/**
 * Integration tests for [RoomDoctorRepository] using an in-memory Room database.
 *
 * Runs on the JVM via Robolectric — no emulator required.
 * Uses [MediHistoryDatabase] version 3 (both Patient and Doctor entities registered).
 * All fixtures use synthetic data only (no real patient or doctor names).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DoctorRepositoryTest {

    private lateinit var db: MediHistoryDatabase
    private lateinit var repository: DoctorRepository

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, MediHistoryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomDoctorRepository(db.doctorDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    // -------------------------------------------------------------------------
    // Fixtures — synthetic data only
    // -------------------------------------------------------------------------

    private fun syntheticDoctor(
        id: String = "doc-001",
        patientId: String = "patient-alpha",
        name: String = "Dr. Synthetic",
        specialty: DoctorSpecialty = DoctorSpecialty.CARDIOLOGY,
    ) = Doctor(
        id = id,
        patientId = patientId,
        name = name,
        specialty = specialty,
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L,
    )

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    fun `test insert and getById returns inserted doctor`() = runTest {
        val doctor = syntheticDoctor()
        repository.insert(doctor)

        val result = repository.getById(doctor.id, doctor.patientId)

        assertNotNull("getById must return the inserted doctor", result)
        assertEquals(doctor.id, result!!.id)
        assertEquals(doctor.name, result.name)
        assertEquals(doctor.specialty, result.specialty)
    }

    @Test
    fun `test listByPatient is scoped to patientId`() = runTest {
        val doctorAlpha = syntheticDoctor(id = "doc-001", patientId = "patient-alpha")
        val doctorBeta = syntheticDoctor(
            id = "doc-002",
            patientId = "patient-beta",
            name = "Dr. Synthetic Beta",
        )
        repository.insert(doctorAlpha)
        repository.insert(doctorBeta)

        val alphaList = repository.observeByPatient("patient-alpha").first()
        val betaList = repository.observeByPatient("patient-beta").first()

        assertEquals("patient-alpha must have exactly 1 doctor", 1, alphaList.size)
        assertEquals("patient-beta must have exactly 1 doctor", 1, betaList.size)
        assertEquals(doctorAlpha.id, alphaList.first().id)
        assertEquals(doctorBeta.id, betaList.first().id)
    }

    @Test
    fun `test delete removes doctor from list`() = runTest {
        val doctor = syntheticDoctor()
        repository.insert(doctor)

        repository.delete(doctor)

        val list = repository.observeByPatient(doctor.patientId).first()
        assertEquals("List must be empty after delete", 0, list.size)
    }

    @Test
    fun `test getById with wrong patientId returns null`() = runTest {
        val doctor = syntheticDoctor(patientId = "patient-alpha")
        repository.insert(doctor)

        // Security: requesting with a different patientId must return null,
        // preventing cross-patient data exposure.
        val result = repository.getById(doctor.id, "patient-beta")

        assertNull(
            "getById with wrong patientId must return null to prevent cross-patient access",
            result,
        )
    }

    @Test
    fun `test DoctorSpecialty enum has exactly 7 values`() {
        val values = DoctorSpecialty.entries
        assertEquals(
            "DoctorSpecialty must have exactly 7 values",
            7,
            values.size,
        )
    }
}
