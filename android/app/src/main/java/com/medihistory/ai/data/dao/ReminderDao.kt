package com.medihistory.ai.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.medihistory.ai.data.model.Reminder
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [Reminder] records.
 *
 * Security invariant: every query is scoped by [patientId].
 * A query that omits [patientId] must not be added here.
 *
 * Safety invariant: this DAO stores and retrieves reminders exactly as provided.
 * It never infers, fabricates, or modifies prescribedInstructions.
 * Never log reminder content, medicine names, or patient data.
 */
@Dao
interface ReminderDao {
    /**
     * Emits the live list of reminders for [patientId], newest first.
     * The Flow updates automatically when the underlying data changes.
     *
     * Security: scoped to [patientId] — never returns records from other patients.
     */
    @Query(
        """
        SELECT * FROM reminders
        WHERE patientId = :patientId
        ORDER BY createdAt DESC
        """,
    )
    fun observeByPatient(patientId: String): Flow<List<Reminder>>

    /**
     * Returns the reminder with [id] that belongs to [patientId], or null.
     *
     * Returning null (not an error) for a wrong [patientId] prevents leaking the
     * existence of records owned by another patient.
     */
    @Query(
        """
        SELECT * FROM reminders
        WHERE id = :id AND patientId = :patientId
        LIMIT 1
        """,
    )
    suspend fun getById(
        id: String,
        patientId: String,
    ): Reminder?

    /**
     * Inserts a new reminder.
     * Uses IGNORE strategy — if a reminder with the same PK already exists, it is left unchanged.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(reminder: Reminder)

    /**
     * Updates an existing reminder (e.g. toggling [Reminder.isActive]).
     * Security: the reminder entity itself carries the patientId — the DAO enforces scope at
     * the entity level. Callers must not substitute reminder objects across patients.
     */
    @Update
    suspend fun update(reminder: Reminder)

    /**
     * Deletes the given reminder.
     * Patients may cancel a reminder at any time.
     */
    @Delete
    suspend fun delete(reminder: Reminder)
}
