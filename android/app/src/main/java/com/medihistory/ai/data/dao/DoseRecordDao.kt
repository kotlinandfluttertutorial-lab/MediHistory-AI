package com.medihistory.ai.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.medihistory.ai.data.model.DoseRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [DoseRecordEntity] records.
 *
 * Security invariant: every query is scoped by [patientId] or [reminderId].
 * A query that omits patient scope must not be added here.
 *
 * Medical safety: dose records are patient-reported only. This DAO stores and retrieves
 * them exactly as provided. It never copies or infers prescribed instructions.
 * Never log dose record content or patient data.
 */
@Dao
interface DoseRecordDao {

    /**
     * Emits the live list of dose records for [reminderId], newest scheduled time first.
     * The Flow updates automatically when the underlying data changes.
     */
    @Query(
        """
        SELECT * FROM dose_records
        WHERE reminderId = :reminderId
        ORDER BY scheduledTime DESC
        """,
    )
    fun observeByReminder(reminderId: String): Flow<List<DoseRecordEntity>>

    /**
     * Inserts a new dose record.
     * Uses IGNORE strategy — if a record with the same PK already exists, it is left unchanged.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(doseRecord: DoseRecordEntity)

    /**
     * Returns all dose records for [patientId] whose [DoseRecordEntity.scheduledTime] falls
     * within [[from], [to]] (both inclusive, epoch milliseconds).
     *
     * Security: scoped to [patientId] — never returns records from other patients.
     * Used for adherence history display.
     */
    @Query(
        """
        SELECT * FROM dose_records
        WHERE patientId = :patientId
          AND scheduledTime >= :from
          AND scheduledTime <= :to
        ORDER BY scheduledTime DESC
        """,
    )
    suspend fun getByDateRange(patientId: String, from: Long, to: Long): List<DoseRecordEntity>
}
