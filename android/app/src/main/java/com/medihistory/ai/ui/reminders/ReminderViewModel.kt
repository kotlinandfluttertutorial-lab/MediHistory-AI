package com.medihistory.ai.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medihistory.ai.data.model.DoseRecord
import com.medihistory.ai.data.model.DoseRecordEntity
import com.medihistory.ai.data.model.Reminder
import com.medihistory.ai.data.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.UUID
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for [ReminderScreen].
 *
 * Exposes a [StateFlow] of [ReminderUiState] driven by [ReminderRepository].
 *
 * Safety invariants enforced here:
 * - [createReminder] requires [prescribedInstructions] to be passed explicitly by the caller.
 *   The ViewModel does NOT read or auto-fill from any OCR field. The caller is responsible
 *   for only passing text from a REVIEWED extracted field (ReviewState.REVIEWED).
 * - [recordDose] stores a [DoseRecordEntity] whose [patientNote] is patient-provided text.
 *   It NEVER copies or reads [Reminder.prescribedInstructions].
 *
 * Privacy: never log patientId, medicine names, or reminder content.
 */
@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val repository: ReminderRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReminderUiState())
    val uiState: StateFlow<ReminderUiState> = _uiState.asStateFlow()

    /**
     * Starts observing reminders for [patientId].
     *
     * Safe to call multiple times (subsequent calls cancel the previous subscription).
     */
    fun loadReminders(patientId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.observeByPatient(patientId)
                .catch { e ->
                    Timber.e(e, "Failed to load reminders")
                    e.cause?.let { Timber.e(it, "Caused by: ${it.message}") }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load reminders.",
                        )
                    }
                }
                .collect { reminders ->
                    _uiState.update {
                        it.copy(
                            reminders = reminders,
                            isLoading = false,
                            error = null,
                        )
                    }
                }
        }
    }

    /**
     * Creates a new opt-in reminder and persists it.
     *
     * Safety: [prescribedInstructions] must originate from a REVIEWED OCR extracted field.
     * The ViewModel accepts it as a parameter — it is the caller's responsibility to only
     * pass reviewed data. Passing null is valid (prescribed instructions are optional).
     *
     * Never auto-fills [prescribedInstructions] from an unreviewed field.
     *
     * @param patientId             Patient this reminder belongs to.
     * @param medicineId            Medicine this reminder is for.
     * @param medicineName          Display name for the medicine (denormalized).
     * @param prescribedInstructions Instructions from a REVIEWED OCR field, or null.
     * @param reminderTime          Time of day for the notification.
     */
    fun createReminder(
        patientId: String,
        medicineId: String,
        medicineName: String,
        prescribedInstructions: String?,
        reminderTime: LocalTime,
    ) {
        val reminder = Reminder(
            id = UUID.randomUUID().toString(),
            patientId = patientId,
            medicineId = medicineId,
            medicineName = medicineName,
            prescribedInstructions = prescribedInstructions,
            reminderTime = reminderTime,
            isActive = true,
        )
        viewModelScope.launch {
            try {
                repository.insert(reminder)
            } catch (e: Exception) {
                Timber.e(e, "Failed to create reminder")
                e.cause?.let { Timber.e(it, "Caused by: ${it.message}") }
                _uiState.update { it.copy(error = e.message ?: "Failed to create reminder.") }
            }
        }
    }

    /**
     * Deletes the given reminder.
     * Patients may cancel a reminder at any time.
     */
    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            try {
                repository.delete(reminder)
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete reminder")
                e.cause?.let { Timber.e(it, "Caused by: ${it.message}") }
                _uiState.update { it.copy(error = e.message ?: "Failed to delete reminder.") }
            }
        }
    }

    /**
     * Records a patient dose event (TAKEN / SKIPPED / MISSED).
     *
     * Safety invariant: [patientNote] is patient-provided free text.
     * This function NEVER reads, copies, or references [Reminder.prescribedInstructions].
     * The prescribed and the patient-reported data must remain strictly separate.
     *
     * @param reminderId    ID of the reminder this dose belongs to.
     * @param patientId     Patient scope (denormalized on the dose record).
     * @param doseRecord    TAKEN, SKIPPED, or MISSED.
     * @param note          Optional patient-provided note. Must NOT contain prescription text.
     */
    fun recordDose(
        reminderId: String,
        patientId: String,
        doseRecord: DoseRecord,
        note: String? = null,
    ) {
        val entity = DoseRecordEntity(
            id = UUID.randomUUID().toString(),
            reminderId = reminderId,
            patientId = patientId,
            scheduledTime = System.currentTimeMillis(),
            recordedTime = System.currentTimeMillis(),
            record = doseRecord,
            patientNote = note,
        )
        viewModelScope.launch {
            try {
                repository.insertDoseRecord(entity)
            } catch (e: Exception) {
                Timber.e(e, "Failed to record dose")
                e.cause?.let { Timber.e(it, "Caused by: ${it.message}") }
                _uiState.update { it.copy(error = e.message ?: "Failed to record dose.") }
            }
        }
    }
}
