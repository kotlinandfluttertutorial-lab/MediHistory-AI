package com.medihistory.ai.data.repository

import com.medihistory.ai.data.dao.DoctorDao
import com.medihistory.ai.data.model.Doctor
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Room-backed implementation of [DoctorRepository].
 *
 * All operations delegate directly to [DoctorDao], which enforces patientId
 * scoping at the SQL level. Business rules live in UseCases, not here.
 */
class RoomDoctorRepository
    @Inject
    constructor(
        private val doctorDao: DoctorDao,
    ) : DoctorRepository {
        override fun observeByPatient(patientId: String): Flow<List<Doctor>> = doctorDao.observeByPatient(patientId)

        override suspend fun getById(
            doctorId: String,
            patientId: String,
        ): Doctor? = doctorDao.getById(doctorId, patientId)

        override suspend fun insert(doctor: Doctor) = doctorDao.insert(doctor)

        override suspend fun update(doctor: Doctor) = doctorDao.update(doctor)

        override suspend fun delete(doctor: Doctor) = doctorDao.delete(doctor)
    }
