package com.medihistory.ai.ui.safety

import com.medihistory.ai.data.dao.DuplicateAlertDao
import com.medihistory.ai.data.model.DuplicateAlert
import com.medihistory.ai.data.model.MatchType
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

/**
 * Unit tests for [SafetyCenterViewModel].
 *
 * Uses [FakeDuplicateAlertDao] — no Room or real storage is accessed.
 * Synthetic fixtures only (no real medicine brand names or patient names).
 *
 * Medical safety assertions:
 * - No UI state field is named "safe" or "clear".
 * - [InteractionCheckStatus.NotConfigured] is always set when ENABLE_INTERACTION_CHECK
 *   is false — never [InteractionCheckStatus.Results] with an empty list.
 * - Source-unavailable messages must direct to doctor/pharmacist, not imply safety.
 *
 * Naming convention: `given_[state]_when_[action]_then_[outcome]`
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SafetyCenterViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeDuplicateAlertDao
    private lateinit var viewModel: SafetyCenterViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeDuplicateAlertDao()
        viewModel = SafetyCenterViewModel(fakeDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fixtures — synthetic data only
    // ─────────────────────────────────────────────────────────────────────────

    private fun syntheticAlert(
        id: String = "alert-001",
        patientId: String = "test-patient-alpha",
        isReviewed: Boolean = false,
    ) = DuplicateAlert(
        id = id,
        patientId = patientId,
        medicineAId = "med-001",
        medicineBId = "med-002",
        matchingIngredient = "synthamolide",
        matchType = MatchType.EXACT,
        confidence = 0.95f,
        isReviewed = isReviewed,
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Load tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun given_unreviewed_alert_when_load_called_then_unreviewedDuplicateCount_is_1() =
        runTest {
            fakeDao.insert(syntheticAlert(isReviewed = false))

            viewModel.load("test-patient-alpha")
            advanceUntilIdle()

            assertEquals(1, viewModel.uiState.value.unreviewedDuplicateCount)
        }

    @Test
    fun given_no_alerts_when_load_called_then_unreviewedDuplicateCount_is_0() =
        runTest {
            viewModel.load("test-patient-alpha")
            advanceUntilIdle()

            assertEquals(0, viewModel.uiState.value.unreviewedDuplicateCount)
        }

    @Test
    fun given_reviewed_alert_when_load_called_then_unreviewedDuplicateCount_is_0() =
        runTest {
            fakeDao.insert(syntheticAlert(isReviewed = true))

            viewModel.load("test-patient-alpha")
            advanceUntilIdle()

            assertEquals(
                "Reviewed alerts must not be counted as needing review",
                0,
                viewModel.uiState.value.unreviewedDuplicateCount,
            )
        }

    @Test
    fun given_mixed_alerts_when_load_called_then_only_unreviewed_are_counted() =
        runTest {
            fakeDao.insert(syntheticAlert(id = "alert-001", isReviewed = false))
            fakeDao.insert(syntheticAlert(id = "alert-002", isReviewed = true))
            fakeDao.insert(syntheticAlert(id = "alert-003", isReviewed = false))

            viewModel.load("test-patient-alpha")
            advanceUntilIdle()

            assertEquals(2, viewModel.uiState.value.unreviewedDuplicateCount)
        }

    @Test
    fun given_different_patient_when_load_called_then_count_is_scoped_to_patientId() =
        runTest {
            // alert for "test-patient-alpha" — should be counted
            fakeDao.insert(syntheticAlert(id = "alert-001", patientId = "test-patient-alpha", isReviewed = false))
            // alert for different patient — must NOT be counted
            fakeDao.insert(syntheticAlert(id = "alert-002", patientId = "test-patient-beta", isReviewed = false))

            viewModel.load("test-patient-alpha")
            advanceUntilIdle()

            assertEquals(
                "Count must be scoped to the requested patientId only",
                1,
                viewModel.uiState.value.unreviewedDuplicateCount,
            )
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Interaction check tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun given_interaction_check_flag_false_when_checkInteractions_called_then_status_is_NotConfigured() =
        runTest {
            // BuildConfig.ENABLE_INTERACTION_CHECK is false in the test environment
            // (the default in build.gradle.kts when local.properties is absent).
            viewModel.checkInteractions("test-patient-alpha")
            advanceUntilIdle()

            val status = viewModel.uiState.value.interactionCheckStatus
            assertTrue(
                "Expected NotConfigured but was $status",
                status is InteractionCheckStatus.NotConfigured,
            )
        }

    @Test
    fun given_NotConfigured_status_when_inspected_then_is_not_Results_with_empty_list() =
        runTest {
            viewModel.checkInteractions("test-patient-alpha")
            advanceUntilIdle()

            val status = viewModel.uiState.value.interactionCheckStatus

            // NotConfigured is semantically different from Results(emptyList()).
            // An empty list would mean "checking ran and found nothing".
            // NotConfigured means "checking could not be attempted".
            assertFalse(
                "NotConfigured must not be represented as Results(emptyList()) — that would " +
                    "falsely imply that no interactions were found",
                status is InteractionCheckStatus.Results,
            )
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Medical safety / naming tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun given_uiState_when_inspected_then_no_field_is_named_safe_or_clear() {
        // Verify via Java reflection (available without kotlin-reflect) that
        // SafetyCenterUiState has no declared field named 'safe' or 'clear'.
        val fieldNames =
            SafetyCenterUiState::class.java.declaredFields
                .map { it.name.lowercase() }
        assertFalse(
            "SafetyCenterUiState must not have a field named 'safe'",
            fieldNames.any { it == "safe" },
        )
        assertFalse(
            "SafetyCenterUiState must not have a field named 'clear'",
            fieldNames.any { it == "clear" },
        )
    }

    @Test
    fun given_NotConfigured_status_when_default_uiState_then_interactionCheckStatus_is_NotConfigured() {
        val defaultState = SafetyCenterUiState()
        assertTrue(
            "Default interactionCheckStatus must be NotConfigured",
            defaultState.interactionCheckStatus is InteractionCheckStatus.NotConfigured,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Fake DAO — in-memory, synthetic data, no I/O
// ─────────────────────────────────────────────────────────────────────────────

private class FakeDuplicateAlertDao : DuplicateAlertDao {
    private val _alerts = MutableStateFlow<List<DuplicateAlert>>(emptyList())

    override fun observeByPatient(patientId: String): Flow<List<DuplicateAlert>> = _alerts.map { list -> list.filter { it.patientId == patientId } }

    override fun observeUnreviewedCount(patientId: String): Flow<Int> =
        _alerts.map { list ->
            list.count { it.patientId == patientId && !it.isReviewed }
        }

    override suspend fun insert(alert: DuplicateAlert) {
        _alerts.update { current -> current + alert }
    }

    override suspend fun markReviewed(
        id: String,
        patientId: String,
        timestamp: Long,
    ) {
        _alerts.update { current ->
            current.map { alert ->
                if (alert.id == id && alert.patientId == patientId) {
                    alert.copy(isReviewed = true, reviewedAt = timestamp)
                } else {
                    alert
                }
            }
        }
    }

    override suspend fun deleteByPatient(patientId: String) {
        _alerts.update { current -> current.filter { it.patientId != patientId } }
    }

    override suspend fun getById(
        id: String,
        patientId: String,
    ): DuplicateAlert? = _alerts.value.find { it.id == id && it.patientId == patientId }

    override suspend fun updateExplanation(
        id: String,
        patientId: String,
        explanation: String,
    ) {
        _alerts.update { current ->
            current.map { alert ->
                if (alert.id == id && alert.patientId == patientId) {
                    alert.copy(explanation = explanation)
                } else {
                    alert
                }
            }
        }
    }
}
