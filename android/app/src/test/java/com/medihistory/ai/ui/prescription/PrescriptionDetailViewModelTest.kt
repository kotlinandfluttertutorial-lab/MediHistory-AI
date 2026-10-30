package com.medihistory.ai.ui.prescription

import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.medihistory.ai.data.model.Doctor
import com.medihistory.ai.data.model.OcrStatus
import com.medihistory.ai.data.model.Prescription
import com.medihistory.ai.data.repository.DoctorRepository
import com.medihistory.ai.data.repository.PrescriptionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 * Unit tests for [PrescriptionDetailViewModel].
 *
 * Uses [FakePrescriptionRepository] and [FakeDoctorRepository] — no Room or real storage
 * is accessed. Uses Robolectric so [WorkManager] can be initialised with
 * [WorkManagerTestInitHelper] (no-op executor, no actual workers run).
 *
 * All fixtures use synthetic data only (no real patient or medicine names).
 *
 * Naming convention: `given_[state]_when_[action]_then_[outcome]`
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PrescriptionDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakePrescriptionRepository
    private lateinit var workManager: WorkManager
    private lateinit var viewModel: PrescriptionDetailViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakePrescriptionRepository()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        workManager = WorkManager.getInstance(context)
        viewModel = PrescriptionDetailViewModel(
            repository = fakeRepository,
            doctorRepository = FakeDetailDoctorRepository(),
            workManager = workManager,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Fixtures — synthetic data only
    // -------------------------------------------------------------------------

    private fun syntheticPrescription(
        id: String = "rx-test-001",
        patientId: String = "test-patient-001",
    ) = Prescription(
        id = id,
        patientId = patientId,
        doctorId = "doc-synthetic-001",
        prescriptionDate = LocalDate.of(2023, 11, 14),
        notes = "Synthetic test prescription",
        documentPath = null,
        documentMimeType = null,
        documentHashSha256 = null,
        ocrStatus = OcrStatus.PENDING,
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L,
    )

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    fun `given viewModel created when observed immediately then isLoading is true`() {
        assertTrue(
            "Initial state must have isLoading = true",
            viewModel.uiState.value.isLoading,
        )
    }

    // -------------------------------------------------------------------------
    // Load — success
    // -------------------------------------------------------------------------

    @Test
    fun `given existing prescription when load called then state has prescription`() = runTest {
        val prescription = syntheticPrescription()
        fakeRepository.add(prescription)

        viewModel.load(prescription.id, prescription.patientId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("isLoading must be false after load", state.isLoading)
        assertNull("error must be null when prescription is found", state.error)
        assertNotNull("prescription must not be null", state.prescription)
        assertEquals("prescription id must match", prescription.id, state.prescription!!.id)
    }

    // -------------------------------------------------------------------------
    // Load — not found
    // -------------------------------------------------------------------------

    @Test
    fun `given unknown prescription id when load called then state has error`() = runTest {
        viewModel.load("rx-unknown-999", "test-patient-001")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("isLoading must be false after failed load", state.isLoading)
        assertNotNull("error must not be null when prescription is not found", state.error)
        assertNull("prescription must be null when not found", state.prescription)
    }

    // -------------------------------------------------------------------------
    // Load — patientId scoping
    // -------------------------------------------------------------------------

    @Test
    fun `given prescription for patient-001 when loaded with patient-002 id then state has error`() =
        runTest {
            val prescription = syntheticPrescription(patientId = "test-patient-001")
            fakeRepository.add(prescription)

            viewModel.load(prescription.id, "test-patient-002")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNotNull(
                "error must not be null when patientId does not match",
                state.error,
            )
            assertNull("prescription must be null for wrong patientId", state.prescription)
        }
}

// ---------------------------------------------------------------------------
// Fake repository — in-memory, synthetic data, no I/O
// ---------------------------------------------------------------------------

private class FakePrescriptionRepository : PrescriptionRepository {

    private val _prescriptions = MutableStateFlow<List<Prescription>>(emptyList())

    fun add(prescription: Prescription) {
        _prescriptions.update { current -> current + prescription }
    }

    override fun observeByPatient(patientId: String): Flow<List<Prescription>> =
        MutableStateFlow(_prescriptions.value.filter { it.patientId == patientId })

    override suspend fun getById(id: String, patientId: String): Prescription? =
        _prescriptions.value.find { it.id == id && it.patientId == patientId }

    override suspend fun insert(prescription: Prescription) {
        _prescriptions.update { current -> current + prescription }
    }

    override suspend fun update(prescription: Prescription) {
        _prescriptions.update { current ->
            current.map { if (it.id == prescription.id) prescription else it }
        }
    }

    override suspend fun delete(prescription: Prescription) {
        _prescriptions.update { current -> current.filter { it.id != prescription.id } }
    }

    override suspend fun updateOcrStatus(
        id: String,
        patientId: String,
        status: OcrStatus,
        updatedAt: Long,
    ) {
        _prescriptions.update { current ->
            current.map { p ->
                if (p.id == id && p.patientId == patientId) {
                    p.copy(ocrStatus = status, updatedAt = updatedAt)
                } else {
                    p
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Fake doctor repository — no-op, always returns null / empty
// ---------------------------------------------------------------------------

private class FakeDetailDoctorRepository : DoctorRepository {

    override fun observeByPatient(patientId: String): Flow<List<Doctor>> =
        MutableStateFlow(emptyList())

    override suspend fun getById(doctorId: String, patientId: String): Doctor? = null

    override suspend fun insert(doctor: Doctor) = Unit

    override suspend fun update(doctor: Doctor) = Unit

    override suspend fun delete(doctor: Doctor) = Unit
}
