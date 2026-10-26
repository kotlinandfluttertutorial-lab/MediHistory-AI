package com.medihistory.ai.data.database

import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// Placeholder entity — replaced by real entities in MA-23 (Prescription), MA-25 (Patient),
// MA-26 (Doctor). Remove when first real entity is added.
@Entity(tableName = "db_init_placeholder")
internal data class DbInitPlaceholder(
    @PrimaryKey val id: Int = 1
)

/**
 * MediHistory Room database — version 1.
 *
 * Entities will be added as feature specs are implemented.
 * Schema exports are written to android/app/schemas/ and committed to VCS.
 *
 * NEVER call fallbackToDestructiveMigration() or allowMainThreadQueries().
 * All migrations must be explicit and tested via MigrationTestHelper.
 */
@Database(
    entities = [DbInitPlaceholder::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MediHistoryDatabase : RoomDatabase() {
    companion object {
        const val DATABASE_NAME = "medihistory.db"
    }
}
