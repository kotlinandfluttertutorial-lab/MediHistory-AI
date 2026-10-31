package com.medihistory.ai.ui.patient

import com.medihistory.ai.data.model.Patient
import com.medihistory.ai.data.repository.PatientRepository
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
import java.time.LocalDate

/**
 * Unit tests for [PatientViewModel].
 *
 * Uses [FakePatientRepository] — no Room or real storage is accessed.
 * All fixtures use synthetic data only (no real patient names or personal data).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PatientViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakePatientRepository
    private lateinit var viewModel: PatientViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakePatientRepository()
        viewModel = PatientViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Fixtures — synthetic data only
    // -------------------------------------------------------------------------

    private fun syntheticPatient(
        id: String = "patient-alpha",
        name: String = "Test Patient Alpha",
    ) = Patient(
        id = id,
        name = name,
        dateOfBirth = LocalDate.of(1990, 6, 15),
        bloodGroup = "A+",
        allergies = listOf("Synthamol", "Testocillin"),
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L,
    )

    // -------------------------------------------------------------------------
    // Load
    // -------------------------------------------------------------------------

    @Test
    fun `should populate state when loadPatient called with existing id`() =
        runTest {
            val patient = syntheticPatient()
            fakeRepository.addPatient(patient)

            viewModel.loadPatient("patient-alpha")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("name must match loaded patient", patient.name, state.name)
            assertEquals("dateOfBirth must match loaded patient", patient.dateOfBirth, state.dateOfBirth)
            assertEquals("bloodGroup must match loaded patient", patient.bloodGroup, state.bloodGroup)
            assertEquals("allergies must match loaded patient", patient.allergies.orEmpty(), state.allergies)
            assertFalse("isLoading must be false after load", state.isLoading)
        }

    @Test
    fun `should leave state blank when loadPatient called with unknown id`() =
        runTest {
            viewModel.loadPatient("unknown-id")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("name must be empty for unknown patient", "", state.name)
            assertFalse("isLoading must be false", state.isLoading)
        }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    @Test
    fun `should set nameError when name is empty on save`() =
        runTest {
            // name is blank by default
            viewModel.onSaveClicked(null)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNotNull("nameError must not be null when name is blank", state.nameError)
            assertFalse("isSaved must be false when name is blank", state.isSaved)
        }

    // -------------------------------------------------------------------------
    // Save
    // -------------------------------------------------------------------------

    @Test
    fun `should set isSaved true when valid save completes`() =
        runTest {
            viewModel.onNameChange("Test Patient Alpha")
            viewModel.onSaveClicked(null)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue("isSaved must be true after a valid save", state.isSaved)
            assertNull("error must be null after a valid save", state.error)
            assertNull("nameError must be null after a valid save", state.nameError)
        }

    @Test
    fun `should update existing patient when patientId is provided`() =
        runTest {
            val patient = syntheticPatient()
            fakeRepository.addPatient(patient)

            viewModel.loadPatient("patient-alpha")
            advanceUntilIdle()

            viewModel.onNameChange("Test Patient Alpha Updated")
            viewModel.onSaveClicked("patient-alpha")
            advanceUntilIdle()

            val saved = fakeRepository.getById("patient-alpha")
            assertNotNull("saved patient must not be null", saved)
            assertEquals("name must be updated", "Test Patient Alpha Updated", saved!!.name)
            assertTrue("isSaved must be true", viewModel.uiState.value.isSaved)
        }

    // -------------------------------------------------------------------------
    // Allergy add / remove
    // -------------------------------------------------------------------------

    @Test
    fun `should add allergy to state`() =
        runTest {
            viewModel.onAllergyAdded("Synthamol")
            viewModel.onAllergyAdded("Testocillin")

            val allergies = viewModel.uiState.value.allergies
            assertEquals("allergies must contain 2 entries", 2, allergies.size)
            assertTrue("Synthamol must be in the list", "Synthamol" in allergies)
            assertTrue("Testocillin must be in the list", "Testocillin" in allergies)
        }

    @Test
    fun `should remove allergy from state`() =
        runTest {
            viewModel.onAllergyAdded("Synthamol")
            viewModel.onAllergyAdded("Testocillin")

            viewModel.onAllergyRemoved("Synthamol")

            val allergies = viewModel.uiState.value.allergies
            assertEquals("allergies must contain 1 entry after remove", 1, allergies.size)
            assertFalse("Synthamol must be removed", "Synthamol" in allergies)
            assertTrue("Testocillin must remain", "Testocillin" in allergies)
        }

    @Test
    fun `should not add blank allergy`() =
        runTest {
            viewModel.onAllergyAdded("   ")

            assertEquals("blank allergy must not be added", 0, viewModel.uiState.value.allergies.size)
        }

    @Test
    fun `should not add duplicate allergy`() =
        runTest {
            viewModel.onAllergyAdded("Synthamol")
            viewModel.onAllergyAdded("Synthamol")

            assertEquals("duplicate allergy must not be added", 1, viewModel.uiState.value.allergies.size)
        }
}

// ---------------------------------------------------------------------------
// Fake repository — in-memory, synthetic data, no I/O
// ---------------------------------------------------------------------------

private class FakePatientRepository : PatientRepository {
    private val _patients = MutableStateFlow<List<Patient>>(emptyList())

    fun addPatient(patient: Patient) {
        _patients.update { current -> current + patient }
    }

    override suspend fun getById(patientId: String): Patient? = _patients.value.find { it.id == patientId }

    override suspend fun insert(patient: Patient) {
        _patients.update { current -> current + patient }
    }

    override suspend fun update(patient: Patient) {
        _patients.update { current ->
            current.map { if (it.id == patient.id) patient else it }
        }
    }

    override suspend fun delete(patient: Patient) {
        _patients.update { current -> current.filter { it.id != patient.id } }
    }

    override fun observeAll(): Flow<List<Patient>> = _patients
}
