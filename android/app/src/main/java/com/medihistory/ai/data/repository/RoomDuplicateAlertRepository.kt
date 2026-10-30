package com.medihistory.ai.data.repository

import com.medihistory.ai.data.dao.DuplicateAlertDao
import com.medihistory.ai.data.model.DuplicateAlert
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Room-backed implementation of [DuplicateAlertRepository].
 *
 * All operations delegate directly to [DuplicateAlertDao], which enforces patientId
 * scoping at the SQL level. Business rules live in UseCases, not here.
 *
 * Never log alert content, medicine names, or active ingredients — callers are responsible
 * for ensuring medical data is not written to Logcat or server logs.
 */
class RoomDuplicateAlertRepository @Inject constructor(
    private val duplicateAlertDao: DuplicateAlertDao,
) : DuplicateAlertRepository {

    override fun observeByPatient(patientId: String): Flow<List<DuplicateAlert>> =
        duplicateAlertDao.observeByPatient(patientId)

    override suspend fun insert(alert: DuplicateAlert) =
        duplicateAlertDao.insert(alert)

    override suspend fun markReviewed(id: String, patientId: String, timestamp: Long) =
        duplicateAlertDao.markReviewed(id, patientId, timestamp)

    override suspend fun deleteByPatient(patientId: String) =
        duplicateAlertDao.deleteByPatient(patientId)

    override suspend fun getById(id: String, patientId: String): DuplicateAlert? =
        duplicateAlertDao.getById(id, patientId)

    override suspend fun updateExplanation(id: String, patientId: String, explanation: String) =
        duplicateAlertDao.updateExplanation(id, patientId, explanation)
}
