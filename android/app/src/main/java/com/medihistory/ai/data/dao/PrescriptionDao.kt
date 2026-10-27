package com.medihistory.ai.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.medihistory.ai.data.model.OcrStatus
import com.medihistory.ai.data.model.Prescription
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [Prescription] records.
 *
 * Security invariant: every query is scoped by [patientId].
 * A query that omits [patientId] must not be added here.
 * Never log prescription text, document content, or document paths.
 */
@Dao
interface PrescriptionDao {

    /**
     * Emits the live list of prescriptions belonging to [patientId], ordered by
     * prescription date descending, then by creation time descending.
     * The Flow updates automatically when the underlying data changes.
     */
    @Query(
        """
        SELECT * FROM prescriptions
        WHERE patientId = :patientId
        ORDER BY prescriptionDate DESC, createdAt DESC
        """
    )
    fun observeByPatient(patientId: String): Flow<List<Prescription>>

    /**
     * Returns the prescription with [id] that also belongs to [patientId], or null.
     *
     * Returns null (not an error) when [id] belongs to another patient, preventing
     * cross-patient data exposure.
     */
    @Query("SELECT * FROM prescriptions WHERE id = :id AND patientId = :patientId LIMIT 1")
    suspend fun getById(id: String, patientId: String): Prescription?

    /** Inserts a new prescription record. Replaces on conflict (idempotent upsert). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prescription: Prescription)

    /** Updates an existing prescription record. */
    @Update
    suspend fun update(prescription: Prescription)

    /**
     * Deletes a prescription record.
     *
     * The document file itself is not deleted by this call — callers must handle
     * file cleanup separately (MA-31/32).
     */
    @Delete
    suspend fun delete(prescription: Prescription)

    /**
     * Updates only the [status] and [updatedAt] columns for the prescription with [id]
     * that belongs to [patientId].
     *
     * Security invariant: scoped to [patientId] — will not update records of other patients.
     * Used by background OCR workers to advance OCR state without loading the full entity.
     * Does not expose document contents or patient data.
     */
    @Query(
        """
        UPDATE prescriptions
        SET ocrStatus = :status, updatedAt = :updatedAt
        WHERE id = :id AND patientId = :patientId
        """
    )
    suspend fun updateOcrStatus(
        id: String,
        patientId: String,
        status: OcrStatus,
        updatedAt: Long = System.currentTimeMillis(),
    )
}
