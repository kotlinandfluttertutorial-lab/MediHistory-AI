package com.medihistory.ai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a potential duplicate ingredient alert.
 *
 * Medical safety invariants:
 * - Alerts are always labelled "Potential duplicate" — NEVER "Duplicate medication" or any
 *   clinical term.
 * - [matchType] is EXACT or PARTIAL — never a clinical equivalence claim.
 * - Duplicate alerts are informational only; they do NOT block the patient from recording
 *   medicines.
 * - A [combinationNote] is present when one or both medicines is a combination product.
 *   The note names both medicines and the matching ingredient but makes NO clinical claim.
 * - The app NEVER indicates that a drug combination is safe or clinically interchangeable.
 *
 * Security invariants:
 * - [patientId] is denormalized so every DAO query can be scoped without a JOIN.
 * - A record belonging to one patient must never be returned when queried with a different
 *   [patientId].
 *
 * Registered in [MediHistoryDatabase] v6 (MIGRATION_5_6).
 */
@Entity(tableName = "duplicate_alerts")
data class DuplicateAlert(
    @PrimaryKey
    val id: String,

    /** Denormalized patient scope — included on every DAO query. */
    val patientId: String,

    /** ID of the first medicine in the potential duplicate pair. */
    val medicineAId: String,

    /** ID of the second medicine in the potential duplicate pair. */
    val medicineBId: String,

    /**
     * Canonical normalised active ingredient that triggered this alert.
     * Derived from [IngredientNormalizer] — not fabricated or inferred.
     */
    val matchingIngredient: String,

    /**
     * Whether the match is an exact ingredient match or a partial match
     * (one ingredient of a combination product overlaps).
     */
    val matchType: MatchType,

    /**
     * Confidence score between 0.0 and 1.0.
     * EXACT matches score ~0.95; PARTIAL / combination matches score ~0.70.
     * Lower confidence = more uncertainty — never implies clinical safety.
     */
    val confidence: Float,

    /**
     * Whether the patient has seen this alert.
     * Reviewing an alert does NOT imply clinical approval of the combination.
     */
    val isReviewed: Boolean = false,

    /** Unix epoch milliseconds when the patient marked this alert reviewed. Null if unseen. */
    val reviewedAt: Long? = null,

    /**
     * Present when one or both medicines is a combination product.
     * Example: "Both medicines contain metoprolol succinate. One or both may be a combination
     * product."
     *
     * This note is informational. It NEVER implies that the combination is safe or approved.
     */
    val combinationNote: String? = null,

    /** Unix epoch milliseconds when this alert was first created. */
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Describes how two medicines matched during duplicate detection.
 *
 * EXACT  — the normalised active ingredients are identical (including synonym equivalence).
 * PARTIAL — one ingredient of a combination product (A+B) matches a single-ingredient medicine.
 *
 * Neither value implies clinical interchangeability or that the combination is safe.
 */
enum class MatchType { EXACT, PARTIAL }
