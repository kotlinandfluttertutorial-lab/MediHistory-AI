package com.medihistory.ai.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.medihistory.ai.data.dao.PatientDao
import com.medihistory.ai.data.model.Patient

/**
 * MediHistory Room database — version 2.
 *
 * Version history:
 *  1 → initial placeholder schema
 *  2 → added patients table (MA-25, PatientDao)
 *
 * Schema exports are written to android/app/schemas/ and committed to VCS.
 *
 * NEVER call fallbackToDestructiveMigration() or allowMainThreadQueries().
 * All migrations must be explicit and tested via MigrationTestHelper.
 */
@Database(
    entities = [Patient::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MediHistoryDatabase : RoomDatabase() {

    abstract fun patientDao(): PatientDao

    companion object {
        const val DATABASE_NAME = "medihistory.db"
    }
}
