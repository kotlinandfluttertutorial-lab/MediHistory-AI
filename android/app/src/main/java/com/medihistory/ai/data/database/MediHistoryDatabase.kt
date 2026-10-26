package com.medihistory.ai.data.database

import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.medihistory.ai.data.dao.DoctorDao
import com.medihistory.ai.data.model.Doctor

// Placeholder entity — will be removed once all real entities are added.
@Entity(tableName = "db_init_placeholder")
internal data class DbInitPlaceholder(
    @PrimaryKey val id: Int = 1
)

/**
 * MediHistory Room database — version 2.
 *
 * v1 → v2: Added [Doctor] entity (MA-25).
 *
 * Schema exports are written to android/app/schemas/ and committed to VCS.
 *
 * NEVER call fallbackToDestructiveMigration() or allowMainThreadQueries().
 * All migrations must be explicit and tested via MigrationTestHelper.
 */
@Database(
    entities = [DbInitPlaceholder::class, Doctor::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MediHistoryDatabase : RoomDatabase() {

    abstract fun doctorDao(): DoctorDao

    companion object {
        const val DATABASE_NAME = "medihistory.db"
    }
}
