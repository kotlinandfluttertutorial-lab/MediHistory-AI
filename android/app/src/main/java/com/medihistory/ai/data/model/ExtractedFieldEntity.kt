package com.medihistory.ai.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.medihistory.ai.data.ocr.ReviewState

/**
 * Room entity persisting a single OCR-extracted field for a prescription document.
 *
 * Medical-safety invariants:
 * - [rawValue] null means the field was absent from the document. Never fabricate a value.
 * - [patientValue] is set only when the patient explicitly confirms or edits the field.
 * - [confidence] is in [0.0, 1.0].
 * - [reviewState] starts as [ReviewState.PENDING_REVIEW] and transitions only via the
 *   state machine enforced in ValidationUtils.
 * - [sourceReference] links to the page/region in the source document for traceability.
 *
 * Privacy: [rawValue] and [patientValue] must never be logged or transmitted without
 * explicit patient consent.
 *
 * Registered in [com.medihistory.ai.data.database.MediHistoryDatabase] at version 5
 * via MIGRATION_4_5.
 */
@Entity(
    tableName = "extracted_fields",
    indices = [Index("prescriptionId")],
    foreignKeys = [
        ForeignKey(
            entity = Prescription::class,
            parentColumns = ["id"],
            childColumns = ["prescriptionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ExtractedFieldEntity(
    @PrimaryKey
    val id: String,
    /** FK to [Prescription]. Cascades on delete. */
    val prescriptionId: String,
    /** Name of the field (e.g. "doctor_name", "medicine_name", "dose"). */
    val fieldName: String,
    /**
     * Raw OCR output for this field.
     * Null when the field was absent from the source document.
     * NEVER replaced with a fabricated or inferred value.
     */
    val rawValue: String?,
    /**
     * Value confirmed or edited by the patient during review.
     * Null until the patient saves a review decision for this field.
     */
    val patientValue: String?,
    /** OCR confidence in [0.0, 1.0]. */
    val confidence: Float,
    /** Review state — starts as [ReviewState.PENDING_REVIEW]. */
    val reviewState: ReviewState,
    /** Page or region reference in the source document, for traceability. */
    val sourceReference: String?,
    /** Unix epoch milliseconds — set at creation, never updated. */
    val createdAt: Long = System.currentTimeMillis(),
    /** Unix epoch milliseconds — updated on every review state change. */
    val updatedAt: Long = System.currentTimeMillis(),
)
