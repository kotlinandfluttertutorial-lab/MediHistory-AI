package com.medihistory.ai.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy

/**
 * Unit tests for [MediHistoryDatabase].
 *
 * Runs on the JVM via Robolectric — no emulator required.
 * Uses in-memory databases; the production database is never touched.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MediHistoryDatabaseTest {
    private var db: MediHistoryDatabase? = null

    @After
    fun tearDown() {
        db?.close()
    }

    @Test
    fun `should open in-memory database without error`() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db =
            Room.inMemoryDatabaseBuilder(context, MediHistoryDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        assertNotNull("Database must not be null after build()", db)
    }

    @Test
    fun `should report schema version 7`() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db =
            Room.inMemoryDatabaseBuilder(context, MediHistoryDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        val version = db!!.openHelper.readableDatabase.version
        assertEquals(
            "Schema version must be 8 (explanation column added to duplicate_alerts)",
            8,
            version,
        )
    }

    @Test
    fun `should expose extractedFieldDao`() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db =
            Room.inMemoryDatabaseBuilder(context, MediHistoryDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        assertNotNull("extractedFieldDao must not be null", db!!.extractedFieldDao())
    }

    @Test
    fun `should expose prescriptionDao`() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db =
            Room.inMemoryDatabaseBuilder(context, MediHistoryDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        assertNotNull("prescriptionDao must not be null", db!!.prescriptionDao())
    }

    @Test
    fun `should expose duplicateAlertDao`() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db =
            Room.inMemoryDatabaseBuilder(context, MediHistoryDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        assertNotNull("duplicateAlertDao must not be null", db!!.duplicateAlertDao())
    }

    @Test
    fun `should expose all remaining data access objects`() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db =
            Room.inMemoryDatabaseBuilder(context, MediHistoryDatabase::class.java)
                .allowMainThreadQueries()
                .build()

        assertNotNull(db!!.doctorDao())
        assertNotNull(db!!.patientDao())
        assertNotNull(db!!.medicineDao())
        assertNotNull(db!!.reminderDao())
        assertNotNull(db!!.doseRecordDao())
    }

    @Test
    fun `database module provides a Room database`() {
        val context: Context = ApplicationProvider.getApplicationContext()

        db = DatabaseModule.provideMediHistoryDatabase(context)

        assertNotNull(db)
    }

    @Test
    fun `all schema migrations execute their SQL statements`() {
        val migrations =
            listOf(
                "MIGRATION_1_2",
                "MIGRATION_2_3",
                "MIGRATION_3_4",
                "MIGRATION_4_5",
                "MIGRATION_5_6",
                "MIGRATION_6_7",
                "MIGRATION_7_8",
            )

        migrations.forEach { migrationName ->
            val statements = mutableListOf<String>()
            val database =
                Proxy.newProxyInstance(
                    SupportSQLiteDatabase::class.java.classLoader,
                    arrayOf(SupportSQLiteDatabase::class.java),
                ) { _, method, args ->
                    if (method.name == "execSQL") {
                        statements.add(args!![0] as String)
                        null
                    } else {
                        throw UnsupportedOperationException("Unexpected database call: ${method.name}")
                    }
                } as SupportSQLiteDatabase

            migration(migrationName).migrate(database)

            assertNotNull("$migrationName must issue migration SQL", statements.firstOrNull())
        }
    }

    private fun migration(name: String): Migration {
        val field = Class.forName("com.medihistory.ai.data.database.DatabaseModuleKt").getDeclaredField(name)
        field.isAccessible = true
        return field.get(null) as Migration
    }
}
