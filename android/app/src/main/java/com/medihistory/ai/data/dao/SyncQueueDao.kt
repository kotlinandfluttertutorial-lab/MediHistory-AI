package com.medihistory.ai.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.medihistory.ai.data.model.SyncQueueEntity

/**
 * Data access object for the sync queue.
 * Privacy: entity IDs are never logged.
 */
@Dao
interface SyncQueueDao {

    /** Add an entity to the sync queue. Ignores duplicate (same entityType + entityId). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(entry: SyncQueueEntity)

    /** Return all pending entries for [patientId], oldest first. */
    @Query("SELECT * FROM sync_queue WHERE patientId = :patientId ORDER BY createdAt ASC LIMIT 50")
    suspend fun getPending(patientId: String): List<SyncQueueEntity>

    /** Delete a successfully synced entry. */
    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun delete(id: Long)

    /** Increment retry count for a failed entry. */
    @Query("UPDATE sync_queue SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetry(id: Long)

    /** Remove all entries that have exceeded the max retry limit. */
    @Query("DELETE FROM sync_queue WHERE retryCount >= :maxRetries")
    suspend fun dropExhausted(maxRetries: Int = 5)

    /** Delete all queue entries for a patient (used on account deletion). */
    @Query("DELETE FROM sync_queue WHERE patientId = :patientId")
    suspend fun deleteByPatient(patientId: String)
}
