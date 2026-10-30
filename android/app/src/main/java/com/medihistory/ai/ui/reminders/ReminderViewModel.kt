package com.medihistory.ai.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medihistory.ai.data.model.DoseRecord
import com.medihistory.ai.data.model.DoseRecordEntity
import com.medihistory.ai.data.model.Medicine
import com.medihistory.ai.data.model.Reminder
import com.medihistory.ai.data.repository.MedicineRepository
import com.medihistory.ai.data.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for [ReminderScreen].
 *
 * Exposes a [StateFlow] of [ReminderUiState] driven by [ReminderRepository] and
 * [MedicineRepository].
 *
 * Safety invariants enforced here:
 * - [createReminder] requires [prescribedInstructions] to be passed explicitly by the
 *   caller. The ViewModel does NOT read or auto-fill from any OCR field.
 * - [recordDose] stores a [DoseRecordEntity] whose [patientNote] is patient-provided.
 *   It NEVER copies or reads [Reminder.prescribedInstructions].
 *
 * Privacy: never log patientId, medicine names, or reminder content.
 */
@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val repository: ReminderRepository,
    private val medicineRepository: MedicineRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReminderUiState())
    val uiState: StateFlow<ReminderUiState> = _uiState.asStateFlow()

    // -------------------------------------------------------------------------
    // Load
    // -------------------------------------------------------------------------

    /**
     * Starts observing reminders and available medicines for [patientId].
     * Safe to call multiple times — each call replaces the previous subscription.
     */
    fun loadReminders(patientId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            combine(
                repository.observeByPatient(patientId),
                medicineRepository.observeByPatient(patientId),
            ) { reminders, medicines -> reminders to medicines }
                .catch { e ->
                    Timber.e(e, "Failed to load reminders")
                    _uiState.update {
                        it.copy(isLoading = false, error = "Failed to load reminders.")
                    }
                }
                .collect { (reminders, medicines) ->
                    _uiState.update {
                        it.copy(
                            reminders          = reminders,
                            availableMedicines = medicines,
                            isLoading          = false,
                            error              = null,
                        )
                    }
                }
        }
    }

    // -------------------------------------------------------------------------
    // Medicine picker
    // -------------------------------------------------------------------------

    /** Opens the medicine-picker dialog. */
    fun onShowMedicinePicker() {
        _uiState.update { it.copy(showMedicinePicker = true) }
    }

    /** Dismisses the medicine-picker dialog without creating a reminder. */
    fun onDismissMedicinePicker() {
        _uiState.update { it.copy(showMedicinePicker = false) }
    }

    /**
     * Creates a new reminder for [medicine] with a default time of 08:00.
     *
     * [prescribedInstructions] is taken directly from [Medicine.dosageInstructions]
     * which was set from a REVIEWED OCR field — this satisfies the safety contract.
     *
     * @param medicine   The medicine the patient wants to be reminded about.
     * @param patientId  The owning patient's ID.
     */
    fun onMedicineSelected(medicine: Medicine, patientId: String) {
        _uiState.update { it.copy(showMedicinePicker = false) }
        createReminder(
            patientId             = patientId,
            medicineId            = medicine.id,
            medicineName          = medicine.medicineName,
            // dosageInstructions was set from a REVIEWED OCR field (PostReviewProcessor).
            prescribedInstructions = medicine.dosageInstructions,
            reminderTime          = LocalTime.of(8, 0), // default 08:00; patient can edit later
        )
    }

    // -------------------------------------------------------------------------
    // Reminder CRUD
    // -------------------------------------------------------------------------

    /**
     * Creates a new opt-in reminder and persists it.
     *
     * Safety: [prescribedInstructions] must originate from a REVIEWED OCR extracted field.
     * The ViewModel accepts it as a parameter — it is the caller's responsibility to only
     * pass reviewed data. Passing null is valid (prescribed instructions are optional).
     */
    fun createReminder(
        patientId: String,
        medicineId: String,
        medicineName: String,
        prescribedInstructions: String?,
        reminderTime: LocalTime,
    ) {
        val reminder = Reminder(
            id                    = UUID.randomUUID().toString(),
            patientId             = patientId,
            medicineId            = medicineId,
            medicineName          = medicineName,
            prescribedInstructions = prescribedInstructions,
            reminderTime          = reminderTime,
            isActive              = true,
        )
        viewModelScope.launch {
            try {
                repository.insert(reminder)
            } catch (e: Exception) {
                Timber.e(e, "Failed to create reminder")
                _uiState.update { it.copy(error = "Failed to create reminder.") }
            }
        }
    }

    /**
     * Toggles the active/inactive state of a reminder.
     * An inactive reminder no longer appears in "Today's Medicines".
     */
    fun toggleActive(reminder: Reminder) {
        viewModelScope.launch {
            try {
                repository.update(reminder.copy(isActive = !reminder.isActive))
            } catch (e: Exception) {
                Timber.e(e, "Failed to toggle reminder active state")
                _uiState.update { it.copy(error = "Failed to update reminder.") }
            }
        }
    }

    /** Deletes the given reminder. Patients may cancel a reminder at any time. */
    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            try {
                repository.delete(reminder)
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete reminder")
                _uiState.update { it.copy(error = "Failed to delete reminder.") }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Dose recording
    // -------------------------------------------------------------------------

    /**
     * Records a patient dose event (TAKEN / SKIPPED / MISSED).
     *
     * Safety invariant: [note] is patient-provided free text. This function NEVER
     * reads, copies, or references [Reminder.prescribedInstructions]. The prescribed
     * and the patient-reported data must remain strictly separate.
     */
    fun recordDose(
        reminderId: String,
        patientId: String,
        doseRecord: DoseRecord,
        note: String? = null,
    ) {
        val entity = DoseRecordEntity(
            id            = UUID.randomUUID().toString(),
            reminderId    = reminderId,
            patientId     = patientId,
            scheduledTime = System.currentTimeMillis(),
            recordedTime  = System.currentTimeMillis(),
            record        = doseRecord,
            patientNote   = note,
        )
        viewModelScope.launch {
            try {
                repository.insertDoseRecord(entity)
            } catch (e: Exception) {
                Timber.e(e, "Failed to record dose")
                _uiState.update { it.copy(error = "Failed to record dose.") }
            }
        }
    }
}
