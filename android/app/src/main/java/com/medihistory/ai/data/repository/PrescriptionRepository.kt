package com.medihistory.ai.data.repository

import com.medihistory.ai.data.model.OcrStatus
import com.medihistory.ai.data.model.Prescription
import kotlinx.coroutines.flow.Flow

/**
 * Contract for prescription data operations.
 *
 * Security invariant: every operation is scoped by [patientId].
 * No implementation may return records belonging to a different patient.
 * Never log prescription text, document content, or document paths.
 */
interface PrescriptionRepository {

    /**
     * Emits the live list of prescriptions for [patientId], ordered by
     * prescription date descending then creation time descending.
     * The Flow updates automatically when the underlying data changes.
     */
    fun observeByPatient(patientId: String): Flow<List<Prescription>>

    /**
     * Returns the prescription with [id] that belongs to [patientId], or null.
     * Returns null (not an error) when [id] belongs to another patient, preventing
     * cross-patient data exposure (equivalent to HTTP 404 at the data layer).
     */
    suspend fun getById(id: String, patientId: String): Prescription?

    /** Persists a new [prescription] record. */
    suspend fun insert(prescription: Prescription)

    /** Persists changes to an existing [prescription] record. */
    suspend fun update(prescription: Prescription)

    /**
     * Removes [prescription] from storage.
     *
     * The associated document file is not deleted here — file cleanup must be
     * performed separately by the caller (MA-31/32).
     */
    suspend fun delete(prescription: Prescription)

    /**
     * Updates only the OCR status for the prescription with [id] that belongs to [patientId].
     * Does not touch other fields or document content.
     * Security invariant: scoped to [patientId] — does not update records of other patients.
     */
    suspend fun updateOcrStatus(id: String, patientId: String, status: OcrStatus, updatedAt: Long = System.currentTimeMillis())
}
