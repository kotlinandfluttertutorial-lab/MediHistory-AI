package com.medihistory.ai.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.medihistory.ai.data.model.DuplicateAlert
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [DuplicateAlert] records.
 *
 * Security invariant: every query is scoped by [patientId].
 * A query that omits [patientId] must not be added here.
 *
 * Medical safety invariant: this DAO stores and retrieves alerts exactly as provided.
 * It never infers, fabricates, or modifies alert content.
 * Never log medicine names, ingredients, or alert content.
 */
@Dao
interface DuplicateAlertDao {

    /**
     * Emits the live list of duplicate alerts for [patientId], newest first.
     * The Flow updates automatically when the underlying data changes.
     *
     * Security: scoped to [patientId] — never returns records from other patients.
     */
    @Query(
        """
        SELECT * FROM duplicate_alerts
        WHERE patientId = :patientId
        ORDER BY createdAt DESC
        """,
    )
    fun observeByPatient(patientId: String): Flow<List<DuplicateAlert>>

    /**
     * Inserts a new duplicate alert.
     * Uses IGNORE strategy — if the alert already exists it is left unchanged.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(alert: DuplicateAlert)

    /**
     * Marks the alert with [id] as reviewed, setting [reviewedAt] to [timestamp].
     *
     * Security: scoped to [patientId] to prevent cross-patient mutation.
     *
     * Medical safety: marking an alert as reviewed means the patient has seen it.
     * It does NOT indicate that the combination has been approved by a doctor.
     */
    @Query(
        """
        UPDATE duplicate_alerts
        SET isReviewed = 1, reviewedAt = :timestamp
        WHERE id = :id AND patientId = :patientId
        """,
    )
    suspend fun markReviewed(id: String, patientId: String, timestamp: Long)

    /**
     * Deletes all duplicate alerts for [patientId].
     * Called when the patient's data is cleared or their account is deleted.
     *
     * Security: scoped to [patientId] — never deletes records from other patients.
     */
    @Query("DELETE FROM duplicate_alerts WHERE patientId = :patientId")
    suspend fun deleteByPatient(patientId: String)

    /**
     * Returns the alert with [id] that belongs to [patientId], or null.
     *
     * Returning null (not an error) for a wrong [patientId] prevents leaking the
     * existence of records owned by another patient.
     */
    @Query(
        """
        SELECT * FROM duplicate_alerts
        WHERE id = :id AND patientId = :patientId
        LIMIT 1
        """,
    )
    suspend fun getById(id: String, patientId: String): DuplicateAlert?
}
