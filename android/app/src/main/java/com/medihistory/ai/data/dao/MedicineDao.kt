package com.medihistory.ai.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.medihistory.ai.data.model.Medicine
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [Medicine] records.
 *
 * Security invariant: every query is scoped by [patientId].
 * A query that omits [patientId] must not be added here.
 * Never log medicine names, ingredients, or dosage details.
 */
@Dao
interface MedicineDao {

    /**
     * Emits the live list of medicines belonging to [patientId], ordered by name.
     * Results update automatically when the underlying data changes.
     */
    @Query("SELECT * FROM medicines WHERE patientId = :patientId ORDER BY medicineName ASC")
    fun observeByPatient(patientId: String): Flow<List<Medicine>>

    /**
     * Emits medicines matching [query] in either [Medicine.medicineName] or
     * [Medicine.activeIngredient] for [patientId].
     *
     * Security: scoped to [patientId] — never returns records from other patients.
     */
    @Query(
        """
        SELECT * FROM medicines
        WHERE patientId = :patientId
          AND (medicineName LIKE '%' || :query || '%'
               OR activeIngredient LIKE '%' || :query || '%')
        ORDER BY medicineName ASC
        """,
    )
    fun searchByPatient(patientId: String, query: String): Flow<List<Medicine>>

    /**
     * Returns the medicine with [id] that also belongs to [patientId], or null.
     *
     * Returning null (not an error) for a wrong [patientId] prevents leaking
     * the existence of records owned by another patient.
     */
    @Query("SELECT * FROM medicines WHERE id = :id AND patientId = :patientId LIMIT 1")
    suspend fun getById(id: String, patientId: String): Medicine?

    /** Inserts or replaces a batch of medicine records (idempotent upsert). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(medicines: List<Medicine>)

    /** Updates an existing medicine record. */
    @Update
    suspend fun update(medicine: Medicine)

    /**
     * Deletes a medicine record.
     *
     * Cascade from the Prescription FK handles bulk removal when a prescription is deleted.
     */
    @Delete
    suspend fun delete(medicine: Medicine)

    /**
     * Emits the distinct non-null active ingredients for [patientId].
     * Used for duplicate detection across ingredient identity.
     */
    @Query(
        """
        SELECT DISTINCT activeIngredient FROM medicines
        WHERE patientId = :patientId AND activeIngredient IS NOT NULL
        """,
    )
    fun observeActiveIngredients(patientId: String): Flow<List<String>>
}
