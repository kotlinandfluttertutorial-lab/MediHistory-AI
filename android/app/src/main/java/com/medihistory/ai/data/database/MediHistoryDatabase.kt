package com.medihistory.ai.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.medihistory.ai.data.dao.DoctorDao
import com.medihistory.ai.data.dao.PatientDao
import com.medihistory.ai.data.model.Doctor
import com.medihistory.ai.data.model.Patient

/**
 * MediHistory Room database — version 3.
 *
 * Version history:
 *  1 → initial placeholder schema
 *  2 → added doctors table (MA-25, DoctorDao)
 *  3 → added patients table (MA-25, PatientDao)
 *
 * Schema exports are written to android/app/schemas/ and committed to VCS.
 *
 * NEVER call fallbackToDestructiveMigration() or allowMainThreadQueries().
 * All migrations must be explicit and tested via MigrationTestHelper.
 */
@Database(
    entities = [Doctor::class, Patient::class],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MediHistoryDatabase : RoomDatabase() {

    abstract fun doctorDao(): DoctorDao

    abstract fun patientDao(): PatientDao

    companion object {
        const val DATABASE_NAME = "medihistory.db"
    }
}
