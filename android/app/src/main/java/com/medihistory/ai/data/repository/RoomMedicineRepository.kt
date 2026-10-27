package com.medihistory.ai.data.repository

import com.medihistory.ai.data.dao.MedicineDao
import com.medihistory.ai.data.model.Medicine
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Room-backed implementation of [MedicineRepository].
 *
 * All operations delegate directly to [MedicineDao], which enforces patientId
 * scoping at the SQL level. Business rules live in UseCases, not here.
 *
 * Never log medicine names, ingredients, or dosage details — callers are responsible
 * for ensuring medical data is not written to Logcat or server logs.
 */
class RoomMedicineRepository @Inject constructor(
    private val medicineDao: MedicineDao,
) : MedicineRepository {

    override fun observeByPatient(patientId: String): Flow<List<Medicine>> =
        medicineDao.observeByPatient(patientId)

    override fun searchByPatient(patientId: String, query: String): Flow<List<Medicine>> =
        medicineDao.searchByPatient(patientId, query)

    override suspend fun getById(id: String, patientId: String): Medicine? =
        medicineDao.getById(id, patientId)

    override suspend fun insertAll(medicines: List<Medicine>) =
        medicineDao.insertAll(medicines)

    override suspend fun update(medicine: Medicine) =
        medicineDao.update(medicine)

    override suspend fun delete(medicine: Medicine) =
        medicineDao.delete(medicine)

    override fun observeActiveIngredients(patientId: String): Flow<List<String>> =
        medicineDao.observeActiveIngredients(patientId)
}
