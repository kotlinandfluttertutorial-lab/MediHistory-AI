package com.medihistory.ai.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
        db = Room.inMemoryDatabaseBuilder(context, MediHistoryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        assertNotNull("Database must not be null after build()", db)
    }

    @Test
    fun `should report schema version 3`() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, MediHistoryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val version = db!!.openHelper.readableDatabase.version
        assertEquals("Schema version must be 3 (patients table added)", 3, version)
    }
}
