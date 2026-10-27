package com.medihistory.ai.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Room entity representing a single medicine entry in the patient's medication history.
 *
 * Each medicine is linked to the prescription it was extracted from via [prescriptionId].
 * The [patientId] column is denormalized to allow efficient patient-scoped queries
 * without a JOIN on the prescriptions table.
 *
 * Security invariants:
 * - [patientId] is always set and must be included on every DAO query.
 * - A medicine record belonging to one patient must never be returned when queried
 *   with a different patientId.
 * - No field here must be logged or transmitted without explicit patient consent.
 *
 * Medical safety invariants:
 * - [activeIngredient] is null when it cannot be reliably identified from OCR output.
 *   The app must NEVER fabricate or infer a missing ingredient value.
 * - [dosageInstructions] is null when not present in the source document.
 *   The app must NEVER infer or fabricate missing dose information.
 * - [status] defaults to [MedicineStatus.UNKNOWN]; it is always patient-assigned.
 *
 * Registered in [MediHistoryDatabase] v5 (MIGRATION_4_5).
 */
@Entity(
    tableName = "medicines",
    foreignKeys = [
        ForeignKey(
            entity = Prescription::class,
            parentColumns = ["id"],
            childColumns = ["prescriptionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["patientId"]),
        Index(value = ["prescriptionId"]),
    ],
)
data class Medicine(
    @PrimaryKey
    val id: String,

    /** FK to the prescription document this medicine was extracted from. */
    val prescriptionId: String,

    /** Denormalized patient scope — used on every DAO query without a JOIN. */
    val patientId: String,

    /** Display name as it appears on the prescription. */
    val medicineName: String,

    /**
     * Verified active ingredient. Null when OCR output is insufficient to identify it.
     * Never fabricated or inferred.
     */
    val activeIngredient: String? = null,

    /** Brand name, if distinct from the active ingredient. */
    val brandName: String? = null,

    /** Strength (e.g. "500mg", "10mg/5ml"). Null if absent from source document. */
    val strength: String? = null,

    /** Formulation (e.g. "tablet", "capsule", "syrup"). Null if absent. */
    val formulation: String? = null,

    /**
     * Dosage instructions as written on the prescription.
     * Null if absent. Never inferred or fabricated.
     */
    val dosageInstructions: String? = null,

    /** FK to the doctor who prescribed this medicine. Null if not recorded. */
    val prescribingDoctorId: String? = null,

    /** Date on the source prescription document. Null if absent. */
    val prescriptionDate: LocalDate? = null,

    /**
     * Patient-assigned status. Defaults to [MedicineStatus.UNKNOWN].
     * The app never sets this to CURRENT or PAST without an explicit patient action.
     */
    val status: MedicineStatus = MedicineStatus.UNKNOWN,

    /** Unix epoch milliseconds — set at creation, never updated. */
    val createdAt: Long = System.currentTimeMillis(),

    /** Unix epoch milliseconds — updated on every write. */
    val updatedAt: Long = System.currentTimeMillis(),
)
