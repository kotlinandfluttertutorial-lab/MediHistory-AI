package com.medihistory.ai.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a pending sync operation.
 *
 * Each time a local entity (Prescription, Doctor, Medicine) is created or updated,
 * a row is inserted here. [SyncWorker] reads the queue, calls the backend upsert
 * endpoints, and deletes successful rows.
 *
 * Using a queue table rather than an `isDirty` flag on every entity:
 * - Avoids complex migrations on multiple tables
 * - Allows retry counting and backoff per operation
 * - Makes the sync backlog observable in UI if needed
 *
 * Privacy: [entityId] is an internal UUID — it must never be logged.
 * Registered in [MediHistoryDatabase] v9 (MIGRATION_8_9).
 */
@Entity(
    tableName = "sync_queue",
    indices = [Index("patientId")],
)
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Type of entity: "PRESCRIPTION" | "DOCTOR" | "MEDICINE" */
    val entityType: String,
    val entityId: String,
    val patientId: String,
    /** Operation type: currently always "UPSERT" */
    val operation: String = "UPSERT",
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
)
