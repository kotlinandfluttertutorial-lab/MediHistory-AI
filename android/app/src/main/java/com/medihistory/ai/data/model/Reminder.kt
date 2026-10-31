package com.medihistory.ai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalTime

/**
 * Represents the status of a single dose event as recorded by the patient.
 *
 * Medical safety invariant: this is a patient-reported record, NOT a clinical record.
 * It must never be presented as a verified medical fact or compared to prescriber intent.
 *
 * Values:
 * - [TAKEN]   — patient reports they took the dose
 * - [SKIPPED] — patient deliberately skipped the dose
 * - [MISSED]  — dose was missed (not recorded in time)
 */
enum class DoseRecord { TAKEN, SKIPPED, MISSED }

/**
 * Room entity representing a single medication reminder.
 *
 * Privacy and safety invariants:
 * - Reminders are opt-in only — never created without an explicit patient action.
 * - [prescribedInstructions] must come from a REVIEWED OCR field. The caller (ViewModel)
 *   is responsible for enforcing this. It is stored here for display only.
 * - [prescribedInstructions] is NEVER shown as a dose record or mixed with patient-reported data.
 * - No reminder field may be logged in Logcat.
 *
 * Registered in [MediHistoryDatabase] v7 (MIGRATION_6_7).
 */
@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey
    val id: String,
    /** Patient scope — included on every DAO query without a JOIN. */
    val patientId: String,
    /** FK to the medicine this reminder is for. */
    val medicineId: String,
    /**
     * Denormalized medicine name for display purposes.
     * Avoids a JOIN on the medicines table in the reminders list.
     */
    val medicineName: String,
    /**
     * Prescribed instructions copied verbatim from a REVIEWED OCR extracted field.
     * Null when not provided by the patient.
     *
     * Medical safety: this is ALWAYS labelled "Prescribed:" in the UI and is
     * NEVER shown as a patient-reported dose record.
     * The app must NEVER fabricate or infer this value.
     */
    val prescribedInstructions: String?,
    /** Time of day the notification is scheduled. Stored as seconds-of-day via [Converters]. */
    val reminderTime: LocalTime,
    /**
     * Whether this reminder is active (i.e. notifications are enabled).
     * Defaults to true. Patients can cancel at any time.
     */
    val isActive: Boolean = true,
    /** Unix epoch milliseconds — set at creation, never updated. */
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Room entity representing a single dose event recorded by the patient.
 *
 * Privacy and safety invariants:
 * - [record] is patient-reported — it is NEVER implied to match prescribed instructions.
 * - [patientNote] is free text entered by the patient. It must NEVER copy [Reminder.prescribedInstructions].
 * - [scheduledTime] is when the dose was scheduled; [recordedTime] is when the patient recorded the action.
 * - No field here may be logged in Logcat.
 *
 * Registered in [MediHistoryDatabase] v7 (MIGRATION_6_7).
 */
@Entity(tableName = "dose_records")
data class DoseRecordEntity(
    @PrimaryKey
    val id: String,
    /** FK to the [Reminder] this dose belongs to. */
    val reminderId: String,
    /** Denormalized patient scope — used on every DAO query without a JOIN. */
    val patientId: String,
    /** Epoch milliseconds when the dose was scheduled. */
    val scheduledTime: Long,
    /** Epoch milliseconds when the patient recorded the action. Null if not yet recorded. */
    val recordedTime: Long?,
    /**
     * Patient-reported dose outcome: TAKEN, SKIPPED, or MISSED.
     * This is patient-reported — it does NOT imply clinical compliance.
     */
    val record: DoseRecord,
    /**
     * Optional free-text note entered by the patient.
     * Must NEVER contain or copy prescription text from any source.
     */
    val patientNote: String? = null,
)
