package com.medihistory.ai.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented migration tests for [MediHistoryDatabase].
 *
 * Uses [MigrationTestHelper] and the exported schema JSON in android/app/schemas/.
 * Run with: .\gradlew.bat connectedDebugAndroidTest
 *
 * Add a test method here for every future migration.
 * NEVER call fallbackToDestructiveMigration() — all migrations must be explicit.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    private val testDb = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            MediHistoryDatabase::class.java,
        )

    @Test
    fun opensVersionOneSchema() {
        helper.createDatabase(testDb, 1).use { db ->
            assert(db.version == 1) {
                "Expected database version 1 but was ${db.version}"
            }
        }
    }

    @Test
    fun migrationFromVersionFiveCreatesDuplicateAlertsTable() {
        helper.createDatabase(testDb, 5).close()

        helper.runMigrationsAndValidate(testDb, 6, true, MIGRATION_5_6).use { db ->
            assert(db.version == 6) {
                "Expected database version 6 but was ${db.version}"
            }
            db.query("SELECT * FROM duplicate_alerts").use { cursor ->
                assert(cursor.columnNames.contains("patientId")) {
                    "Expected duplicate_alerts to include patientId"
                }
            }
        }
    }
}
