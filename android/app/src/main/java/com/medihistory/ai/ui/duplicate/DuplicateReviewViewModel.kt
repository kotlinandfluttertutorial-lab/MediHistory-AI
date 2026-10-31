package com.medihistory.ai.ui.duplicate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medihistory.ai.data.dao.DuplicateAlertDao
import com.medihistory.ai.data.repository.MedicineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for [DuplicateReviewScreen].
 *
 * Observes [DuplicateAlertDao.observeByPatient] and enriches each alert with medicine
 * display names from [MedicineRepository]. Exposes a single [StateFlow] of
 * [DuplicateReviewUiState] that the screen collects.
 *
 * Medical safety:
 * - "Mark as reviewed" sets [isReviewed] = true. It does NOT imply clinical approval.
 * - Reviewed alerts remain visible — they are never deleted or hidden by default.
 * - No action in this ViewModel blocks the patient from recording medicines.
 *
 * Privacy: never log [patientId] or any alert/medicine field in Logcat.
 */
@HiltViewModel
class DuplicateReviewViewModel
    @Inject
    constructor(
        private val duplicateAlertDao: DuplicateAlertDao,
        private val medicineRepository: MedicineRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DuplicateReviewUiState())
        val uiState: StateFlow<DuplicateReviewUiState> = _uiState.asStateFlow()

        /** Holds the active patientId; null means no load has been initiated yet. */
        private var activePatientId: String? = null

        // -------------------------------------------------------------------------
        // Public API
        // -------------------------------------------------------------------------

        /**
         * Starts observing duplicate alerts for [patientId].
         *
         * Safe to call multiple times. Calling with the same [patientId] is a no-op after
         * the first call, because [DuplicateAlertDao.observeByPatient] returns a live Flow.
         * Calling with a new [patientId] replaces the previous subscription.
         */
        fun loadAlerts(patientId: String) {
            if (activePatientId == patientId) return
            activePatientId = patientId
            observeAlerts(patientId)
        }

        /**
         * Toggles the expanded state of the alert with [alertId].
         * Has no effect if the alert is not found in the current list.
         */
        fun onToggleExpanded(alertId: String) {
            _uiState.update { state ->
                state.copy(
                    alerts =
                        state.alerts.map { item ->
                            if (item.alert.id == alertId) {
                                item.copy(isExpanded = !item.isExpanded)
                            } else {
                                item
                            }
                        },
                )
            }
        }

        /**
         * Marks the alert with [alertId] as reviewed.
         *
         * Medical safety: reviewing an alert means the patient has seen it. It does NOT
         * indicate that the drug combination has been approved by a doctor.
         * Reviewed alerts remain visible — they are not deleted.
         *
         * Security: the update is scoped to [activePatientId]; cross-patient mutation is
         * prevented at the DAO level.
         */
        fun onMarkReviewed(alertId: String) {
            val patientId = activePatientId ?: return
            viewModelScope.launch {
                duplicateAlertDao.markReviewed(
                    id = alertId,
                    patientId = patientId,
                    timestamp = System.currentTimeMillis(),
                )
                // The Flow from observeByPatient will emit an updated list automatically;
                // no manual state update is needed here.
            }
        }

        /**
         * Toggles whether reviewed alerts are shown alongside unreviewed ones.
         *
         * When [DuplicateReviewUiState.showReviewedAlerts] is false, only unreviewed
         * alerts appear in [DuplicateReviewUiState.alerts].
         */
        fun onToggleShowReviewed() {
            _uiState.update { it.copy(showReviewedAlerts = !it.showReviewedAlerts) }
            // Re-apply the filter against the latest full list kept in _allAlerts.
            activePatientId?.let { applyFilter(it) }
        }

        // -------------------------------------------------------------------------
        // Internal helpers
        // -------------------------------------------------------------------------

        /** Holds all alert items fetched from the DAO before the show-reviewed filter. */
        private var _allAlerts: List<DuplicateAlertItem> = emptyList()

        private fun observeAlerts(patientId: String) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }

                duplicateAlertDao
                    .observeByPatient(patientId)
                    .catch { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "Failed to load duplicate alerts.",
                            )
                        }
                    }
                    .collect { alerts ->
                        // Enrich each alert with medicine display names.
                        val items =
                            alerts.map { alert ->
                                val medA = medicineRepository.getById(alert.medicineAId, patientId)
                                val medB = medicineRepository.getById(alert.medicineBId, patientId)

                                // Preserve existing isExpanded state across updates.
                                val existing = _allAlerts.find { it.alert.id == alert.id }

                                DuplicateAlertItem(
                                    alert = alert,
                                    medicineAName = medA?.medicineName ?: alert.medicineAId,
                                    medicineBName = medB?.medicineName ?: alert.medicineBId,
                                    prescriptionAId = medA?.prescriptionId,
                                    prescriptionBId = medB?.prescriptionId,
                                    isExpanded = existing?.isExpanded ?: false,
                                )
                            }

                        _allAlerts = items
                        applyFilter(patientId)
                        _uiState.update { it.copy(isLoading = false, error = null) }
                    }
            }
        }

        /**
         * Applies the show-reviewed filter to [_allAlerts] and updates [_uiState].
         * Called after every DAO emission and after toggling [showReviewedAlerts].
         */
        private fun applyFilter(patientId: String) {
            _uiState.update { state ->
                val filtered =
                    if (state.showReviewedAlerts) {
                        _allAlerts
                    } else {
                        _allAlerts.filter { !it.alert.isReviewed }
                    }
                state.copy(alerts = filtered)
            }
        }
    }
