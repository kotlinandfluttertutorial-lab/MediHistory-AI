package com.medihistory.ai.ui.safety

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medihistory.ai.BuildConfig
import com.medihistory.ai.data.dao.DuplicateAlertDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for [SafetyCenterScreen].
 *
 * Provides:
 * 1. A live count of unreviewed potential duplicate alerts (always available,
 *    independent of any reference source).
 * 2. Interaction-check status — gated on [BuildConfig.ENABLE_INTERACTION_CHECK].
 *    When the flag is false (or no reference source is configured) the status is
 *    [InteractionCheckStatus.NotConfigured]. This explicitly avoids any implication
 *    that medicines are safe to combine.
 *
 * Medical safety invariants:
 * - This ViewModel NEVER sets a status that implies a drug combination is safe.
 * - [InteractionCheckStatus.NotConfigured] is the correct state when checking is
 *   unavailable — NOT [InteractionCheckStatus.Results] with an empty list.
 * - The word "safe" must not appear in any message produced by this class.
 * - No missing dose, duration, diagnosis, allergy, or medicine status is inferred.
 *
 * Privacy: never log patient IDs, medicine names, or alert content.
 */
@HiltViewModel
class SafetyCenterViewModel @Inject constructor(
    private val duplicateAlertDao: DuplicateAlertDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SafetyCenterUiState())
    val uiState: StateFlow<SafetyCenterUiState> = _uiState.asStateFlow()

    /**
     * Loads the unreviewed duplicate alert count for [patientId].
     *
     * The duplicate-alert section is always available regardless of whether an
     * interaction-check reference source is configured.
     *
     * Privacy: [patientId] is used only as a DAO filter — it is not logged.
     */
    fun load(patientId: String) {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            duplicateAlertDao.observeUnreviewedCount(patientId)
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            // Keep existing interactionCheckStatus; only the count failed.
                        )
                    }
                }
                .collect { count ->
                    _uiState.update {
                        it.copy(
                            unreviewedDuplicateCount = count,
                            isLoading = false,
                        )
                    }
                }
        }
    }

    /**
     * Triggers an interaction check for [patientId].
     *
     * When [BuildConfig.ENABLE_INTERACTION_CHECK] is false — or when no verified
     * reference source has been configured — the status is set to
     * [InteractionCheckStatus.NotConfigured].
     *
     * This state explicitly signals to the UI that:
     * - Checking was NOT performed.
     * - The absence of results does NOT mean medicines are safe to combine.
     * - The patient should consult a doctor or pharmacist.
     *
     * When a reference source is eventually integrated, this function will call it and
     * set [InteractionCheckStatus.Results] or [InteractionCheckStatus.Error] accordingly.
     *
     * Medical safety: this function NEVER sets [InteractionCheckStatus.Results] with an
     * empty list as a fallback for "unavailable" — that would falsely imply that
     * "no interactions were found".
     */
    fun checkInteractions(patientId: String) {
        if (!BuildConfig.ENABLE_INTERACTION_CHECK) {
            // Interaction checking is disabled via build flag.
            // Set NotConfigured — do NOT set Results(emptyList()).
            _uiState.update {
                it.copy(interactionCheckStatus = InteractionCheckStatus.NotConfigured)
            }
            return
        }

        // ENABLE_INTERACTION_CHECK is true but no reference source is yet integrated.
        // Set NotConfigured — still explicitly not "no interactions found".
        // When a real reference source is wired in, replace this block with the
        // actual check logic that sets Results or Error.
        _uiState.update {
            it.copy(interactionCheckStatus = InteractionCheckStatus.NotConfigured)
        }
    }
}
