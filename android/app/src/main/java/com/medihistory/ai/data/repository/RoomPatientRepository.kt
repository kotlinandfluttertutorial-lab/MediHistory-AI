package com.medihistory.ai.data.repository

import com.medihistory.ai.data.dao.PatientDao
import com.medihistory.ai.data.model.Patient
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Room-backed implementation of [PatientRepository].
 *
 * All operations delegate directly to [PatientDao].
 * Business rules live in UseCases, not here.
 *
 * Security invariants:
 * - No patient data is written to Logcat or any log sink.
 * - [getById] passes [patientId] through to the DAO, which scopes the SQL query.
 */
class RoomPatientRepository @Inject constructor(
    private val patientDao: PatientDao,
) : PatientRepository {

    override suspend fun getById(patientId: String): Patient? =
        patientDao.getById(patientId)

    override suspend fun insert(patient: Patient) =
        patientDao.insert(patient)

    override suspend fun update(patient: Patient) =
        patientDao.update(patient)

    override suspend fun delete(patient: Patient) =
        patientDao.delete(patient)

    override fun observeAll(): Flow<List<Patient>> =
        patientDao.observeAll()
}
