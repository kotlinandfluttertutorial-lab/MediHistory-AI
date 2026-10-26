package com.medihistory.ai.ui.prescription

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [PrescriptionListViewModel].
 *
 * Uses synthetic fixtures only — no real patient or medicine data.
 * Naming convention: `given_[state]_when_[action]_then_[outcome]`
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrescriptionListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: PrescriptionListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = PrescriptionListViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given viewModel created when observed immediately then initial state isLoading is true`() {
        // The ViewModel starts with isLoading = true before the coroutine has a chance to run.
        assertTrue(
            "Initial state must have isLoading = true",
            viewModel.uiState.value.isLoading,
        )
    }

    @Test
    fun `given initial load when coroutine completes then isLoading is false and prescriptions are not empty`() =
        runTest {
            advanceUntilIdle()

            assertFalse(
                "isLoading must be false after load completes",
                viewModel.uiState.value.isLoading,
            )
            assertNotEquals(
                "prescriptions must not be empty after load",
                0,
                viewModel.uiState.value.prescriptions.size,
            )
        }

    @Test
    fun `given synthetic samples when loaded then all entries have non-empty id`() = runTest {
        advanceUntilIdle()

        val emptyIdEntries = viewModel.uiState.value.prescriptions.filter { it.id.isBlank() }
        assertTrue(
            "Every prescription must have a non-empty id, but found: $emptyIdEntries",
            emptyIdEntries.isEmpty(),
        )
    }

    @Test
    fun `given synthetic samples when loaded then Doctor removed entry exists`() = runTest {
        advanceUntilIdle()

        val hasDoctorRemoved = viewModel.uiState.value.prescriptions.any {
            it.doctorName == "Doctor removed"
        }
        assertTrue(
            "Synthetic samples must include an entry with doctorName='Doctor removed'",
            hasDoctorRemoved,
        )
    }
}
