package com.medihistory.ai.ui.ocr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medihistory.ai.data.dao.ExtractedFieldDao
import com.medihistory.ai.data.dao.PrescriptionDao
import com.medihistory.ai.data.model.OcrStatus
import com.medihistory.ai.data.ocr.PostReviewProcessorInterface
import com.medihistory.ai.data.ocr.ReviewState
import com.medihistory.ai.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for [OcrReviewScreen].
 *
 * Medical-safety invariants enforced here:
 * - [rawValue] is NEVER changed by any method. Only [patientValue] changes.
 * - Review state machine:
 *     PENDING_REVIEW → REVIEWED   (confirm / save edit)
 *     PENDING_REVIEW → REJECTED   (reject)
 *     REVIEWED       → PENDING_REVIEW  (un-review)
 *     REJECTED       → PENDING_REVIEW  (un-reject)
 *   No other transitions are permitted.
 * - Fields with null [rawValue] are never confirmed (no-op).
 * - When every field with a non-null rawValue has been reviewed or rejected,
 *   the prescription's [OcrStatus] is advanced to [OcrStatus.REVIEWED].
 *
 * Privacy: rawValue and patientValue are never logged.
 */
@HiltViewModel
class OcrReviewViewModel @Inject constructor(
    private val extractedFieldDao: ExtractedFieldDao,
    private val prescriptionDao: PrescriptionDao,
    private val postReviewProcessor: PostReviewProcessorInterface,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OcrReviewUiState(isLoading = true))
    val uiState: StateFlow<OcrReviewUiState> = _uiState.asStateFlow()

    // Cached patientId for updateOcrStatus; set once during load().
    private var cachedPatientId: String? = null

    /**
     * Loads and observes the extracted fields for [prescriptionId].
     * Safe to call multiple times; each call cancels the previous observation.
     */
    fun load(prescriptionId: String) {
        _uiState.update { it.copy(prescriptionId = prescriptionId, isLoading = true, error = null) }

        // Resolve the patientId for later OcrStatus update — no content exposed.
        viewModelScope.launch {
            cachedPatientId = prescriptionDao.getPatientId(prescriptionId)
                ?: sessionManager.getPatientId()
        }

        extractedFieldDao
            .observeByPrescription(prescriptionId)
            .onEach { entities ->
                val fieldStates = entities.map { entity ->
                    val display = entity.patientValue
                        ?: entity.rawValue
                        ?: "Not extracted"
                    FieldReviewState(
                        fieldId      = entity.id,
                        fieldName    = formatFieldName(entity.fieldName),
                        rawValue     = entity.rawValue,
                        patientValue = entity.patientValue,
                        displayValue = display,
                        confidence   = entity.confidence,
                        reviewState  = entity.reviewState,
                        isEditing    = false,
                        editText     = entity.patientValue ?: entity.rawValue ?: "",
                    )
                }
                val allReviewed = fieldStates
                    .filter { it.rawValue != null }
                    .all { it.reviewState != ReviewState.PENDING_REVIEW }

                _uiState.update {
                    it.copy(
                        fields      = fieldStates,
                        isLoading   = false,
                        error       = null,
                        allReviewed = allReviewed,
                    )
                }

                // Advance OcrStatus once every extractable field has been reviewed/rejected.
                if (allReviewed && fieldStates.isNotEmpty()) {
                    val pid = cachedPatientId ?: return@onEach
                    prescriptionDao.updateOcrStatus(
                        id        = prescriptionId,
                        patientId = pid,
                        status    = OcrStatus.REVIEWED,
                    )
                    // Apply confirmed field values to Doctor, Prescription, and Medicine tables.
                    postReviewProcessor.process(prescriptionId, pid)
                }
            }
            .catch { _ ->
                _uiState.update {
                    it.copy(isLoading = false, error = "Failed to load fields.")
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Confirms a field as-is: PENDING_REVIEW → REVIEWED, patientValue = rawValue.
     * No-op when the field has null rawValue (nothing to confirm).
     * [rawValue] is NEVER changed.
     */
    fun onConfirmField(fieldId: String) {
        val field = findField(fieldId) ?: return
        if (field.rawValue == null) return              // nothing to confirm
        if (field.reviewState != ReviewState.PENDING_REVIEW) return

        viewModelScope.launch {
            // rawValue is not passed to updateReview — only patientValue changes.
            extractedFieldDao.updateReview(
                id           = fieldId,
                state        = ReviewState.REVIEWED,
                patientValue = field.rawValue,             // set patientValue = rawValue
            )
        }
    }

    /**
     * Opens the edit text field for a field in PENDING_REVIEW.
     * Sets isEditing = true in local UI state only; no DB write occurs here.
     */
    fun onEditField(fieldId: String) {
        _uiState.update { state ->
            state.copy(
                fields = state.fields.map { f ->
                    if (f.fieldId == fieldId) {
                        f.copy(isEditing = true, editText = f.patientValue ?: f.rawValue ?: "")
                    } else {
                        f
                    }
                },
            )
        }
    }

    /**
     * Updates the edit buffer for a field. No DB write — buffer only.
     */
    fun onEditTextChange(fieldId: String, text: String) {
        _uiState.update { state ->
            state.copy(
                fields = state.fields.map { f ->
                    if (f.fieldId == fieldId) f.copy(editText = text) else f
                },
            )
        }
    }

    /**
     * Cancels an in-progress edit, closing the edit field without any DB write.
     * The editText buffer is reset.
     */
    fun onCancelEdit(fieldId: String) {
        _uiState.update { state ->
            state.copy(
                fields = state.fields.map { f ->
                    if (f.fieldId == fieldId) {
                        f.copy(isEditing = false, editText = f.patientValue ?: f.rawValue ?: "")
                    } else {
                        f
                    }
                },
            )
        }
    }

    /**
     * Saves an edit: PENDING_REVIEW → REVIEWED, patientValue = editText.
     * [rawValue] is NEVER changed — the patient's override is stored separately.
     */
    fun onSaveEdit(fieldId: String) {
        val field = findField(fieldId) ?: return
        if (field.reviewState != ReviewState.PENDING_REVIEW) return

        viewModelScope.launch {
            // rawValue is NOT touched — only patientValue is written.
            extractedFieldDao.updateReview(
                id           = fieldId,
                state        = ReviewState.REVIEWED,
                patientValue = field.editText,            // patient override, not rawValue
            )
        }
        // Close the edit UI immediately (optimistic).
        _uiState.update { state ->
            state.copy(
                fields = state.fields.map { f ->
                    if (f.fieldId == fieldId) f.copy(isEditing = false) else f
                },
            )
        }
    }

    /**
     * Rejects a field: PENDING_REVIEW → REJECTED.
     * Does NOT delete the field — the record is preserved with REJECTED state.
     */
    fun onRejectField(fieldId: String) {
        val field = findField(fieldId) ?: return
        if (field.reviewState != ReviewState.PENDING_REVIEW) return

        viewModelScope.launch {
            extractedFieldDao.updateReview(
                id           = fieldId,
                state        = ReviewState.REJECTED,
                patientValue = null,
            )
        }
    }

    /**
     * Un-reviews or un-rejects a field back to PENDING_REVIEW.
     * Allowed from both REVIEWED and REJECTED states.
     */
    fun onUnreviewField(fieldId: String) {
        val field = findField(fieldId) ?: return
        if (field.reviewState == ReviewState.PENDING_REVIEW) return   // already pending

        viewModelScope.launch {
            extractedFieldDao.updateReview(
                id           = fieldId,
                state        = ReviewState.PENDING_REVIEW,
                patientValue = null,
            )
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun findField(fieldId: String): FieldReviewState? =
        _uiState.value.fields.find { it.fieldId == fieldId }

    /**
     * Converts snake_case field names to human-readable title case.
     * Example: "doctor_name" → "Doctor Name".
     */
    private fun formatFieldName(raw: String): String =
        raw.replace('_', ' ').split(' ')
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercaseChar() }
            }
}
