package com.medihistory.ai.ui.prescription

import com.medihistory.ai.data.model.Doctor
import com.medihistory.ai.data.model.DoctorSpecialty
import com.medihistory.ai.data.model.OcrStatus
import com.medihistory.ai.data.model.Prescription
import com.medihistory.ai.data.repository.DoctorRepository
import com.medihistory.ai.data.repository.PrescriptionRepository
import com.medihistory.ai.data.session.SessionManager
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
import java.time.LocalDate

/**
 * Unit tests for [PrescriptionListViewModel].
 *
 * Uses fake in-memory repositories — no Room or I/O is accessed.
 * Synthetic data only: no real patient names, doctor names, or medicine brands.
 * Naming convention: `given_[state]_when_[action]_then_[outcome]`
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrescriptionListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val patientId = "patient-test-alpha"

    private lateinit var fakePrescriptionRepository: FakePrescriptionRepositoryForList
    private lateinit var fakeDoctorRepository: FakeDoctorRepository
    private lateinit var fakeSessionManager: FakeSessionManager
    private lateinit var viewModel: PrescriptionListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePrescriptionRepository = FakePrescriptionRepositoryForList()
        fakeDoctorRepository = FakeDoctorRepository()
        fakeSessionManager = FakeSessionManager(patientId)
        viewModel = PrescriptionListViewModel(
            prescriptionRepository = fakePrescriptionRepository,
            doctorRepository = fakeDoctorRepository,
            sessionManager = fakeSessionManager,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Synthetic fixture helpers
    // -------------------------------------------------------------------------

    private fun syntheticPrescription(
        id: String = "rx-test-001",
        doctorId: String? = null,
        ocrStatus: OcrStatus = OcrStatus.PENDING,
    ) = Prescription(
        id = id,
        patientId = patientId,
        doctorId = doctorId,
        ocrStatus = ocrStatus,
        prescriptionDate = LocalDate.of(2024, 3, 10),
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L,
    )

    private fun syntheticDoctor(
        id: String = "doc-test-001",
        name: String = "Dr. Synthetic Test",
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
    // Initial loading state
    // -------------------------------------------------------------------------

    @Test
    fun `given viewModel created when observed immediately then isLoading is true`() {
        assertTrue(
            "Initial state must have isLoading = true before coroutine runs",
            viewModel.uiState.value.isLoading,
        )
    }

    // -------------------------------------------------------------------------
    // Empty state
    // -------------------------------------------------------------------------

    @Test
    fun `given no prescriptions when coroutine completes then list is empty and isLoading is false`() =
        runTest {
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse("isLoading must be false after load completes", state.isLoading)
            assertTrue(
                "prescriptions must be empty when no records exist",
                state.prescriptions.isEmpty(),
            )
            assertTrue(
                "doctorGroups must be empty when no records exist",
                state.doctorGroups.isEmpty(),
            )
        }

    // -------------------------------------------------------------------------
    // Populated state
    // -------------------------------------------------------------------------

    @Test
    fun `given one prescription when loaded then list has one entry and isLoading is false`() =
        runTest {
            fakePrescriptionRepository.add(syntheticPrescription())
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse("isLoading must be false after load", state.isLoading)
            assertEquals("prescriptions must have exactly 1 entry", 1, state.prescriptions.size)
        }

    @Test
    fun `given prescription with linked doctor when loaded then doctorName uses doctor name`() =
        runTest {
            val doctor = syntheticDoctor()
            fakeDoctorRepository.add(doctor)
            fakePrescriptionRepository.add(syntheticPrescription(doctorId = doctor.id))
            advanceUntilIdle()

            val summary = viewModel.uiState.value.prescriptions.first()
            assertEquals(
                "doctorName must match the linked doctor's name",
                doctor.name,
                summary.doctorName,
            )
        }

    @Test
    fun `given prescription with no doctorId when loaded then doctorName is Doctor removed`() =
        runTest {
            fakePrescriptionRepository.add(syntheticPrescription(doctorId = null))
            advanceUntilIdle()

            val summary = viewModel.uiState.value.prescriptions.first()
            assertEquals(
                "doctorName must be 'Doctor removed' when doctorId is null",
                "Doctor removed",
                summary.doctorName,
            )
        }

    @Test
    fun `given two prescriptions same doctor when loaded then one doctor group with two prescriptions`() =
        runTest {
            val doctor = syntheticDoctor()
            fakeDoctorRepository.add(doctor)
            fakePrescriptionRepository.add(syntheticPrescription(id = "rx-test-001", doctorId = doctor.id))
            fakePrescriptionRepository.add(syntheticPrescription(id = "rx-test-002", doctorId = doctor.id))
            advanceUntilIdle()

            val groups = viewModel.uiState.value.doctorGroups
            assertEquals("Must have exactly one doctor group", 1, groups.size)
            assertEquals("The group must contain both prescriptions", 2, groups.first().prescriptions.size)
        }

    @Test
    fun `given prescription added after initial load when repository emits then list updates`() =
        runTest {
            advanceUntilIdle()

            // Initially empty
            assertTrue(viewModel.uiState.value.prescriptions.isEmpty())

            // Add a prescription after initial load
            fakePrescriptionRepository.add(syntheticPrescription())
            advanceUntilIdle()

            assertEquals(
                "List must update automatically when a new prescription is inserted",
                1,
                viewModel.uiState.value.prescriptions.size,
            )
        }

    // -------------------------------------------------------------------------
    // Toggle group expand/collapse
    // -------------------------------------------------------------------------

    @Test
    fun `given expanded group when onToggleGroup called then group is collapsed`() = runTest {
        val doctor = syntheticDoctor()
        fakeDoctorRepository.add(doctor)
        fakePrescriptionRepository.add(syntheticPrescription(doctorId = doctor.id))
        advanceUntilIdle()

        assertTrue(
            "Group must be expanded by default",
            viewModel.uiState.value.doctorGroups.first().isExpanded,
        )

        viewModel.onToggleGroup(doctor.name)

        assertFalse(
            "Group must be collapsed after toggle",
            viewModel.uiState.value.doctorGroups.first().isExpanded,
        )
    }

    @Test
    fun `given collapsed group when onToggleGroup called again then group is expanded`() = runTest {
        val doctor = syntheticDoctor()
        fakeDoctorRepository.add(doctor)
        fakePrescriptionRepository.add(syntheticPrescription(doctorId = doctor.id))
        advanceUntilIdle()

        viewModel.onToggleGroup(doctor.name)
        viewModel.onToggleGroup(doctor.name)

        assertTrue(
            "Group must be expanded after two toggles",
            viewModel.uiState.value.doctorGroups.first().isExpanded,
        )
    }
}

// -------------------------------------------------------------------------
// Fake implementations — in-memory, synthetic data, no I/O
// -------------------------------------------------------------------------

private class FakePrescriptionRepositoryForList : PrescriptionRepository {

    private val _prescriptions = MutableStateFlow<List<Prescription>>(emptyList())

    fun add(prescription: Prescription) {
        _prescriptions.update { it + prescription }
    }

    override fun observeByPatient(patientId: String): Flow<List<Prescription>> =
        _prescriptions.map { list -> list.filter { it.patientId == patientId } }

    override suspend fun getById(id: String, patientId: String): Prescription? =
        _prescriptions.value.find { it.id == id && it.patientId == patientId }

    override suspend fun insert(prescription: Prescription) {
        _prescriptions.update { it + prescription }
    }

    override suspend fun update(prescription: Prescription) {
        _prescriptions.update { list ->
            list.map { if (it.id == prescription.id) prescription else it }
        }
    }

    override suspend fun delete(prescription: Prescription) {
        _prescriptions.update { it.filter { p -> p.id != prescription.id } }
    }

    override suspend fun updateOcrStatus(
        id: String,
        patientId: String,
        status: OcrStatus,
        updatedAt: Long,
    ) {
        _prescriptions.update { list ->
            list.map { p ->
                if (p.id == id && p.patientId == patientId)
                    p.copy(ocrStatus = status, updatedAt = updatedAt)
                else p
            }
        }
    }
}

private class FakeDoctorRepository : DoctorRepository {

    private val _doctors = MutableStateFlow<List<Doctor>>(emptyList())

    fun add(doctor: Doctor) {
        _doctors.update { it + doctor }
    }

    override fun observeByPatient(patientId: String): Flow<List<Doctor>> =
        _doctors.map { list -> list.filter { it.patientId == patientId } }

    override suspend fun getById(doctorId: String, patientId: String): Doctor? =
        _doctors.value.find { it.id == doctorId && it.patientId == patientId }

    override suspend fun insert(doctor: Doctor) {
        _doctors.update { it + doctor }
    }

    override suspend fun update(doctor: Doctor) {
        _doctors.update { list ->
            list.map { if (it.id == doctor.id) doctor else it }
        }
    }

    override suspend fun delete(doctor: Doctor) {
        _doctors.update { it.filter { d -> d.id != doctor.id } }
    }
}

private class FakeSessionManager(private val patientId: String) : SessionManager(
    securePreferences = object : com.medihistory.ai.data.security.SecurePreferencesStore {
        override fun getString(key: String): String? = null
        override fun putString(key: String, value: String) = Unit
        override fun remove(key: String) = Unit
        override fun clear() = Unit
    }
) {
    override fun getPatientId(): String = patientId
}
