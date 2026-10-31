package com.medihistory.ai.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.medihistory.ai.data.model.ExtractedFieldEntity
import com.medihistory.ai.data.ocr.ReviewState
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [ExtractedFieldEntity] records.
 *
 * Security invariants:
 * - All queries are scoped to a [prescriptionId]; cross-prescription access is not possible
 *   through this DAO.
 * - [rawValue] and [patientValue] must never be logged — callers are responsible.
 */
@Dao
interface ExtractedFieldDao {
    /**
     * Emits the live list of extracted fields for a given prescription.
     * Updates automatically when the underlying data changes.
     */
    @Query("SELECT * FROM extracted_fields WHERE prescriptionId = :prescriptionId")
    fun observeByPrescription(prescriptionId: String): Flow<List<ExtractedFieldEntity>>

    /**
     * Inserts or replaces a batch of extracted fields.
     * Used by [com.medihistory.ai.worker.OcrWorker] to persist OCR results.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(fields: List<ExtractedFieldEntity>)

    /**
     * Updates the review decision for a single field.
     *
     * [patientValue] is set when the patient confirms or edits the raw value.
     * [state] must follow the allowed transitions in the review state machine.
     * [ts] defaults to the current wall-clock time and is recorded as [updatedAt].
     */
    @Query(
        """
        UPDATE extracted_fields
        SET reviewState = :state, patientValue = :patientValue, updatedAt = :ts
        WHERE id = :id
        """,
    )
    suspend fun updateReview(
        id: String,
        state: ReviewState,
        patientValue: String?,
        ts: Long = System.currentTimeMillis(),
    )

    /**
     * Deletes all extracted fields for a prescription.
     * The cascade defined on the foreign key handles this automatically when a
     * [com.medihistory.ai.data.model.Prescription] is deleted via [PrescriptionDao.delete],
     * but this method allows explicit cleanup when needed.
     */
    @Query("DELETE FROM extracted_fields WHERE prescriptionId = :prescriptionId")
    suspend fun deleteByPrescription(prescriptionId: String)
}
