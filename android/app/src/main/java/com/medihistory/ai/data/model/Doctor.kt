package com.medihistory.ai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a doctor in the patient's medical history.
 *
 * Security invariants:
 * - [patientId] is always set and used as a scope on every DAO query.
 * - No field here must be logged or transmitted without explicit patient consent.
 *
 * Registered in [MediHistoryDatabase] with the version 2 schema migration.
 */
@Entity(tableName = "doctors")
data class Doctor(
    @PrimaryKey
    val id: String,
    /** Scopes this record to a single patient. Never omit from queries. */
    val patientId: String,
    val name: String,
    val specialty: DoctorSpecialty,
    /** Required when [specialty] == [DoctorSpecialty.OTHER]; null otherwise. */
    val specialtyOther: String? = null,
    val clinicName: String? = null,
    /** Free-text contact notes (phone, address, etc.). */
    val contactNotes: String? = null,
    /** Unix epoch milliseconds — set at creation, never updated. */
    val createdAt: Long,
    /** Unix epoch milliseconds — updated on every [DoctorDao.update] call. */
    val updatedAt: Long,
)
