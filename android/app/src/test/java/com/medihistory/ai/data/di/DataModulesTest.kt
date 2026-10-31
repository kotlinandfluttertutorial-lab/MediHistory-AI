package com.medihistory.ai.data.di

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.medihistory.ai.data.database.MediHistoryDatabase
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DataModulesTest {
    private lateinit var context: Context
    private lateinit var database: MediHistoryDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database =
            Room.inMemoryDatabaseBuilder(context, MediHistoryDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `DAO providers return the corresponding database DAOs`() {
        assertSame(database.doctorDao(), DoctorModule.provideDoctorDao(database))
        assertSame(database.duplicateAlertDao(), DuplicateAlertModule.provideDuplicateAlertDao(database))
        assertSame(database.medicineDao(), MedicineModule.provideMedicineDao(database))
        assertSame(database.extractedFieldDao(), OcrDaoModule.provideExtractedFieldDao(database))
        assertSame(database.patientDao(), PatientModule.providePatientDao(database))
        assertSame(database.prescriptionDao(), PrescriptionModule.providePrescriptionDao(database))
        assertSame(database.reminderDao(), ReminderModule.provideReminderDao(database))
        assertSame(database.doseRecordDao(), ReminderModule.provideDoseRecordDao(database))
    }

    @Test
    fun `WorkManager provider returns initialized instance`() {
        assertNotNull(WorkManagerModule.provideWorkManager(context))
        assertSame(WorkManager.getInstance(context), WorkManagerModule.provideWorkManager(context))
    }
}
