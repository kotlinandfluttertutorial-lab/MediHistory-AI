package com.medihistory.ai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Room entity representing a prescription in the patient's medical history.
 *
 * Security invariants:
 * - [patientId] is always set and used as a scope on every DAO query.
 * - [documentPath] points to app-private storage only (getFilesDir / getExternalFilesDir).
 * - [documentHashSha256] is nullable here — it is set after the file is copied to
 *   app-private storage (MA-31/32). Never fabricate or infer this value.
 * - No field here must be logged or transmitted without explicit patient consent.
 *
 * Registered in [MediHistoryDatabase] with the version 4 schema migration (MIGRATION_3_4).
 */
@Entity(tableName = "prescriptions")
data class Prescription(
    @PrimaryKey
    val id: String,
    /** Scopes this record to a single patient. Never omit from queries. */
    val patientId: String,
    /** Optional FK to doctors table. Null if no doctor was recorded. */
    val doctorId: String? = null,
    /** Date shown on the prescription document, if present. */
    val prescriptionDate: LocalDate? = null,
    /** Free-text notes added by the patient. */
    val notes: String? = null,
    /**
     * Path to the document in app-private storage.
     * Must never point to shared external storage.
     */
    val documentPath: String? = null,
    /** MIME type of the stored document (e.g. "image/jpeg", "application/pdf"). */
    val documentMimeType: String? = null,
    /**
     * SHA-256 hex digest of the stored document file.
     * Null until the file has been copied to app-private storage (MA-31/32).
     * Never infer or fabricate this value.
     */
    val documentHashSha256: String? = null,
    /** Current OCR processing and review state. */
    val ocrStatus: OcrStatus = OcrStatus.PENDING,
    /** Unix epoch milliseconds — set at creation, never updated. */
    val createdAt: Long = System.currentTimeMillis(),
    /** Unix epoch milliseconds — updated on every write. */
    val updatedAt: Long = System.currentTimeMillis(),
)
