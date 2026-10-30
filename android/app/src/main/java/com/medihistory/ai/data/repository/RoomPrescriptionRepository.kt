package com.medihistory.ai.data.repository

import com.medihistory.ai.data.dao.PrescriptionDao
import com.medihistory.ai.data.model.OcrStatus
import com.medihistory.ai.data.model.Prescription
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Room-backed implementation of [PrescriptionRepository].
 *
 * All operations delegate directly to [PrescriptionDao], which enforces patientId
 * scoping at the SQL level. Business rules live in UseCases, not here.
 * Never log prescription text, document content, or document paths.
 */
class RoomPrescriptionRepository
    @Inject
    constructor(
        private val prescriptionDao: PrescriptionDao,
    ) : PrescriptionRepository {
        override fun observeByPatient(patientId: String): Flow<List<Prescription>> = prescriptionDao.observeByPatient(patientId)

        override suspend fun getById(
            id: String,
            patientId: String,
        ): Prescription? = prescriptionDao.getById(id, patientId)

        override suspend fun insert(prescription: Prescription) = prescriptionDao.insert(prescription)

        override suspend fun update(prescription: Prescription) = prescriptionDao.update(prescription)

        override suspend fun delete(prescription: Prescription) = prescriptionDao.delete(prescription)

        override suspend fun updateOcrStatus(
            id: String,
            patientId: String,
            status: OcrStatus,
            updatedAt: Long,
        ) = prescriptionDao.updateOcrStatus(id, patientId, status, updatedAt)
    }
