package com.medihistory.ai.data.repository

import com.medihistory.ai.data.model.DoseRecordEntity
import com.medihistory.ai.data.model.Reminder
import kotlinx.coroutines.flow.Flow

/**
 * Contract for reminder and dose record data operations.
 *
 * Security invariant: every operation is scoped by [patientId].
 * No implementation may return records belonging to a different patient.
 *
 * Medical safety invariants:
 * - Reminders are opt-in only. This repository never creates a reminder implicitly.
 * - [Reminder.prescribedInstructions] is stored as provided — never inferred or fabricated.
 * - Dose records ([DoseRecordEntity]) are patient-reported observations and are kept strictly
 *   separate from [Reminder.prescribedInstructions].
 * - A dose record's [patientNote] must NEVER copy or include prescribed instruction text.
 */
interface ReminderRepository {
    /**
     * Emits the live list of reminders for [patientId], newest first.
     * The Flow updates automatically when the underlying data changes.
     */
    fun observeByPatient(patientId: String): Flow<List<Reminder>>

    /**
     * Returns the reminder with [id] belonging to [patientId], or null.
     * Returns null (not an error) when [id] belongs to another patient.
     */
    suspend fun getById(
        id: String,
        patientId: String,
    ): Reminder?

    /**
     * Inserts a new reminder.
     * Ignored if a reminder with the same primary key already exists.
     *
     * Callers must only pass [Reminder.prescribedInstructions] sourced from a REVIEWED OCR field.
     */
    suspend fun insert(reminder: Reminder)

    /**
     * Updates an existing reminder (e.g. toggling [Reminder.isActive]).
     * Security: the reminder entity carries [Reminder.patientId] — callers must not
     * swap reminder objects across patients.
     */
    suspend fun update(reminder: Reminder)

    /**
     * Deletes the given reminder. Patients may cancel a reminder at any time.
     */
    suspend fun delete(reminder: Reminder)

    /**
     * Emits the live list of dose records for the given [reminderId], newest first.
     */
    fun observeDoseRecords(reminderId: String): Flow<List<DoseRecordEntity>>

    /**
     * Records a dose event (TAKEN / SKIPPED / MISSED) for a reminder.
     *
     * Medical safety: the [patientNote] is patient free text and must NEVER contain
     * or copy the [Reminder.prescribedInstructions] text. The repository stores it as-is.
     */
    suspend fun insertDoseRecord(doseRecord: DoseRecordEntity)

    /**
     * Returns all dose records for [patientId] whose scheduled time falls within
     * [[from], [to]] (both inclusive, epoch milliseconds).
     *
     * Security: scoped to [patientId] — never returns records from other patients.
     */
    suspend fun getDoseRecordsByDateRange(
        patientId: String,
        from: Long,
        to: Long,
    ): List<DoseRecordEntity>
}
