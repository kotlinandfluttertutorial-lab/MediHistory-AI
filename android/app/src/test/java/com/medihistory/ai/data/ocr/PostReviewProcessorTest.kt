package com.medihistory.ai.data.ocr

import com.medihistory.ai.data.dao.ExtractedFieldDao
import com.medihistory.ai.data.dao.PrescriptionDao
import com.medihistory.ai.data.duplicate.DuplicateExplainerEngine
import com.medihistory.ai.data.model.Doctor
import com.medihistory.ai.data.model.DoctorSpecialty
import com.medihistory.ai.data.model.DuplicateAlert
import com.medihistory.ai.data.model.ExtractedFieldEntity
import com.medihistory.ai.data.model.Medicine
import com.medihistory.ai.data.model.MedicineStatus
import com.medihistory.ai.data.model.Prescription
import com.medihistory.ai.data.repository.DoctorRepository
import com.medihistory.ai.data.repository.DuplicateAlertRepository
import com.medihistory.ai.data.repository.MedicineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PostReviewProcessorTest {
    private val patientId = "patient-synthetic"
    private val prescription = Prescription(id = "rx-synthetic", patientId = patientId)

    @Test
    fun process_returns_without_loading_fields_for_missing_prescription() =
        runTest {
            val fixture = Fixture(prescription = null)

            fixture.processor.process("missing-rx", patientId)

            assertEquals(0, fixture.fieldDao.observeCalls)
            assertNull(fixture.prescriptionDao.updated)
            assertTrue(fixture.medicineRepository.inserted.isEmpty())
        }

    @Test
    fun process_uses_only_reviewed_patient_values_and_creates_doctor_and_medicine() =
        runTest {
            val fixture =
                Fixture(
                    fields =
                        listOf(
                            field("doctor_name", "  Dr. Synthetic  ", ReviewState.REVIEWED),
                            field("prescription_date", "01/07/2025", ReviewState.REVIEWED),
                            field("medicine_name", "  Synthamol  ", ReviewState.REVIEWED),
                            field("dose", "  500mg  ", ReviewState.REVIEWED),
                            field("frequency", " once daily ", ReviewState.REVIEWED),
                            field("medicine_name_2", "Testocillin", ReviewState.PENDING_REVIEW),
                            field("medicine_name_3", "RejectedMed", ReviewState.REJECTED),
                            field("medicine_name_4", null, ReviewState.REVIEWED),
                            field("dose_2", "twice daily", ReviewState.REVIEWED),
                        ),
                )

            fixture.processor.process(prescription.id, patientId)

            val newDoctor = fixture.doctorRepository.inserted.single()
            assertEquals("Dr. Synthetic", newDoctor.name)
            assertEquals(DoctorSpecialty.OTHER, newDoctor.specialty)
            val updated = fixture.prescriptionDao.updated!!
            assertEquals(newDoctor.id, updated.doctorId)
            assertEquals(LocalDate.parse("2025-07-01"), updated.prescriptionDate)

            val medicine = fixture.medicineRepository.inserted.single()
            assertEquals("Synthamol", medicine.medicineName)
            assertEquals("500mg, once daily", medicine.dosageInstructions)
            assertEquals(newDoctor.id, medicine.prescribingDoctorId)
            assertEquals(LocalDate.parse("2025-07-01"), medicine.prescriptionDate)
            assertEquals(MedicineStatus.UNKNOWN, medicine.status)
            assertEquals("rx-synthetic", medicine.prescriptionId)
            assertEquals(patientId, medicine.patientId)
            assertTrue(fixture.alertRepository.inserted.isEmpty())
        }

    @Test
    fun process_reuses_doctor_and_ignores_invalid_date_and_blank_medicine_values() =
        runTest {
            val existingDoctor =
                Doctor(
                    id = "doctor-existing",
                    patientId = patientId,
                    name = "Dr. Synthetic",
                    specialty = DoctorSpecialty.OTHER,
                    createdAt = 1L,
                    updatedAt = 1L,
                )
            val fixture =
                Fixture(
                    fields =
                        listOf(
                            field("doctor_name", " dr. synthetic ", ReviewState.REVIEWED),
                            field("prescription_date", "not a date", ReviewState.REVIEWED),
                            field("medicine_name", "   ", ReviewState.REVIEWED),
                            field("medicine_name_2", "Testocillin", ReviewState.REVIEWED),
                            field("dose_2", "  ", ReviewState.REVIEWED),
                        ),
                    doctors = listOf(existingDoctor),
                    prescription = prescription.copy(prescriptionDate = LocalDate.parse("2024-01-02")),
                )

            fixture.processor.process(prescription.id, patientId)

            assertTrue(fixture.doctorRepository.inserted.isEmpty())
            assertEquals("doctor-existing", fixture.prescriptionDao.updated!!.doctorId)
            assertEquals(LocalDate.parse("2024-01-02"), fixture.prescriptionDao.updated!!.prescriptionDate)
            assertEquals("Testocillin", fixture.medicineRepository.inserted.single().medicineName)
            assertNull(fixture.medicineRepository.inserted.single().dosageInstructions)
            assertEquals(LocalDate.parse("2024-01-02"), fixture.medicineRepository.inserted.single().prescriptionDate)
        }

    @Test
    fun process_preserves_existing_doctor_when_no_reviewed_name_and_handles_no_medicines() =
        runTest {
            val original = prescription.copy(doctorId = "already-linked")
            val fixture =
                Fixture(
                    fields =
                        listOf(
                            field("doctor_name", "ignored", ReviewState.PENDING_REVIEW),
                            field("medicine_name", "ignored", ReviewState.REJECTED),
                        ),
                    prescription = original,
                )

            fixture.processor.process(original.id, patientId)

            assertEquals("already-linked", fixture.prescriptionDao.updated!!.doctorId)
            assertTrue(fixture.doctorRepository.inserted.isEmpty())
            assertTrue(fixture.medicineRepository.inserted.isEmpty())
        }

    private fun field(
        name: String,
        patientValue: String?,
        state: ReviewState,
    ) = ExtractedFieldEntity(
        id = "$name-${state.name}",
        prescriptionId = "rx-synthetic",
        fieldName = name,
        rawValue = "raw-only",
        patientValue = patientValue,
        confidence = 0.9f,
        reviewState = state,
        sourceReference = null,
    )

    private class Fixture(
        fields: List<ExtractedFieldEntity> = emptyList(),
        doctors: List<Doctor> = emptyList(),
        prescription: Prescription? = Prescription(id = "rx-synthetic", patientId = "patient-synthetic"),
    ) {
        val prescriptionDao = FakePrescriptionDao(prescription)
        val fieldDao = FakeExtractedFieldDao(fields)
        val doctorRepository = FakeDoctorRepository(doctors)
        val medicineRepository = FakeMedicineRepository()
        val alertRepository = FakeDuplicateAlertRepository()
        val processor =
            PostReviewProcessor(
                fieldDao,
                prescriptionDao,
                doctorRepository,
                medicineRepository,
                alertRepository,
                DuplicateExplainerEngine(),
            )
    }

    private class FakePrescriptionDao(
        var current: Prescription?,
    ) : PrescriptionDao {
        var updated: Prescription? = null

        override fun observeByPatient(patientId: String): Flow<List<Prescription>> = flowOf(emptyList())

        override suspend fun getById(
            id: String,
            patientId: String,
        ): Prescription? = current?.takeIf { it.id == id && it.patientId == patientId }

        override suspend fun insert(prescription: Prescription) {
            current = prescription
        }

        override suspend fun update(prescription: Prescription) {
            updated = prescription
            current = prescription
        }

        override suspend fun delete(prescription: Prescription) {
            current = null
        }

        override suspend fun getPatientId(id: String): String? = current?.patientId

        override suspend fun updateOcrStatus(
            id: String,
            patientId: String,
            status: com.medihistory.ai.data.model.OcrStatus,
            updatedAt: Long,
        ) = Unit
    }

    private class FakeExtractedFieldDao(
        private val fields: List<ExtractedFieldEntity>,
    ) : ExtractedFieldDao {
        var observeCalls = 0

        override fun observeByPrescription(prescriptionId: String): Flow<List<ExtractedFieldEntity>> {
            observeCalls++
            return flowOf(fields.filter { it.prescriptionId == prescriptionId })
        }

        override suspend fun insertAll(fields: List<ExtractedFieldEntity>) = Unit

        override suspend fun updateReview(
            id: String,
            state: ReviewState,
            patientValue: String?,
            ts: Long,
        ) = Unit

        override suspend fun deleteByPrescription(prescriptionId: String) = Unit
    }

    private class FakeDoctorRepository(
        private val doctors: List<Doctor>,
    ) : DoctorRepository {
        val inserted = mutableListOf<Doctor>()

        override fun observeByPatient(patientId: String): Flow<List<Doctor>> = flowOf(doctors + inserted.filter { it.patientId == patientId })

        override suspend fun getById(
            doctorId: String,
            patientId: String,
        ): Doctor? = doctors.firstOrNull { it.id == doctorId && it.patientId == patientId }

        override suspend fun insert(doctor: Doctor) {
            inserted += doctor
        }

        override suspend fun update(doctor: Doctor) = Unit

        override suspend fun delete(doctor: Doctor) = Unit
    }

    private class FakeMedicineRepository : MedicineRepository {
        var inserted = emptyList<Medicine>()

        override fun observeByPatient(patientId: String): Flow<List<Medicine>> = flowOf(inserted.filter { it.patientId == patientId })

        override fun searchByPatient(
            patientId: String,
            query: String,
        ): Flow<List<Medicine>> = observeByPatient(patientId)

        override suspend fun getById(
            id: String,
            patientId: String,
        ): Medicine? = inserted.firstOrNull { it.id == id && it.patientId == patientId }

        override suspend fun insertAll(medicines: List<Medicine>) {
            inserted = inserted + medicines
        }

        override suspend fun update(medicine: Medicine) = Unit

        override suspend fun delete(medicine: Medicine) = Unit

        override fun observeActiveIngredients(patientId: String): Flow<List<String>> = flowOf(emptyList())
    }

    private class FakeDuplicateAlertRepository : DuplicateAlertRepository {
        val inserted = mutableListOf<DuplicateAlert>()

        override fun observeByPatient(patientId: String): Flow<List<DuplicateAlert>> = flowOf(inserted)

        override suspend fun insert(alert: DuplicateAlert) {
            inserted += alert
        }

        override suspend fun markReviewed(
            id: String,
            patientId: String,
            timestamp: Long,
        ) = Unit

        override suspend fun deleteByPatient(patientId: String) = Unit

        override suspend fun getById(
            id: String,
            patientId: String,
        ): DuplicateAlert? = inserted.firstOrNull { it.id == id && it.patientId == patientId }

        override suspend fun updateExplanation(
            id: String,
            patientId: String,
            explanation: String,
        ) = Unit
    }
}
