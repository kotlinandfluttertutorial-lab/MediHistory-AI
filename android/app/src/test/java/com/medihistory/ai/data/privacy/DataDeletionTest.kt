package com.medihistory.ai.data.privacy

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.medihistory.ai.data.database.MediHistoryDatabase
import com.medihistory.ai.data.model.Doctor
import com.medihistory.ai.data.model.DoctorSpecialty
import com.medihistory.ai.data.model.Medicine
import com.medihistory.ai.data.model.MedicineStatus
import com.medihistory.ai.data.model.OcrStatus
import com.medihistory.ai.data.model.Patient
import com.medihistory.ai.data.model.Prescription
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Unit/integration tests for [DataDeletion] using Robolectric.
 *
 * Uses in-memory Room and a fake [FakeSecurePreferences] to avoid
 * AndroidKeyStore dependency (which is not available on JVM).
 * All test data uses synthetic fixtures only — no real patient names.
 *
 * Naming convention: `test_[outcome]_[condition]`
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DataDeletionTest {

    private lateinit var db: MediHistoryDatabase
    private lateinit var fakePrefs: FakeSecurePreferences
    private lateinit var context: Context

    private val syntheticPatientId = "test-patient-deletion-alpha"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, MediHistoryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        fakePrefs = FakeSecurePreferences()

        kotlinx.coroutines.runBlocking {
            insertSyntheticData()
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── Fixtures ────────────────────────────────────────────────────────────

    private suspend fun insertSyntheticData() {
        // Patient
        db.patientDao().insert(
            Patient(
                id = syntheticPatientId,
                name = "Synthetic Test Patient Alpha",
                createdAt = 1_700_000_000_000L,
                updatedAt = 1_700_000_000_000L,
            ),
        )

        // Prescriptions
        db.prescriptionDao().insert(
            Prescription(
                id = "rx-del-test-001",
                patientId = syntheticPatientId,
                ocrStatus = OcrStatus.PENDING,
                createdAt = 1_700_000_001_000L,
                updatedAt = 1_700_000_001_000L,
            ),
        )
        db.prescriptionDao().insert(
            Prescription(
                id = "rx-del-test-002",
                patientId = syntheticPatientId,
                ocrStatus = OcrStatus.REVIEWED,
                createdAt = 1_700_000_002_000L,
                updatedAt = 1_700_000_002_000L,
            ),
        )

        // Medicines (linked to prescriptions above via CASCADE)
        db.medicineDao().insertAll(
            listOf(
                Medicine(
                    id = "med-del-001",
                    prescriptionId = "rx-del-test-001",
                    patientId = syntheticPatientId,
                    medicineName = "Synthamol 500mg",
                    status = MedicineStatus.CURRENT,
                    createdAt = 1_700_000_001_000L,
                    updatedAt = 1_700_000_001_000L,
                ),
                Medicine(
                    id = "med-del-002",
                    prescriptionId = "rx-del-test-002",
                    patientId = syntheticPatientId,
                    medicineName = "Testocillin 250mg",
                    status = MedicineStatus.PAST,
                    createdAt = 1_700_000_002_000L,
                    updatedAt = 1_700_000_002_000L,
                ),
            ),
        )

        // Doctor
        db.doctorDao().insert(
            Doctor(
                id = "doc-del-001",
                patientId = syntheticPatientId,
                name = "Dr. Synthetic Test",
                specialty = DoctorSpecialty.GENERAL_MEDICINE,
                createdAt = 1_700_000_000_000L,
                updatedAt = 1_700_000_000_000L,
            ),
        )

        // Store some preferences
        fakePrefs.putString("access_token", "synthetic-test-token-del")
        fakePrefs.putString("patient_id", syntheticPatientId)
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    fun `test_deleteAllData_returns_DeletionResult_with_correct_counts`() = runTest {
        val result = DataDeletion.deleteAllData(
            context = context,
            patientId = syntheticPatientId,
            database = db,
            securePreferences = fakePrefs,
        )

        assertEquals("prescriptionsDeleted must equal 2", 2, result.prescriptionsDeleted)
        assertEquals("medicinesDeleted must equal 2", 2, result.medicinesDeleted)
        assertTrue("preferencesCleared must be true", result.preferencesCleared)
    }

    @Test
    fun `test_deleteAllData_removes_patient_from_database`() = runTest {
        DataDeletion.deleteAllData(
            context = context,
            patientId = syntheticPatientId,
            database = db,
            securePreferences = fakePrefs,
        )

        val patient = db.patientDao().getById(syntheticPatientId)
        assertNull("Patient record must be null after deletion", patient)
    }

    @Test
    fun `test_deleteAllData_removes_prescriptions_from_database`() = runTest {
        DataDeletion.deleteAllData(
            context = context,
            patientId = syntheticPatientId,
            database = db,
            securePreferences = fakePrefs,
        )

        val prescriptions = db.prescriptionDao()
            .observeByPatient(syntheticPatientId)
            .first()
        assertTrue("No prescriptions must remain after deletion", prescriptions.isEmpty())
    }

    @Test
    fun `test_deleteAllData_removes_medicines_via_cascade`() = runTest {
        DataDeletion.deleteAllData(
            context = context,
            patientId = syntheticPatientId,
            database = db,
            securePreferences = fakePrefs,
        )

        val medicines = db.medicineDao()
            .observeByPatient(syntheticPatientId)
            .first()
        assertTrue("No medicines must remain after deletion", medicines.isEmpty())
    }

    @Test
    fun `test_deleteAllData_clears_SecurePreferences`() = runTest {
        DataDeletion.deleteAllData(
            context = context,
            patientId = syntheticPatientId,
            database = db,
            securePreferences = fakePrefs,
        )

        assertTrue("SecurePreferences must be cleared after deletion", fakePrefs.isCleared)
    }

    @Test
    fun `test_DeletionResult_preferencesCleared_is_true`() = runTest {
        val result = DataDeletion.deleteAllData(
            context = context,
            patientId = syntheticPatientId,
            database = db,
            securePreferences = fakePrefs,
        )

        assertTrue(
            "DeletionResult.preferencesCleared must be true",
            result.preferencesCleared,
        )
    }

    @Test
    fun `test_deleteAllData_deletes_files_in_prescriptions_dir`() = runTest {
        // Create a synthetic file in filesDir/prescriptions/
        val prescriptionsDir = File(context.filesDir, "prescriptions")
        prescriptionsDir.mkdirs()
        val syntheticFile = File(prescriptionsDir, "synthetic_prescription_test.jpg")
        syntheticFile.writeText("synthetic content")
        assertTrue("Test file must exist before deletion", syntheticFile.exists())

        val result = DataDeletion.deleteAllData(
            context = context,
            patientId = syntheticPatientId,
            database = db,
            securePreferences = fakePrefs,
        )

        assertEquals("filesDeleted count must be 1", 1, result.filesDeleted)
        assertTrue(
            "Synthetic file must be deleted from prescriptions dir",
            !syntheticFile.exists(),
        )
    }

    @Test
    fun `test_deleteAllData_does_not_affect_other_patient_records`() = runTest {
        // Insert data for a second patient
        val otherPatientId = "test-patient-deletion-beta"
        db.patientDao().insert(
            Patient(
                id = otherPatientId,
                name = "Synthetic Test Patient Beta",
                createdAt = 1_700_000_000_000L,
                updatedAt = 1_700_000_000_000L,
            ),
        )
        db.prescriptionDao().insert(
            Prescription(
                id = "rx-other-001",
                patientId = otherPatientId,
                ocrStatus = OcrStatus.PENDING,
                createdAt = 1_700_000_003_000L,
                updatedAt = 1_700_000_003_000L,
            ),
        )

        DataDeletion.deleteAllData(
            context = context,
            patientId = syntheticPatientId,
            database = db,
            securePreferences = fakePrefs,
        )

        val otherPrescriptions = db.prescriptionDao()
            .observeByPatient(otherPatientId)
            .first()
        assertEquals(
            "Other patient prescriptions must not be deleted",
            1,
            otherPrescriptions.size,
        )
    }
}

// ── Fake SecurePreferences ─────────────────────────────────────────────────────

/**
 * In-memory fake for [SecurePreferencesStore].
 *
 * Avoids the AndroidKeyStore dependency (unavailable in JVM unit tests).
 * Uses plain [HashMap] — not for production use.
 */
private class FakeSecurePreferences : com.medihistory.ai.data.security.SecurePreferencesStore {
    private val store = HashMap<String, String>()
    var isCleared = false
        private set

    override fun putString(key: String, value: String) {
        store[key] = value
    }

    override fun getString(key: String): String? = store[key]

    override fun remove(key: String) {
        store.remove(key)
    }

    override fun clear() {
        store.clear()
        isCleared = true
    }
}
