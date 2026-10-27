package com.medihistory.ai.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.medihistory.ai.data.dao.DoctorDao
import com.medihistory.ai.data.dao.PatientDao
import com.medihistory.ai.data.dao.PrescriptionDao
import com.medihistory.ai.data.model.Doctor
import com.medihistory.ai.data.model.Patient
import com.medihistory.ai.data.model.Prescription

/**
 * MediHistory Room database — version 4.
 *
 * Version history:
 *  1 → initial placeholder schema
 *  2 → added doctors table (MA-25, DoctorDao)
 *  3 → added patients table (MA-25, PatientDao)
 *  4 → added prescriptions table (MA-30, PrescriptionDao)
 *
 * Schema exports are written to android/app/schemas/ and committed to VCS.
 *
 * NEVER call fallbackToDestructiveMigration() or allowMainThreadQueries().
 * All migrations must be explicit and tested via MigrationTestHelper.
 */
@Database(
    entities = [Doctor::class, Patient::class, Prescription::class],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MediHistoryDatabase : RoomDatabase() {

    abstract fun doctorDao(): DoctorDao

    abstract fun patientDao(): PatientDao

    abstract fun prescriptionDao(): PrescriptionDao

    companion object {
        const val DATABASE_NAME = "medihistory.db"
    }
}
