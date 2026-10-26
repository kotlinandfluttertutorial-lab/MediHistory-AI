package com.medihistory.ai.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.medihistory.ai.data.model.Doctor
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [Doctor] records.
 *
 * Security invariant: every query is scoped by [patientId].
 * A query that omits [patientId] must not be added here.
 */
@Dao
interface DoctorDao {

    /**
     * Emits the full list of doctors belonging to [patientId], updated reactively.
     * Results are ordered by name for stable list presentation.
     */
    @Query("SELECT * FROM doctors WHERE patientId = :patientId ORDER BY name ASC")
    fun observeByPatient(patientId: String): Flow<List<Doctor>>

    /**
     * Returns the doctor with [doctorId] that also belongs to [patientId],
     * or null if no such record exists.
     *
     * Returning null (instead of throwing) for a wrong [patientId] prevents
     * leaking the existence of records owned by another patient.
     */
    @Query("SELECT * FROM doctors WHERE id = :doctorId AND patientId = :patientId LIMIT 1")
    suspend fun getById(doctorId: String, patientId: String): Doctor?

    /** Inserts a new doctor record. Replaces on conflict (idempotent upsert). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(doctor: Doctor)

    /** Updates an existing doctor record. */
    @Update
    suspend fun update(doctor: Doctor)

    /**
     * Deletes a doctor record.
     *
     * Prescriptions linked to this doctor will show "Doctor removed" in the UI —
     * the prescription records themselves are not deleted.
     */
    @Delete
    suspend fun delete(doctor: Doctor)
}
