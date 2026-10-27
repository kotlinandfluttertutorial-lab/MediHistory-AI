package com.medihistory.ai.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.medihistory.ai.data.database.MediHistoryDatabase
import com.medihistory.ai.data.model.OcrStatus
import com.medihistory.ai.data.model.Patient
import com.medihistory.ai.data.model.Prescription
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
 * Integration tests for [RoomPrescriptionRepository] using an in-memory Room database.
 *
 * Runs on the JVM via Robolectric — no emulator required.
 * Uses [MediHistoryDatabase] version 4 (Prescription entity added in MA-30).
 * All fixtures use synthetic data only (no real patient, doctor, or medicine names).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PrescriptionRepositoryTest {

    private lateinit var db: MediHistoryDatabase
    private lateinit var repository: PrescriptionRepository

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, MediHistoryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        runBlocking {
            listOf("test-patient-001", "test-patient-002", "test-patient-999").forEach { id ->
                db.patientDao().insert(
                    Patient(
                        id = id,
                        name = "Synthetic Test Patient",
                        createdAt = 1_700_000_000_000L,
                        updatedAt = 1_700_000_000_000L,
                    ),
                )
            }
        }
        repository = RoomPrescriptionRepository(db.prescriptionDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    // -------------------------------------------------------------------------
    // Fixtures — synthetic data only
    // -------------------------------------------------------------------------

    private fun syntheticPrescription(
        id: String = "rx-test-001",
        patientId: String = "test-patient-001",
        doctorId: String? = "doc-synthetic-001",
        ocrStatus: OcrStatus = OcrStatus.PENDING,
        prescriptionDate: LocalDate? = LocalDate.of(2023, 11, 14),
    ) = Prescription(
        id = id,
        patientId = patientId,
        doctorId = doctorId,
        prescriptionDate = prescriptionDate,
        notes = "Synthetic prescription for testing",
        documentPath = null,
        documentMimeType = null,
        documentHashSha256 = null,
        ocrStatus = ocrStatus,
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L,
    )

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    fun `test_insert_and_getById_returns_inserted_prescription`() = runTest {
        val prescription = syntheticPrescription()
        repository.insert(prescription)

        val result = repository.getById(prescription.id, prescription.patientId)

        assertNotNull("getById must return the inserted prescription", result)
        assertEquals(prescription.id, result!!.id)
        assertEquals(prescription.patientId, result.patientId)
        assertEquals(prescription.ocrStatus, result.ocrStatus)
    }

    @Test
    fun `test_getById_scoped_to_patientId`() = runTest {
        val prescription = syntheticPrescription(
            id = "rx-test-001",
            patientId = "test-patient-001",
        )
        repository.insert(prescription)

        // Requesting with a different patientId must return null — no cross-patient access.
        val result = repository.getById("rx-test-001", "test-patient-002")

        assertNull(
            "getById with wrong patientId must return null to prevent cross-patient access",
            result,
        )
    }

    @Test
    fun `test_updateOcrStatus_changes_status_correctly`() = runTest {
        val prescription = syntheticPrescription(ocrStatus = OcrStatus.PENDING)
        repository.insert(prescription)

        repository.updateOcrStatus(
            id = prescription.id,
            patientId = prescription.patientId,
            status = OcrStatus.NEEDS_REVIEW,
            updatedAt = 1_700_000_001_000L,
        )

        val updated = repository.getById(prescription.id, prescription.patientId)
        assertNotNull("Updated prescription must still exist", updated)
        assertEquals(
            "OcrStatus must be updated to NEEDS_REVIEW",
            OcrStatus.NEEDS_REVIEW,
            updated!!.ocrStatus,
        )
    }

    @Test
    fun `test_delete_removes_prescription`() = runTest {
        val prescription = syntheticPrescription()
        repository.insert(prescription)

        repository.delete(prescription)

        val result = repository.getById(prescription.id, prescription.patientId)
        assertNull("Prescription must be null after deletion", result)
    }

    @Test
    fun `test_observeByPatient_scoped_to_patientId`() = runTest {
        val prescriptionAlpha = syntheticPrescription(
            id = "rx-test-001",
            patientId = "test-patient-001",
        )
        val prescriptionBeta = syntheticPrescription(
            id = "rx-test-002",
            patientId = "test-patient-002",
        )
        repository.insert(prescriptionAlpha)
        repository.insert(prescriptionBeta)

        val alphaList = repository.observeByPatient("test-patient-001").first()
        val betaList = repository.observeByPatient("test-patient-002").first()

        assertEquals("test-patient-001 must have exactly 1 prescription", 1, alphaList.size)
        assertEquals("test-patient-002 must have exactly 1 prescription", 1, betaList.size)
        assertEquals(prescriptionAlpha.id, alphaList.first().id)
        assertEquals(prescriptionBeta.id, betaList.first().id)
    }

    @Test
    fun `test_getById_wrong_patientId_returns_null`() = runTest {
        val prescription = syntheticPrescription(patientId = "test-patient-001")
        repository.insert(prescription)

        val result = repository.getById(prescription.id, "test-patient-999")

        assertNull(
            "getById with wrong patientId must return null",
            result,
        )
    }
}
