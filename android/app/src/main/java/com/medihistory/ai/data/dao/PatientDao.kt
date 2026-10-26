package com.medihistory.ai.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.medihistory.ai.data.model.Patient
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [Patient] records.
 *
 * Security invariant: [getById] is scoped by [patientId] so that a caller
 * cannot accidentally retrieve another patient's record.
 * [observeAll] is intentionally available for local single-user use; it must
 * only be invoked from UI contexts where the authenticated user is known.
 */
@Dao
interface PatientDao {

    /**
     * Returns the patient with [patientId], or null if no such record exists.
     * Returning null (not throwing) prevents leaking record existence.
     */
    @Query("SELECT * FROM patients WHERE id = :patientId LIMIT 1")
    suspend fun getById(patientId: String): Patient?

    /** Inserts a new patient record. Replaces on conflict (idempotent upsert). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(patient: Patient)

    /** Updates an existing patient record. */
    @Update
    suspend fun update(patient: Patient)

    /** Deletes a patient record and all its owned data must be removed by the caller. */
    @Delete
    suspend fun delete(patient: Patient)

    /**
     * Emits the full list of patients ordered by name, updated reactively.
     * Single-user apps will always see at most one entry; the list form
     * supports multi-profile expansion without a schema change.
     */
    @Query("SELECT * FROM patients ORDER BY name ASC")
    fun observeAll(): Flow<List<Patient>>
}
