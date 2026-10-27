package com.medihistory.ai.data.repository

import com.medihistory.ai.data.model.DuplicateAlert
import kotlinx.coroutines.flow.Flow

/**
 * Contract for duplicate alert data operations.
 *
 * Security invariant: every operation is scoped by [patientId].
 * No implementation may return records belonging to a different patient.
 *
 * Medical safety: this repository stores and retrieves alerts exactly as provided.
 * It never infers, fabricates, or modifies alert content.
 * Marking an alert as reviewed means the patient has seen it — it does NOT imply
 * that the combination has been approved by a doctor.
 */
interface DuplicateAlertRepository {

    /**
     * Emits the live list of duplicate alerts for [patientId], newest first.
     * The Flow updates automatically when the underlying data changes.
     */
    fun observeByPatient(patientId: String): Flow<List<DuplicateAlert>>

    /**
     * Inserts a new duplicate alert.
     * Ignored if an alert with the same primary key already exists.
     */
    suspend fun insert(alert: DuplicateAlert)

    /**
     * Marks the alert with [id] as reviewed at [timestamp].
     *
     * Marking as reviewed means the patient has seen this alert.
     * It does NOT mean the combination has been approved by a doctor.
     *
     * Security: operation is scoped to [patientId].
     */
    suspend fun markReviewed(id: String, patientId: String, timestamp: Long)

    /**
     * Deletes all duplicate alerts for [patientId].
     * Called when the patient's data is cleared or their account is deleted.
     */
    suspend fun deleteByPatient(patientId: String)

    /**
     * Returns the alert with [id] that belongs to [patientId], or null.
     * Returns null (not an error) when [id] belongs to another patient.
     */
    suspend fun getById(id: String, patientId: String): DuplicateAlert?
}
