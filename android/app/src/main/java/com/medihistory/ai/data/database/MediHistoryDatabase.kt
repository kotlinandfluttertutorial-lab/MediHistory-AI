package com.medihistory.ai.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.medihistory.ai.data.dao.DoctorDao
import com.medihistory.ai.data.dao.ExtractedFieldDao
import com.medihistory.ai.data.dao.MedicineDao
import com.medihistory.ai.data.dao.PatientDao
import com.medihistory.ai.data.dao.PrescriptionDao
import com.medihistory.ai.data.model.Doctor
import com.medihistory.ai.data.model.ExtractedFieldEntity
import com.medihistory.ai.data.model.Medicine
import com.medihistory.ai.data.model.Patient
import com.medihistory.ai.data.model.Prescription

/**
 * MediHistory Room database — version 5.
 *
 * Version history:
 *  1 → initial placeholder schema
 *  2 → added doctors table (MA-25, DoctorDao)
 *  3 → added patients table (MA-25, PatientDao)
 *  4 → added prescriptions table (MA-30/31/32, PrescriptionDao)
 *  5 → added extracted_fields and medicines tables (MA-35, MA-33)
 *
 * Schema exports are written to android/app/schemas/ and committed to VCS.
 *
 * NEVER call fallbackToDestructiveMigration() or allowMainThreadQueries().
 * All migrations must be explicit and tested via MigrationTestHelper.
 */
@Database(
    entities = [
        Doctor::class,
        Patient::class,
        Prescription::class,
        ExtractedFieldEntity::class,
        Medicine::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MediHistoryDatabase : RoomDatabase() {

    abstract fun doctorDao(): DoctorDao

    abstract fun patientDao(): PatientDao

    abstract fun prescriptionDao(): PrescriptionDao

    abstract fun extractedFieldDao(): ExtractedFieldDao

    abstract fun medicineDao(): MedicineDao

    companion object {
        const val DATABASE_NAME = "medihistory.db"
    }
}
