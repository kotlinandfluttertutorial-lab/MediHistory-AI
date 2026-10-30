package com.medihistory.ai.data.repository

import com.medihistory.ai.data.dao.DoseRecordDao
import com.medihistory.ai.data.dao.ReminderDao
import com.medihistory.ai.data.model.DoseRecordEntity
import com.medihistory.ai.data.model.Reminder
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Room-backed implementation of [ReminderRepository].
 *
 * All operations delegate directly to [ReminderDao] and [DoseRecordDao], which enforce
 * patientId scoping at the SQL level. Business rules live in ViewModels, not here.
 *
 * Medical safety: this class never reads, copies, or modifies [Reminder.prescribedInstructions].
 * It stores and retrieves data exactly as provided by the caller.
 *
 * Never log reminder content, dose records, medicine names, or patient data.
 */
class RoomReminderRepository
    @Inject
    constructor(
        private val reminderDao: ReminderDao,
        private val doseRecordDao: DoseRecordDao,
    ) : ReminderRepository {
        override fun observeByPatient(patientId: String): Flow<List<Reminder>> = reminderDao.observeByPatient(patientId)

        override suspend fun getById(
            id: String,
            patientId: String,
        ): Reminder? = reminderDao.getById(id, patientId)

        override suspend fun insert(reminder: Reminder) = reminderDao.insert(reminder)

        override suspend fun update(reminder: Reminder) = reminderDao.update(reminder)

        override suspend fun delete(reminder: Reminder) = reminderDao.delete(reminder)

        override fun observeDoseRecords(reminderId: String): Flow<List<DoseRecordEntity>> = doseRecordDao.observeByReminder(reminderId)

        override suspend fun insertDoseRecord(doseRecord: DoseRecordEntity) = doseRecordDao.insert(doseRecord)

        override suspend fun getDoseRecordsByDateRange(
            patientId: String,
            from: Long,
            to: Long,
        ): List<DoseRecordEntity> = doseRecordDao.getByDateRange(patientId, from, to)
    }
