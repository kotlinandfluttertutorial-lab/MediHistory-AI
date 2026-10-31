package com.medihistory.ai.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.medihistory.ai.data.database.MediHistoryDatabase
import com.medihistory.ai.data.model.DuplicateAlert
import com.medihistory.ai.data.model.MatchType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DuplicateAlertRepositoryTest {
    private lateinit var db: MediHistoryDatabase
    private lateinit var repository: DuplicateAlertRepository

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db =
            Room.inMemoryDatabaseBuilder(context, MediHistoryDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = RoomDuplicateAlertRepository(db.duplicateAlertDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `operations are scoped to patient and support review and explanation`() =
        runTest {
            val alert = alert()
            repository.insert(alert)
            repository.insert(alert(patientId = "patient-beta", id = "alert-beta"))

            assertEquals(listOf(alert), repository.observeByPatient("patient-alpha").first())
            assertEquals(1, db.duplicateAlertDao().observeUnreviewedCount("patient-alpha").first())
            assertNull(repository.getById(alert.id, "patient-beta"))

            repository.markReviewed(alert.id, alert.patientId, timestamp = 123L)
            repository.updateExplanation(alert.id, alert.patientId, "Potential duplicate information.")

            val updated = repository.getById(alert.id, alert.patientId)
            assertTrue(updated!!.isReviewed)
            assertEquals(123L, updated.reviewedAt)
            assertEquals("Potential duplicate information.", updated.explanation)
            assertEquals(0, db.duplicateAlertDao().observeUnreviewedCount(alert.patientId).first())

            repository.deleteByPatient(alert.patientId)
            assertTrue(repository.observeByPatient(alert.patientId).first().isEmpty())
            assertFalse(repository.observeByPatient("patient-beta").first().isEmpty())
        }

    private fun alert(
        id: String = "alert-alpha",
        patientId: String = "patient-alpha",
    ) = DuplicateAlert(
        id = id,
        patientId = patientId,
        medicineAId = "medicine-a",
        medicineBId = "medicine-b",
        matchingIngredient = "synthetic-ingredient",
        matchType = MatchType.EXACT,
        confidence = 0.95f,
        createdAt = 1L,
    )
}
