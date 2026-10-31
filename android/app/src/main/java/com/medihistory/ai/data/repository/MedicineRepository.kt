package com.medihistory.ai.data.repository

import com.medihistory.ai.data.model.Medicine
import kotlinx.coroutines.flow.Flow

/**
 * Contract for medicine data operations.
 *
 * Security invariant: every operation is scoped by [patientId].
 * No implementation may return records belonging to a different patient.
 *
 * Medical safety: this repository stores and retrieves medicines exactly as provided.
 * It never infers, fabricates, or modifies field values.
 */
interface MedicineRepository {
    /**
     * Emits the live list of medicines for [patientId], ordered by name.
     * The Flow updates automatically when the underlying data changes.
     */
    fun observeByPatient(patientId: String): Flow<List<Medicine>>

    /**
     * Emits medicines whose [Medicine.medicineName] or [Medicine.activeIngredient]
     * contain [query] (case-insensitive) for [patientId].
     *
     * Returns the full unfiltered list when [query] is blank.
     */
    fun searchByPatient(
        patientId: String,
        query: String,
    ): Flow<List<Medicine>>

    /**
     * Returns the medicine with [id] that belongs to [patientId], or null.
     * Returns null (not an error) when [id] belongs to another patient.
     */
    suspend fun getById(
        id: String,
        patientId: String,
    ): Medicine?

    /** Inserts or replaces a batch of medicine records. */
    suspend fun insertAll(medicines: List<Medicine>)

    /** Persists changes to an existing medicine record. */
    suspend fun update(medicine: Medicine)

    /**
     * Removes a medicine record from storage.
     * The parent prescription record is not affected.
     */
    suspend fun delete(medicine: Medicine)

    /**
     * Emits the distinct non-null active ingredients for [patientId].
     * Used by the duplicate detection layer.
     */
    fun observeActiveIngredients(patientId: String): Flow<List<String>>
}
