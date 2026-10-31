package com.medihistory.ai.data.repository

import com.medihistory.ai.data.model.Doctor
import kotlinx.coroutines.flow.Flow

/**
 * Contract for doctor data operations.
 *
 * Security invariant: every operation is scoped by [patientId].
 * No implementation may return records belonging to a different patient.
 */
interface DoctorRepository {
    /**
     * Emits the live list of doctors for [patientId].
     * The Flow updates automatically when the underlying data changes.
     */
    fun observeByPatient(patientId: String): Flow<List<Doctor>>

    /**
     * Returns the doctor with [doctorId] that belongs to [patientId], or null.
     * Returns null (not an error) when [doctorId] belongs to another patient.
     */
    suspend fun getById(
        doctorId: String,
        patientId: String,
    ): Doctor?

    /** Persists a new [doctor] record. */
    suspend fun insert(doctor: Doctor)

    /** Persists changes to an existing [doctor] record. */
    suspend fun update(doctor: Doctor)

    /**
     * Removes [doctor] from storage.
     * Prescriptions linked to this doctor will show "Doctor removed" in the UI.
     */
    suspend fun delete(doctor: Doctor)
}
