package com.medihistory.ai.ui.privacy

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medihistory.ai.data.database.MediHistoryDatabase
import com.medihistory.ai.data.privacy.DataDeletion
import com.medihistory.ai.data.privacy.DataExporter
import com.medihistory.ai.data.security.SecurePreferencesStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for [PrivacyScreen].
 *
 * Manages data export (JSON to app-private cache) and two-step deletion flow.
 *
 * Deletion flow:
 * 1. [onDeleteStep1Clicked] → shows consequences dialog with cloud backup retention note.
 * 2. User taps "I understand, continue" → [onDeleteStep2Triggered] → shows final confirm.
 * 3. User types "DELETE" and taps confirm → [onDeleteStep2Confirmed] → actual deletion.
 *
 * Privacy invariants:
 * - NEVER silently delete or auto-export.
 * - Export writes only to app-private cache; caller shares via FileProvider.
 * - No patient data (names, content) is written to Logcat or UiState.
 * - After deletion, [PrivacyUiState.deletionResult] is set so the caller can
 *   navigate to login/onboarding.
 */
@HiltViewModel
class PrivacyViewModel @Inject constructor(
    private val database: MediHistoryDatabase,
    private val securePreferences: SecurePreferencesStore,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrivacyUiState())
    val uiState: StateFlow<PrivacyUiState> = _uiState.asStateFlow()

    // ── Cloud backup retention explanation text ──────────────────────────────

    /**
     * Text shown in the Step 1 deletion dialog explaining cloud backup retention.
     *
     * Exposed as a constant so UI tests can verify its presence without rendering.
     */
    val cloudBackupRetentionExplanation: String = CLOUD_BACKUP_RETENTION_EXPLANATION

    // ── Export ───────────────────────────────────────────────────────────────

    /**
     * Initiates a data export for [patientId].
     *
     * On success [PrivacyUiState.exportFile] is set; the caller must share it
     * via a FileProvider URI. Never shares the file path directly.
     *
     * @param patientId The patient whose data is exported.
     */
    fun onExportClicked(patientId: String) {
        _uiState.update { it.copy(isExporting = true, exportFile = null, error = null) }
        viewModelScope.launch {
            runCatching {
                DataExporter.export(
                    context = context,
                    patientId = patientId,
                    database = database,
                )
            }.fold(
                onSuccess = { file ->
                    _uiState.update { it.copy(isExporting = false, exportFile = file) }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            error = "Export failed. Please try again.",
                        )
                    }
                },
            )
        }
    }

    // ── Deletion — Step 1 ────────────────────────────────────────────────────

    /**
     * Shows the first deletion confirmation dialog.
     *
     * The dialog explains what will be deleted and includes the cloud backup
     * retention note. No deletion occurs at this step.
     */
    fun onDeleteStep1Clicked() {
        _uiState.update { it.copy(showDeleteStep1 = true, error = null) }
    }

    // ── Deletion — Step 1 → Step 2 ───────────────────────────────────────────

    /**
     * Advances from the first confirmation to the second (final) confirmation dialog.
     *
     * Called when the user taps "I understand, continue" in Step 1.
     * No deletion occurs at this step.
     */
    fun onDeleteStep2Triggered() {
        _uiState.update { it.copy(showDeleteStep1 = false, showDeleteStep2 = true) }
    }

    // ── Deletion — Step 2 confirmed ──────────────────────────────────────────

    /**
     * Performs permanent deletion after the user types "DELETE" in Step 2.
     *
     * On success [PrivacyUiState.deletionResult] is set and both dialogs are
     * dismissed. The caller must navigate to login/onboarding.
     * On failure [PrivacyUiState.error] is set; the dialogs are dismissed.
     *
     * This function MUST only be called when the user has typed exactly "DELETE"
     * in the confirmation field. The UI enforces this gate by disabling the button.
     *
     * @param patientId The patient whose data is permanently deleted.
     */
    fun onDeleteStep2Confirmed(patientId: String) {
        _uiState.update {
            it.copy(
                showDeleteStep1 = false,
                showDeleteStep2 = false,
                isDeleting = true,
                error = null,
            )
        }
        viewModelScope.launch {
            runCatching {
                DataDeletion.deleteAllData(
                    context = context,
                    patientId = patientId,
                    database = database,
                    securePreferences = securePreferences,
                )
            }.fold(
                onSuccess = { result ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            deletionResult = result,
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            error = "Deletion failed. Please try again.",
                        )
                    }
                },
            )
        }
    }

    // ── Cancel ───────────────────────────────────────────────────────────────

    /**
     * Cancels any in-progress deletion dialog and resets dialog state.
     *
     * No deletion occurs. Called when the user taps "Cancel" in either dialog.
     */
    fun onDeleteCancelled() {
        _uiState.update {
            it.copy(showDeleteStep1 = false, showDeleteStep2 = false)
        }
    }

    // ── Export file consumed ─────────────────────────────────────────────────

    /**
     * Clears the export file from state after the caller has shared it.
     *
     * Must be called once the share sheet has been launched to avoid re-showing
     * the share sheet on recomposition.
     */
    fun onExportFileConsumed() {
        _uiState.update { it.copy(exportFile = null) }
    }

    companion object {
        /**
         * Cloud backup retention explanation shown in the Step 1 deletion dialog.
         *
         * Exposed here so UI tests can verify the text without rendering the screen.
         */
        const val CLOUD_BACKUP_RETENTION_EXPLANATION =
            "IMPORTANT: If cloud backup is enabled, copies may remain on the cloud " +
                "server according to the backup retention policy. Contact support to " +
                "request complete cloud deletion."
    }
}
