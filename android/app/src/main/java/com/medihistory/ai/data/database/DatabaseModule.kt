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

/** Migration from v2 (doctors) to v3 (adds patients table — MA-25). */
private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `patients` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `dateOfBirth` INTEGER,
                `bloodGroup` TEXT,
                `allergies` TEXT NOT NULL DEFAULT '',
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
    }
}

/** Migration from v3 (doctors + patients) to v4 (adds prescriptions table — MA-30/31/32). */
private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `prescriptions` (
                `id` TEXT NOT NULL,
                `patientId` TEXT NOT NULL,
                `doctorId` TEXT,
                `prescriptionDate` INTEGER,
                `notes` TEXT,
                `documentPath` TEXT,
                `documentMimeType` TEXT,
                `documentHashSha256` TEXT,
                `ocrStatus` TEXT NOT NULL DEFAULT 'PENDING',
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_prescriptions_patientId` ON `prescriptions` (`patientId`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_prescriptions_doctorId` ON `prescriptions` (`doctorId`)"
        )
    }
}

/**
 * Migration from v4 (prescriptions) to v5 (adds extracted_fields and medicines — MA-35, MA-33).
 *
 * Both tables are added in the same migration step because both have foreign keys to
 * the prescriptions table, which must already exist at this version.
 */
private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // extracted_fields table — child of prescriptions (MA-35)
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `extracted_fields` (
                `id` TEXT NOT NULL,
                `prescriptionId` TEXT NOT NULL,
                `fieldName` TEXT NOT NULL,
                `rawValue` TEXT,
                `patientValue` TEXT,
                `confidence` REAL NOT NULL,
                `reviewState` TEXT NOT NULL,
                `sourceReference` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`prescriptionId`) REFERENCES `prescriptions`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_extracted_fields_prescriptionId` ON `extracted_fields` (`prescriptionId`)"
        )

        // medicines table — child of prescriptions (MA-33)
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `medicines` (
                `id` TEXT NOT NULL,
                `prescriptionId` TEXT NOT NULL,
                `patientId` TEXT NOT NULL,
                `medicineName` TEXT NOT NULL,
                `activeIngredient` TEXT,
                `brandName` TEXT,
                `strength` TEXT,
                `formulation` TEXT,
                `dosageInstructions` TEXT,
                `prescribingDoctorId` TEXT,
                `prescriptionDate` INTEGER,
                `status` TEXT NOT NULL DEFAULT 'UNKNOWN',
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`prescriptionId`) REFERENCES `prescriptions`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_medicines_patientId` ON `medicines` (`patientId`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_medicines_prescriptionId` ON `medicines` (`prescriptionId`)"
        )
    }
}

/** Migration from v5 (medicines and extracted fields) to v6 (adds duplicate alerts — MA-37). */
internal val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `duplicate_alerts` (
                `id` TEXT NOT NULL,
                `patientId` TEXT NOT NULL,
                `medicineAId` TEXT NOT NULL,
                `medicineBId` TEXT NOT NULL,
                `matchingIngredient` TEXT NOT NULL,
                `matchType` TEXT NOT NULL,
                `confidence` REAL NOT NULL,
                `isReviewed` INTEGER NOT NULL,
                `reviewedAt` INTEGER,
                `combinationNote` TEXT,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
    }
}

/** Migration from v6 (duplicate alerts) to v7 (adds reminders and dose_records — MA-41). */
internal val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // reminders table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `reminders` (
                `id` TEXT NOT NULL,
                `patientId` TEXT NOT NULL,
                `medicineId` TEXT NOT NULL,
                `medicineName` TEXT NOT NULL,
                `prescribedInstructions` TEXT,
                `reminderTime` INTEGER NOT NULL,
                `isActive` INTEGER NOT NULL DEFAULT 1,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_reminders_patientId` ON `reminders` (`patientId`)",
        )

        // dose_records table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `dose_records` (
                `id` TEXT NOT NULL,
                `reminderId` TEXT NOT NULL,
                `patientId` TEXT NOT NULL,
                `scheduledTime` INTEGER NOT NULL,
                `recordedTime` INTEGER,
                `record` TEXT NOT NULL,
                `patientNote` TEXT,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_dose_records_reminderId` ON `dose_records` (`reminderId`)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_dose_records_patientId` ON `dose_records` (`patientId`)",
        )
    }
}

/** Migration from v7 (reminders/dose_records) to v8 (adds explanation column to duplicate_alerts — F13). */
internal val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE duplicate_alerts ADD COLUMN explanation TEXT",
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMediHistoryDatabase(
        @ApplicationContext context: Context,
    ): MediHistoryDatabase =
        Room.databaseBuilder(
            context,
            MediHistoryDatabase::class.java,
            MediHistoryDatabase.DATABASE_NAME,
        )
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
            )
            .build()
    // fallbackToDestructiveMigration() intentionally omitted — migrations are always explicit
}
