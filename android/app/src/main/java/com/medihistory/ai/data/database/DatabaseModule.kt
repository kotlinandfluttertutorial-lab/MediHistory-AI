package com.medihistory.ai.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Migration from v1 (placeholder only) to v2 (adds doctors table — MA-25). */
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `doctors` (
                `id` TEXT NOT NULL,
                `patientId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `specialty` TEXT NOT NULL,
                `specialtyOther` TEXT,
                `clinicName` TEXT,
                `contactNotes` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMediHistoryDatabase(
        @ApplicationContext context: Context
    ): MediHistoryDatabase =
        Room.databaseBuilder(
            context,
            MediHistoryDatabase::class.java,
            MediHistoryDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_1_2)
            .build()
        // fallbackToDestructiveMigration() intentionally omitted — migrations are always explicit
}
