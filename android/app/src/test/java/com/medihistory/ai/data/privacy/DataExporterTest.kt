package com.medihistory.ai.data.privacy

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.medihistory.ai.data.database.MediHistoryDatabase
import com.medihistory.ai.data.model.Medicine
import com.medihistory.ai.data.model.MedicineStatus
import com.medihistory.ai.data.model.OcrStatus
import com.medihistory.ai.data.model.Patient
import com.medihistory.ai.data.model.Prescription
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit/integration tests for [DataExporter] using Robolectric.
 *
 * Verifies that the export function produces a valid JSON file in cacheDir
 * with the expected structure.
 *
 * All test data uses synthetic fixtures only — no real patient names.
 *
 * Naming convention: `test_[outcome]_[condition]`
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DataExporterTest {
    private lateinit var db: MediHistoryDatabase
    private lateinit var context: Context

    private val syntheticPatientId = "test-patient-export-alpha"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db =
            Room.inMemoryDatabaseBuilder(context, MediHistoryDatabase::class.java)
                .allowMainThreadQueries()
                .build()

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
        db.patientDao().insert(
            Patient(
                id = syntheticPatientId,
                name = "Synthetic Test Patient Alpha",
                createdAt = 1_700_000_000_000L,
                updatedAt = 1_700_000_000_000L,
            ),
        )
        db.prescriptionDao().insert(
            Prescription(
                id = "rx-exp-test-001",
                patientId = syntheticPatientId,
                ocrStatus = OcrStatus.REVIEWED,
                createdAt = 1_700_000_001_000L,
                updatedAt = 1_700_000_001_000L,
            ),
        )
        db.medicineDao().insertAll(
            listOf(
                Medicine(
                    id = "med-exp-001",
                    prescriptionId = "rx-exp-test-001",
                    patientId = syntheticPatientId,
                    medicineName = "Synthamol 500mg",
                    activeIngredient = "Synthamolamine",
                    status = MedicineStatus.CURRENT,
                    createdAt = 1_700_000_001_000L,
                    updatedAt = 1_700_000_001_000L,
                ),
            ),
        )
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    fun `test_export_creates_file_in_cacheDir`() =
        runTest {
            val file =
                DataExporter.export(
                    context = context,
                    patientId = syntheticPatientId,
                    database = db,
                )

            assertTrue("Exported file must exist", file.exists())
            assertTrue(
                "Exported file must be in cacheDir",
                file.absolutePath.startsWith(context.cacheDir.absolutePath),
            )
        }

    @Test
    fun `test_exported_file_is_valid_JSON`() =
        runTest {
            val file =
                DataExporter.export(
                    context = context,
                    patientId = syntheticPatientId,
                    database = db,
                )

            val content = file.readText()
            // If the content is not valid JSON, JSONObject constructor throws
            val json = JSONObject(content)
            assertNotNull("Parsed JSON must not be null", json)
        }

    @Test
    fun `test_exported_JSON_contains_patientId_field`() =
        runTest {
            val file =
                DataExporter.export(
                    context = context,
                    patientId = syntheticPatientId,
                    database = db,
                )

            val json = JSONObject(file.readText())
            assertTrue(
                "JSON must contain patientId field",
                json.has("patientId"),
            )
            assertEquals(
                "patientId must match the requested patient",
                syntheticPatientId,
                json.getString("patientId"),
            )
        }

    @Test
    fun `test_exported_JSON_contains_exportedAt_field`() =
        runTest {
            val file =
                DataExporter.export(
                    context = context,
                    patientId = syntheticPatientId,
                    database = db,
                )

            val json = JSONObject(file.readText())
            assertTrue("JSON must contain exportedAt field", json.has("exportedAt"))
            assertTrue(
                "exportedAt must not be blank",
                json.getString("exportedAt").isNotBlank(),
            )
        }

    @Test
    fun `test_file_name_contains_timestamp`() =
        runTest {
            val file =
                DataExporter.export(
                    context = context,
                    patientId = syntheticPatientId,
                    database = db,
                )

            // File name format: mha_export_{patientId}_{timestamp}.json
            // Timestamp is yyyyMMdd_HHmmss — e.g. 20240101_120000
            val fileName = file.name
            assertTrue(
                "File name must start with mha_export_",
                fileName.startsWith("mha_export_"),
            )
            assertTrue("File name must end with .json", fileName.endsWith(".json"))

            // Extract the timestamp portion after stripping prefix and suffix
            val withoutPrefix = fileName.removePrefix("mha_export_${syntheticPatientId}_")
            val timestamp = withoutPrefix.removeSuffix(".json")
            // Timestamp format: yyyyMMdd_HHmmss — should be 15 characters
            assertEquals(
                "Timestamp portion of file name must be 15 characters (yyyyMMdd_HHmmss)",
                15,
                timestamp.length,
            )
            assertTrue(
                "Timestamp must match yyyyMMdd_HHmmss pattern",
                timestamp.matches(Regex("""\d{8}_\d{6}""")),
            )
        }

    @Test
    fun `test_exported_JSON_contains_prescriptions_array`() =
        runTest {
            val file =
                DataExporter.export(
                    context = context,
                    patientId = syntheticPatientId,
                    database = db,
                )

            val json = JSONObject(file.readText())
            assertTrue("JSON must contain prescriptions array", json.has("prescriptions"))
            val prescriptions = json.getJSONArray("prescriptions")
            assertEquals("Prescriptions array must have 1 entry", 1, prescriptions.length())
        }

    @Test
    fun `test_exported_JSON_contains_medicines_array`() =
        runTest {
            val file =
                DataExporter.export(
                    context = context,
                    patientId = syntheticPatientId,
                    database = db,
                )

            val json = JSONObject(file.readText())
            assertTrue("JSON must contain medicines array", json.has("medicines"))
            val medicines = json.getJSONArray("medicines")
            assertEquals("Medicines array must have 1 entry", 1, medicines.length())
        }

    @Test
    fun `test_exported_JSON_does_not_contain_documentPath`() =
        runTest {
            // documentPath is an internal storage path — must never appear in exports
            val file =
                DataExporter.export(
                    context = context,
                    patientId = syntheticPatientId,
                    database = db,
                )

            val json = JSONObject(file.readText())
            val prescriptions = json.getJSONArray("prescriptions")
            for (i in 0 until prescriptions.length()) {
                val prescription = prescriptions.getJSONObject(i)
                assertTrue(
                    "Prescription object must not contain documentPath field",
                    !prescription.has("documentPath"),
                )
            }
        }
}
