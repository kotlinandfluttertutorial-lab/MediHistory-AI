package com.medihistory.ai.ui.ocr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medihistory.ai.data.dao.ExtractedFieldDao
import com.medihistory.ai.data.dao.PrescriptionDao
import com.medihistory.ai.data.model.OcrStatus
import com.medihistory.ai.data.ocr.OcrCorrectionEngine
import com.medihistory.ai.data.ocr.PostReviewProcessorInterface
import com.medihistory.ai.data.ocr.ReviewState
import com.medihistory.ai.data.repository.DoctorRepository
import com.medihistory.ai.data.security.SecurePreferences
import com.medihistory.ai.data.security.SecurePreferencesStore
import com.medihistory.ai.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** State of the patient's consent to use Gemini for OCR field extraction. */
enum class GeminiConsentState {
    /** Consent has not been recorded yet — dialog should be shown. */
    UNKNOWN,
    /** Patient has explicitly granted consent. */
    GRANTED,
    /** Patient has explicitly declined — use ML Kit fallback silently. */
    DECLINED,
}

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
    private val securePrefs: SecurePreferencesStore,
    private val ocrCorrectionEngine: OcrCorrectionEngine,
    private val doctorRepository: DoctorRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OcrReviewUiState(isLoading = true))
    val uiState: StateFlow<OcrReviewUiState> = _uiState.asStateFlow()

    private val _geminiConsentState = MutableStateFlow(readConsentState())
    val geminiConsentState: StateFlow<GeminiConsentState> = _geminiConsentState.asStateFlow()

    // Cached patientId for updateOcrStatus; set once during load().
    private var cachedPatientId: String? = null

    // Set to true the first time the patient confirms, edits, or rejects any field.
    // Auto-navigation only fires once the patient has actually interacted.
    private var hasUserInteracted = false

    /**
     * Loads and observes the extracted fields for [prescriptionId].
     * Safe to call multiple times; each call cancels the previous observation.
     */
    fun load(prescriptionId: String) {
        hasUserInteracted = false
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
                val allReviewed = fieldStates.isNotEmpty() &&
                    fieldStates.filter { it.rawValue != null }.let { reviewable ->
                        reviewable.isNotEmpty() &&
                            reviewable.all { it.reviewState != ReviewState.PENDING_REVIEW }
                    }

                _uiState.update {
                    it.copy(
                        fields              = fieldStates,
                        isLoading           = false,
                        error               = null,
                        allReviewed         = allReviewed,
                        hasUserInteracted   = hasUserInteracted,
                    )
                }

                // Compute correction suggestions for low-confidence PENDING fields (F14).
                val patientIdForSuggestions = cachedPatientId ?: sessionManager.getPatientId()
                if (patientIdForSuggestions != null) {
                    val doctors = doctorRepository.observeByPatient(patientIdForSuggestions).first()
                    val suggestions = entities
                        .filter { entity ->
                            entity.reviewState == ReviewState.PENDING_REVIEW &&
                                entity.confidence < 0.70f &&
                                entity.rawValue != null
                        }
                        .mapNotNull { entity ->
                            val suggestion = ocrCorrectionEngine.suggest(
                                fieldName    = entity.fieldName,
                                rawValue     = entity.rawValue!!,
                                allFields    = entities,
                                knownDoctors = doctors,
                            )
                            if (suggestion != null) entity.id to suggestion else null
                        }
                        .toMap()
                    _uiState.update { it.copy(suggestedValues = suggestions) }
                }

                // Advance OcrStatus once every extractable field has been reviewed/rejected.
                // Only fire PostReviewProcessor if the patient has actually interacted —
                // prevents auto-processing when the worker finishes before the screen opens.
                if (allReviewed && fieldStates.isNotEmpty() && hasUserInteracted) {
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
        if (field.rawValue == null) return
        if (field.reviewState != ReviewState.PENDING_REVIEW) return
        hasUserInteracted = true
        viewModelScope.launch {
            extractedFieldDao.updateReview(
                id           = fieldId,
                state        = ReviewState.REVIEWED,
                patientValue = field.rawValue,
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
        hasUserInteracted = true
        viewModelScope.launch {
            extractedFieldDao.updateReview(
                id           = fieldId,
                state        = ReviewState.REVIEWED,
                patientValue = field.editText,
            )
        }
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
        hasUserInteracted = true
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

    /**
     * Applies an [OcrCorrectionEngine] suggestion to [fieldId].
     *
     * Transition: PENDING_REVIEW → REVIEWED with patientValue = [suggestion].
     * [rawValue] is NEVER changed — the suggestion is stored as [patientValue].
     *
     * Medical safety: only called after the patient explicitly confirms the suggestion dialog.
     */
    fun onApplySuggestion(fieldId: String, suggestion: String) {
        val field = findField(fieldId) ?: return
        if (field.reviewState != ReviewState.PENDING_REVIEW) return
        hasUserInteracted = true
        viewModelScope.launch {
            extractedFieldDao.updateReview(
                id           = fieldId,
                state        = ReviewState.REVIEWED,
                patientValue = suggestion,
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

    // -------------------------------------------------------------------------
    // Gemini consent
    // -------------------------------------------------------------------------

    /** Reads the persisted consent state from EncryptedSharedPreferences. */
    private fun readConsentState(): GeminiConsentState =
        when (securePrefs.getString(SecurePreferences.KEY_GEMINI_OCR_CONSENT)) {
            "true"  -> GeminiConsentState.GRANTED
            "false" -> GeminiConsentState.DECLINED
            else    -> GeminiConsentState.UNKNOWN
        }

    /**
     * Called when the patient taps "Allow" in [GeminiConsentDialog].
     * Persists consent and updates the StateFlow so the screen re-composes.
     */
    fun onGeminiConsentGranted() {
        securePrefs.putString(SecurePreferences.KEY_GEMINI_OCR_CONSENT, "true")
        _geminiConsentState.value = GeminiConsentState.GRANTED
    }

    /**
     * Called when the patient taps "Not now" in [GeminiConsentDialog].
     * Persists the decline so the dialog is not shown again.
     * The OCR pipeline will use the ML Kit fallback silently.
     */
    fun onGeminiConsentDeclined() {
        securePrefs.putString(SecurePreferences.KEY_GEMINI_OCR_CONSENT, "false")
        _geminiConsentState.value = GeminiConsentState.DECLINED
    }
}
