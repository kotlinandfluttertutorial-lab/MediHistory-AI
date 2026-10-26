package com.medihistory.ai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Room entity representing a patient (the app owner / primary user).
 *
 * Security invariants:
 * - [id] is a UUID assigned at creation; it is the scope key for all related records.
 * - No field here must be logged or transmitted without explicit patient consent.
 * - [allergies] is stored as a comma-escaped string via [com.medihistory.ai.data.database.Converters].
 */
@Entity(tableName = "patients")
data class Patient(
    @PrimaryKey
    val id: String,
    val name: String,
    /** Optional date of birth; stored as epoch day via [Converters]. */
    val dateOfBirth: LocalDate? = null,
    /** Optional ABO/Rh blood group string (e.g. "A+", "O-"). */
    val bloodGroup: String? = null,
    /**
     * List of known allergens; stored as a comma-escaped string via [Converters].
     * Nullable at the Room level because [Converters.stringToStringList] returns null for an
     * empty/null column value; callers should use [allergiesSafe] or coerce to emptyList().
     */
    val allergies: List<String>? = emptyList(),
    /** Unix epoch milliseconds — set at creation, never changed. */
    val createdAt: Long,
    /** Unix epoch milliseconds — updated on every save. */
    val updatedAt: Long,
)
