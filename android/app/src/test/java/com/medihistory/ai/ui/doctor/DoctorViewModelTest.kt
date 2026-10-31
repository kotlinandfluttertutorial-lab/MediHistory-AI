package com.medihistory.ai.ui.doctor

import com.medihistory.ai.data.model.Doctor
import com.medihistory.ai.data.model.DoctorSpecialty
import com.medihistory.ai.data.repository.DoctorRepository
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

/**
 * Unit tests for [DoctorViewModel].
 *
 * Uses [FakeDoctorRepository] — no Room or real storage is accessed.
 * Synthetic fixtures only (no real doctor or patient names).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DoctorViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeDoctorRepository
    private lateinit var viewModel: DoctorViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeDoctorRepository()
        viewModel = DoctorViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Fixtures — synthetic data only
    // -------------------------------------------------------------------------

    private fun syntheticDoctor(
        id: String = "doc-001",
        patientId: String = "patient-alpha",
    ) = Doctor(
        id = id,
        patientId = patientId,
        name = "Dr. Synthetic",
        specialty = DoctorSpecialty.CARDIOLOGY,
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L,
    )

    // -------------------------------------------------------------------------
    // Load
    // -------------------------------------------------------------------------

    @Test
    fun `should populate doctors when loadDoctors called`() =
        runTest {
            val doctor = syntheticDoctor()
            fakeRepository.addDoctor(doctor)

            viewModel.loadDoctors("patient-alpha")
            advanceUntilIdle()

            val doctors = viewModel.uiState.value.doctors
            assertEquals("doctors list must contain the inserted doctor", 1, doctors.size)
            assertEquals(doctor.id, doctors.first().id)
        }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    @Test
    fun `should set error when name is empty on save`() =
        runTest {
            // name is blank by default
            viewModel.onSaveClicked("patient-alpha")
            advanceUntilIdle()

            val error = viewModel.uiState.value.error
            assertNotNull("error must not be null when name is blank", error)
            assertFalse("isSaved must be false when name is blank", viewModel.uiState.value.isSaved)
        }

    @Test
    fun `should set error when specialty is OTHER and specialtyOther is empty`() =
        runTest {
            viewModel.onNameChange("Dr. Synthetic")
            viewModel.onSpecialtyChange(DoctorSpecialty.OTHER)
            // specialtyOther left blank
            viewModel.onSaveClicked("patient-alpha")
            advanceUntilIdle()

            val error = viewModel.uiState.value.error
            assertNotNull("error must not be null when specialtyOther is blank for OTHER", error)
            assertFalse(
                "isSaved must be false when specialtyOther is blank",
                viewModel.uiState.value.isSaved,
            )
        }

    // -------------------------------------------------------------------------
    // Save
    // -------------------------------------------------------------------------

    @Test
    fun `should set isSaved true when valid save completes`() =
        runTest {
            viewModel.onNameChange("Dr. Synthetic")
            viewModel.onSpecialtyChange(DoctorSpecialty.CARDIOLOGY)
            viewModel.onSaveClicked("patient-alpha")
            advanceUntilIdle()

            assertTrue(
                "isSaved must be true after a valid save",
                viewModel.uiState.value.isSaved,
            )
            assertNull(
                "error must be null after a valid save",
                viewModel.uiState.value.error,
            )
        }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @Test
    fun `should show delete confirm dialog when onDeleteClicked`() =
        runTest {
            val doctor = syntheticDoctor()

            viewModel.onDeleteClicked(doctor)

            assertNotNull(
                "showDeleteConfirm must not be null after onDeleteClicked",
                viewModel.uiState.value.showDeleteConfirm,
            )
            assertEquals(doctor.id, viewModel.uiState.value.showDeleteConfirm!!.id)
        }

    @Test
    fun `should delete doctor when onDeleteConfirmed`() =
        runTest {
            val doctor = syntheticDoctor()
            fakeRepository.addDoctor(doctor)
            viewModel.loadDoctors("patient-alpha")
            advanceUntilIdle()

            viewModel.onDeleteClicked(doctor)
            viewModel.onDeleteConfirmed(doctor, "patient-alpha")
            advanceUntilIdle()

            assertNull(
                "showDeleteConfirm must be null after confirmation",
                viewModel.uiState.value.showDeleteConfirm,
            )
            assertTrue(
                "doctor must be removed from fake repository",
                fakeRepository.doctors.none { it.id == doctor.id },
            )
        }
}

// ---------------------------------------------------------------------------
// Fake repository — in-memory, synthetic data, no I/O
// ---------------------------------------------------------------------------

private class FakeDoctorRepository : DoctorRepository {
    private val _doctors = MutableStateFlow<List<Doctor>>(emptyList())

    /** Exposes the current in-memory list for assertion in tests. */
    val doctors: List<Doctor> get() = _doctors.value

    fun addDoctor(doctor: Doctor) {
        _doctors.update { current -> current + doctor }
    }

    override fun observeByPatient(patientId: String): Flow<List<Doctor>> = MutableStateFlow(_doctors.value.filter { it.patientId == patientId })

    override suspend fun getById(
        doctorId: String,
        patientId: String,
    ): Doctor? = _doctors.value.find { it.id == doctorId && it.patientId == patientId }

    override suspend fun insert(doctor: Doctor) {
        _doctors.update { current -> current + doctor }
    }

    override suspend fun update(doctor: Doctor) {
        _doctors.update { current ->
            current.map { if (it.id == doctor.id) doctor else it }
        }
    }

    override suspend fun delete(doctor: Doctor) {
        _doctors.update { current -> current.filter { it.id != doctor.id } }
    }
}
