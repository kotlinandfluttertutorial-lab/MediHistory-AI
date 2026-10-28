package com.medihistory.ai.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.medihistory.ai.data.dao.ExtractedFieldDao
import com.medihistory.ai.data.dao.PrescriptionDao
import com.medihistory.ai.data.model.ExtractedFieldEntity
import com.medihistory.ai.data.model.OcrStatus
import com.medihistory.ai.data.ocr.ExtractedField
import com.medihistory.ai.data.ocr.OcrResult
import com.medihistory.ai.data.ocr.OcrService
import com.medihistory.ai.data.ocr.ReviewState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [OcrWorker].
 *
 * Uses Robolectric and [TestListenableWorkerBuilder] — no emulator required.
 * All fakes use synthetic data only; no real patient names, medicine brands, or documents.
 * Naming convention: `should_[outcome]_when_[condition]`
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OcrWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    // --- fake input data ---

    private val syntheticPrescriptionId = "synth-rx-worker-001"
    private val syntheticPatientId = "synth-patient-worker-001"
    private val syntheticDocumentPath = "/data/user/0/com.medihistory.ai/files/synth-doc.jpg"
    private val syntheticMimeType = "image/jpeg"

    private fun buildWorker(
        ocrService: OcrService,
        prescriptionDao: PrescriptionDao,
        extractedFieldDao: ExtractedFieldDao,
    ): OcrWorker =
        TestListenableWorkerBuilder<OcrWorker>(context)
            .setInputData(
                androidx.work.workDataOf(
                    OcrWorker.KEY_PRESCRIPTION_ID to syntheticPrescriptionId,
                    OcrWorker.KEY_PATIENT_ID to syntheticPatientId,
                    OcrWorker.KEY_DOCUMENT_PATH to syntheticDocumentPath,
                    OcrWorker.KEY_MIME_TYPE to syntheticMimeType,
                ),
            )
            .setWorkerFactory(
                FakeOcrWorkerFactory(ocrService, prescriptionDao, extractedFieldDao),
            )
            .build()

    // --- tests ---

    @Test
    fun `should set status PROCESSING before calling OCR service`() = runTest {
        val statusLog = mutableListOf<OcrStatus>()
        val fakePrescriptionDao = RecordingPrescriptionDao(statusLog)
        val fakeExtractedFieldDao = NoOpExtractedFieldDao()
        val fakeOcrService = SuccessfulOcrService(syntheticPrescriptionId)

        val worker = buildWorker(fakeOcrService, fakePrescriptionDao, fakeExtractedFieldDao)
        worker.doWork()

        assertEquals(
            "First status update must be PROCESSING",
            OcrStatus.PROCESSING,
            statusLog.firstOrNull(),
        )
    }

    @Test
    fun `should set status NEEDS_REVIEW when OCR returns low-confidence fields`() = runTest {
        val statusLog = mutableListOf<OcrStatus>()
        val fakePrescriptionDao = RecordingPrescriptionDao(statusLog)
        val fakeExtractedFieldDao = NoOpExtractedFieldDao()
        // Returns a result with pendingReviewCount > 0 (low-confidence fields)
        val fakeOcrService = SuccessfulOcrService(syntheticPrescriptionId)

        val worker = buildWorker(fakeOcrService, fakePrescriptionDao, fakeExtractedFieldDao)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(
            "Final status must be NEEDS_REVIEW when pendingReviewCount > 0",
            OcrStatus.NEEDS_REVIEW,
            statusLog.lastOrNull(),
        )
    }

    @Test
    fun `should set status ERROR when OcrService throws an exception`() = runTest {
        val statusLog = mutableListOf<OcrStatus>()
        val fakePrescriptionDao = RecordingPrescriptionDao(statusLog)
        val fakeExtractedFieldDao = NoOpExtractedFieldDao()
        val throwingService = ThrowingOcrService()

        val worker = buildWorker(throwingService, fakePrescriptionDao, fakeExtractedFieldDao)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
        assertEquals(
            "Final status must be ERROR when OcrService throws",
            OcrStatus.ERROR,
            statusLog.lastOrNull(),
        )
    }

    @Test
    fun `should return failure when prescription_id input is missing`() = runTest {
        val worker = TestListenableWorkerBuilder<OcrWorker>(context)
            .setInputData(
                androidx.work.workDataOf(
                    OcrWorker.KEY_PATIENT_ID to syntheticPatientId,
                    OcrWorker.KEY_DOCUMENT_PATH to syntheticDocumentPath,
                    OcrWorker.KEY_MIME_TYPE to syntheticMimeType,
                    // KEY_PRESCRIPTION_ID intentionally omitted
                ),
            )
            .setWorkerFactory(
                FakeOcrWorkerFactory(
                    SuccessfulOcrService(syntheticPrescriptionId),
                    RecordingPrescriptionDao(mutableListOf()),
                    NoOpExtractedFieldDao(),
                ),
            )
            .build()

        assertEquals(ListenableWorker.Result.failure(), worker.doWork())
    }
}

// ---------------------------------------------------------------------------
// Fakes — synthetic only, no real data
// ---------------------------------------------------------------------------

/**
 * Returns a successful [OcrResult] with synthetic low-confidence fields
 * so that [pendingReviewCount] > 0 and the worker sets NEEDS_REVIEW.
 */
private class SuccessfulOcrService(private val prescriptionId: String) : OcrService {
    override suspend fun extract(
        documentPath: String,
        mimeType: String,
        prescriptionId: String,
    ): OcrResult = OcrResult(
        prescriptionId = this.prescriptionId,
        fields = listOf(
            // Low confidence — forces PENDING_REVIEW and increments pendingReviewCount.
            ExtractedField("doctor_name", "Dr. Synthetic", null, 0.85f),
            ExtractedField("medicine_name", "Synthamol 500mg", null, 0.62f),
            // Absent field — rawValue null, does NOT count toward pendingReviewCount.
            ExtractedField("dose", null, null, 0.0f),
        ),
        overallConfidence = 0.65f,
    )
}

/** Always throws to simulate an OCR service failure. */
private class ThrowingOcrService : OcrService {
    override suspend fun extract(
        documentPath: String,
        mimeType: String,
        prescriptionId: String,
    ): OcrResult = throw RuntimeException("Synthetic OCR service failure — for testing only")
}

/** Records each [OcrStatus] update for assertion. */
private class RecordingPrescriptionDao(
    private val log: MutableList<OcrStatus>,
) : PrescriptionDao {
    override fun observeByPatient(patientId: String) =
        flowOf(emptyList<com.medihistory.ai.data.model.Prescription>())

    override suspend fun getById(id: String, patientId: String) = null

    override suspend fun insert(prescription: com.medihistory.ai.data.model.Prescription) = Unit

    override suspend fun update(prescription: com.medihistory.ai.data.model.Prescription) = Unit

    override suspend fun delete(prescription: com.medihistory.ai.data.model.Prescription) = Unit

    override suspend fun getPatientId(id: String): String? = null

    override suspend fun updateOcrStatus(
        id: String,
        patientId: String,
        status: OcrStatus,
        updatedAt: Long,
    ) {
        log.add(status)
    }

}

/** No-op implementation for [ExtractedFieldDao]. */
private class NoOpExtractedFieldDao : ExtractedFieldDao {
    override fun observeByPrescription(prescriptionId: String): Flow<List<ExtractedFieldEntity>> =
        flowOf(emptyList())

    override suspend fun insertAll(fields: List<ExtractedFieldEntity>) = Unit

    override suspend fun updateReview(
        id: String,
        state: ReviewState,
        patientValue: String?,
        ts: Long,
    ) = Unit

    override suspend fun deleteByPrescription(prescriptionId: String) = Unit
}

/** [androidx.work.WorkerFactory] that injects fakes into [OcrWorker]. */
private class FakeOcrWorkerFactory(
    private val ocrService: OcrService,
    private val prescriptionDao: PrescriptionDao,
    private val extractedFieldDao: ExtractedFieldDao,
) : androidx.work.WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: androidx.work.WorkerParameters,
    ): ListenableWorker = OcrWorker(
        context = appContext,
        params = workerParameters,
        ocrService = ocrService,
        prescriptionDao = prescriptionDao,
        extractedFieldDao = extractedFieldDao,
    )
}
