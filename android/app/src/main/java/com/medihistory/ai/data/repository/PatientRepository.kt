package com.medihistory.ai.data.repository

import com.medihistory.ai.data.model.Patient
import kotlinx.coroutines.flow.Flow

/**
 * Contract for patient data operations.
 *
 * Security invariant: [getById] accepts [patientId] explicitly so every call
 * site is forced to name the patient scope. No implementation may return a
 * record belonging to a different patient.
 */
interface PatientRepository {

    /**
     * Returns the patient with [patientId], or null.
     * Returns null (not an error) when no record with that id exists.
     */
    suspend fun getById(patientId: String): Patient?

    /** Persists a new [patient] record. */
    suspend fun insert(patient: Patient)

    /** Persists changes to an existing [patient] record. */
    suspend fun update(patient: Patient)

    /**
     * Removes [patient] from storage.
     * The caller is responsible for cascading deletion of related records.
     */
    suspend fun delete(patient: Patient)

    /**
     * Emits the live list of all patients, ordered by name.
     * The Flow updates automatically when the underlying data changes.
     */
    fun observeAll(): Flow<List<Patient>>
}
