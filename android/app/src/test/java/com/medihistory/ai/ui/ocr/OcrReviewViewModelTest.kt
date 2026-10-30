package com.medihistory.ai.ui.ocr

import com.medihistory.ai.data.dao.ExtractedFieldDao
import com.medihistory.ai.data.dao.PrescriptionDao
import com.medihistory.ai.data.model.ExtractedFieldEntity
import com.medihistory.ai.data.model.OcrStatus
import com.medihistory.ai.data.ocr.OcrCorrectionEngine
import com.medihistory.ai.data.ocr.PostReviewProcessorInterface
import com.medihistory.ai.data.ocr.ReviewState
import com.medihistory.ai.data.repository.DoctorRepository
import com.medihistory.ai.data.security.SecurePreferencesStore
import com.medihistory.ai.data.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [OcrReviewViewModel].
 *
 * Uses [FakeExtractedFieldDao] and [FakePrescriptionDao] — no Room or I/O.
 * All fixtures use synthetic data only (no real patient/medicine names).
 *
 * Critical medical-safety tests:
 * - rawValue is NEVER changed by any ViewModel action.
 * - patientValue changes only when the patient confirms or edits.
 * - Fields with null rawValue cannot be confirmed (no-op).
 * - Review state machine transitions are enforced.
 *
 * Naming convention: `given_[state]_when_[action]_then_[outcome]`
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OcrReviewViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeFieldDao: FakeExtractedFieldDao
    private lateinit var fakePrescriptionDao: FakePrescriptionDao
    private lateinit var viewModel: OcrReviewViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeFieldDao = FakeExtractedFieldDao()
        fakePrescriptionDao = FakePrescriptionDao()
        viewModel = OcrReviewViewModel(
            extractedFieldDao = fakeFieldDao,
            prescriptionDao = fakePrescriptionDao,
            postReviewProcessor = NoOpPostReviewProcessor(),
            sessionManager = FakeSessionManager(),
            securePrefs = FakeSecurePrefsStore(),
            ocrCorrectionEngine = OcrCorrectionEngine(),
            doctorRepository = FakeDoctorRepository(),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fixtures
    // ─────────────────────────────────────────────────────────────────────────

    private fun syntheticField(
        id             : String      = "field-001",
        prescriptionId : String      = "rx-test-001",
        fieldName      : String      = "medicine_name",
        rawValue       : String?     = "Synthamol 500mg",
        patientValue   : String?     = null,
        confidence     : Float       = 0.85f,
        reviewState    : ReviewState = ReviewState.PENDING_REVIEW,
    ) = ExtractedFieldEntity(
        id             = id,
        prescriptionId = prescriptionId,
        fieldName      = fieldName,
        rawValue       = rawValue,
        patientValue   = patientValue,
        confidence     = confidence,
        reviewState    = reviewState,
        sourceReference = "page-1",
        createdAt      = 1_700_000_000_000L,
        updatedAt      = 1_700_000_000_000L,
    )

    // ─────────────────────────────────────────────────────────────────────────
    // load
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `given fields exist when load called then fields are emitted in UI state`() = runTest {
        val field = syntheticField()
        fakeFieldDao.setFields(listOf(field))

        viewModel.load("rx-test-001")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("isLoading must be false after load", state.isLoading)
        assertNull("error must be null on successful load", state.error)
        assertEquals("one field must be present", 1, state.fields.size)
    }

    @Test
    fun `given no fields when load called then fields list is empty`() = runTest {
        fakeFieldDao.setFields(emptyList())

        viewModel.load("rx-test-001")
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.fields.size)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // onConfirmField
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `given PENDING_REVIEW field when onConfirmField called then reviewState becomes REVIEWED`() =
        runTest {
            val field = syntheticField(id = "field-001", reviewState = ReviewState.PENDING_REVIEW)
            fakeFieldDao.setFields(listOf(field))

            viewModel.load("rx-test-001")
            advanceUntilIdle()

            viewModel.onConfirmField("field-001")
            advanceUntilIdle()

            val updated = fakeFieldDao.getField("field-001")
            assertEquals(ReviewState.REVIEWED, updated?.reviewState)
        }

    @Test
    fun `given PENDING_REVIEW field when onConfirmField called then patientValue equals rawValue`() =
        runTest {
            val field = syntheticField(rawValue = "Synthamol 500mg", patientValue = null)
            fakeFieldDao.setFields(listOf(field))

            viewModel.load("rx-test-001")
            advanceUntilIdle()

            viewModel.onConfirmField(field.id)
            advanceUntilIdle()

            val updated = fakeFieldDao.getField(field.id)
            assertEquals(
                "patientValue must be set to rawValue on confirm",
                "Synthamol 500mg",
                updated?.patientValue,
            )
        }

    @Test
    fun `given PENDING_REVIEW field when onConfirmField called then rawValue is NEVER changed`() =
        runTest {
            val field = syntheticField(rawValue = "Synthamol 500mg")
            fakeFieldDao.setFields(listOf(field))

            viewModel.load("rx-test-001")
            advanceUntilIdle()

            viewModel.onConfirmField(field.id)
            advanceUntilIdle()

            val updated = fakeFieldDao.getField(field.id)
            assertEquals(
                "CRITICAL SAFETY: rawValue must NEVER change — only patientValue changes",
                "Synthamol 500mg",
                updated?.rawValue,
            )
        }

    @Test
    fun `given field with null rawValue when onConfirmField called then it is a no-op`() =
        runTest {
            val field = syntheticField(rawValue = null, reviewState = ReviewState.PENDING_REVIEW)
            fakeFieldDao.setFields(listOf(field))

            viewModel.load("rx-test-001")
            advanceUntilIdle()

            viewModel.onConfirmField(field.id)
            advanceUntilIdle()

            val updated = fakeFieldDao.getField(field.id)
            assertEquals(
                "field with null rawValue must stay PENDING_REVIEW — no confirmation possible",
                ReviewState.PENDING_REVIEW,
                updated?.reviewState,
            )
        }

    // ─────────────────────────────────────────────────────────────────────────
    // onSaveEdit
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `given PENDING_REVIEW field when onSaveEdit called then patientValue equals editText`() =
        runTest {
            val field = syntheticField(rawValue = "Synthamol 500mg")
            fakeFieldDao.setFields(listOf(field))

            viewModel.load("rx-test-001")
            advanceUntilIdle()

            viewModel.onEditField(field.id)
            viewModel.onEditTextChange(field.id, "Testamol 250mg")
            viewModel.onSaveEdit(field.id)
            advanceUntilIdle()

            val updated = fakeFieldDao.getField(field.id)
            assertEquals("patientValue must reflect the edited text", "Testamol 250mg", updated?.patientValue)
            assertEquals(ReviewState.REVIEWED, updated?.reviewState)
        }

    @Test
    fun `given PENDING_REVIEW field when onSaveEdit called then rawValue is NEVER changed`() =
        runTest {
            val field = syntheticField(rawValue = "Synthamol 500mg")
            fakeFieldDao.setFields(listOf(field))

            viewModel.load("rx-test-001")
            advanceUntilIdle()

            viewModel.onEditField(field.id)
            viewModel.onEditTextChange(field.id, "Testamol 250mg")
            viewModel.onSaveEdit(field.id)
            advanceUntilIdle()

            val updated = fakeFieldDao.getField(field.id)
            assertEquals(
                "CRITICAL SAFETY: rawValue must NEVER change even after edit",
                "Synthamol 500mg",
                updated?.rawValue,
            )
        }

    // ─────────────────────────────────────────────────────────────────────────
    // onRejectField
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `given PENDING_REVIEW field when onRejectField called then reviewState becomes REJECTED`() =
        runTest {
            val field = syntheticField()
            fakeFieldDao.setFields(listOf(field))

            viewModel.load("rx-test-001")
            advanceUntilIdle()

            viewModel.onRejectField(field.id)
            advanceUntilIdle()

            val updated = fakeFieldDao.getField(field.id)
            assertEquals(ReviewState.REJECTED, updated?.reviewState)
        }

    @Test
    fun `given PENDING_REVIEW field when onRejectField called then field is NOT deleted`() =
        runTest {
            val field = syntheticField()
            fakeFieldDao.setFields(listOf(field))

            viewModel.load("rx-test-001")
            advanceUntilIdle()

            viewModel.onRejectField(field.id)
            advanceUntilIdle()

            assertNotNull(
                "Rejected field must still exist — it is never deleted",
                fakeFieldDao.getField(field.id),
            )
        }

    // ─────────────────────────────────────────────────────────────────────────
    // onUnreviewField
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `given REVIEWED field when onUnreviewField called then reviewState returns to PENDING_REVIEW`() =
        runTest {
            val field = syntheticField(reviewState = ReviewState.REVIEWED, patientValue = "Synthamol 500mg")
            fakeFieldDao.setFields(listOf(field))

            viewModel.load("rx-test-001")
            advanceUntilIdle()

            viewModel.onUnreviewField(field.id)
            advanceUntilIdle()

            val updated = fakeFieldDao.getField(field.id)
            assertEquals(ReviewState.PENDING_REVIEW, updated?.reviewState)
        }

    @Test
    fun `given REJECTED field when onUnreviewField called then reviewState returns to PENDING_REVIEW`() =
        runTest {
            val field = syntheticField(reviewState = ReviewState.REJECTED)
            fakeFieldDao.setFields(listOf(field))

            viewModel.load("rx-test-001")
            advanceUntilIdle()

            viewModel.onUnreviewField(field.id)
            advanceUntilIdle()

            val updated = fakeFieldDao.getField(field.id)
            assertEquals(ReviewState.PENDING_REVIEW, updated?.reviewState)
        }

    // ─────────────────────────────────────────────────────────────────────────
    // pendingCount
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `given one pending field when onConfirmField called then pendingCount decreases`() =
        runTest {
            val field = syntheticField()
            fakeFieldDao.setFields(listOf(field))

            viewModel.load("rx-test-001")
            advanceUntilIdle()

            val beforeCount = viewModel.uiState.value.pendingCount
            viewModel.onConfirmField(field.id)
            advanceUntilIdle()

            val afterCount = viewModel.uiState.value.pendingCount
            assertTrue(
                "pendingCount must decrease after confirmation (was $beforeCount, now $afterCount)",
                afterCount < beforeCount,
            )
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Gemini consent state machine
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `should_emit_UNKNOWN_when_consent_key_absent`() {
        // FakeSecurePrefsStore starts empty — no KEY_GEMINI_OCR_CONSENT stored.
        // The ViewModel reads consent state during construction.
        val prefs = FakeSecurePrefsStore()                      // nothing stored
        val vm = OcrReviewViewModel(
            extractedFieldDao    = fakeFieldDao,
            prescriptionDao      = fakePrescriptionDao,
            postReviewProcessor  = NoOpPostReviewProcessor(),
            sessionManager       = FakeSessionManager(),
            securePrefs          = prefs,
            ocrCorrectionEngine  = OcrCorrectionEngine(),
            doctorRepository     = FakeDoctorRepository(),
        )

        assertEquals(
            "geminiConsentState must be UNKNOWN when no consent key is stored",
            GeminiConsentState.UNKNOWN,
            vm.geminiConsentState.value,
        )
    }

    @Test
    fun `should_emit_GRANTED_when_consent_key_is_true`() {
        val prefs = FakeSecurePrefsStore()
        prefs.putString(com.medihistory.ai.data.security.SecurePreferences.KEY_GEMINI_OCR_CONSENT, "true")
        val vm = OcrReviewViewModel(
            extractedFieldDao    = fakeFieldDao,
            prescriptionDao      = fakePrescriptionDao,
            postReviewProcessor  = NoOpPostReviewProcessor(),
            sessionManager       = FakeSessionManager(),
            securePrefs          = prefs,
            ocrCorrectionEngine  = OcrCorrectionEngine(),
            doctorRepository     = FakeDoctorRepository(),
        )

        assertEquals(
            "geminiConsentState must be GRANTED when consent key is 'true'",
            GeminiConsentState.GRANTED,
            vm.geminiConsentState.value,
        )
    }

    @Test
    fun `should_emit_DECLINED_when_consent_key_is_false`() {
        val prefs = FakeSecurePrefsStore()
        prefs.putString(com.medihistory.ai.data.security.SecurePreferences.KEY_GEMINI_OCR_CONSENT, "false")
        val vm = OcrReviewViewModel(
            extractedFieldDao    = fakeFieldDao,
            prescriptionDao      = fakePrescriptionDao,
            postReviewProcessor  = NoOpPostReviewProcessor(),
            sessionManager       = FakeSessionManager(),
            securePrefs          = prefs,
            ocrCorrectionEngine  = OcrCorrectionEngine(),
            doctorRepository     = FakeDoctorRepository(),
        )

        assertEquals(
            "geminiConsentState must be DECLINED when consent key is 'false'",
            GeminiConsentState.DECLINED,
            vm.geminiConsentState.value,
        )
    }

    @Test
    fun `should_persist_true_and_emit_GRANTED_when_onGeminiConsentGranted_called`() {
        val prefs = FakeSecurePrefsStore()                      // starts with no consent
        val vm = OcrReviewViewModel(
            extractedFieldDao    = fakeFieldDao,
            prescriptionDao      = fakePrescriptionDao,
            postReviewProcessor  = NoOpPostReviewProcessor(),
            sessionManager       = FakeSessionManager(),
            securePrefs          = prefs,
            ocrCorrectionEngine  = OcrCorrectionEngine(),
            doctorRepository     = FakeDoctorRepository(),
        )
        assertEquals("Initial state must be UNKNOWN", GeminiConsentState.UNKNOWN, vm.geminiConsentState.value)

        vm.onGeminiConsentGranted()

        assertEquals(
            "geminiConsentState must be GRANTED after onGeminiConsentGranted()",
            GeminiConsentState.GRANTED,
            vm.geminiConsentState.value,
        )
        assertEquals(
            "SecurePrefs must store 'true' after onGeminiConsentGranted()",
            "true",
            prefs.getString(com.medihistory.ai.data.security.SecurePreferences.KEY_GEMINI_OCR_CONSENT),
        )
    }

    @Test
    fun `should_persist_false_and_emit_DECLINED_when_onGeminiConsentDeclined_called`() {
        val prefs = FakeSecurePrefsStore()                      // starts with no consent
        val vm = OcrReviewViewModel(
            extractedFieldDao    = fakeFieldDao,
            prescriptionDao      = fakePrescriptionDao,
            postReviewProcessor  = NoOpPostReviewProcessor(),
            sessionManager       = FakeSessionManager(),
            securePrefs          = prefs,
            ocrCorrectionEngine  = OcrCorrectionEngine(),
            doctorRepository     = FakeDoctorRepository(),
        )
        assertEquals("Initial state must be UNKNOWN", GeminiConsentState.UNKNOWN, vm.geminiConsentState.value)

        vm.onGeminiConsentDeclined()

        assertEquals(
            "geminiConsentState must be DECLINED after onGeminiConsentDeclined()",
            GeminiConsentState.DECLINED,
            vm.geminiConsentState.value,
        )
        assertEquals(
            "SecurePrefs must store 'false' after onGeminiConsentDeclined()",
            "false",
            prefs.getString(com.medihistory.ai.data.security.SecurePreferences.KEY_GEMINI_OCR_CONSENT),
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // rawValue immutability — comprehensive
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `given any action when performed then rawValue is NEVER changed`() = runTest {
        val originalRawValue = "Testocillin 250mg"
        val field = syntheticField(rawValue = originalRawValue)
        fakeFieldDao.setFields(listOf(field))

        viewModel.load("rx-test-001")
        advanceUntilIdle()

        // Confirm
        viewModel.onConfirmField(field.id)
        advanceUntilIdle()
        assertEquals("rawValue must not change after confirm", originalRawValue, fakeFieldDao.getField(field.id)?.rawValue)

        // Un-review back to pending
        viewModel.onUnreviewField(field.id)
        advanceUntilIdle()

        // Edit and save
        viewModel.onEditField(field.id)
        viewModel.onEditTextChange(field.id, "Testocillin 500mg")
        viewModel.onSaveEdit(field.id)
        advanceUntilIdle()
        assertEquals("rawValue must not change after edit+save", originalRawValue, fakeFieldDao.getField(field.id)?.rawValue)

        // Un-review again
        viewModel.onUnreviewField(field.id)
        advanceUntilIdle()

        // Reject
        viewModel.onRejectField(field.id)
        advanceUntilIdle()
        assertEquals("rawValue must not change after reject", originalRawValue, fakeFieldDao.getField(field.id)?.rawValue)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Fake DAOs — in-memory, no I/O, synthetic data only
// ─────────────────────────────────────────────────────────────────────────────

private class FakeExtractedFieldDao : ExtractedFieldDao {

    private val _fields = MutableStateFlow<List<ExtractedFieldEntity>>(emptyList())

    fun setFields(fields: List<ExtractedFieldEntity>) {
        _fields.value = fields
    }

    fun getField(id: String): ExtractedFieldEntity? =
        _fields.value.find { it.id == id }

    override fun observeByPrescription(prescriptionId: String): Flow<List<ExtractedFieldEntity>> =
        _fields.map { list -> list.filter { it.prescriptionId == prescriptionId } }

    override suspend fun insertAll(fields: List<ExtractedFieldEntity>) {
        _fields.update { current -> current + fields }
    }

    override suspend fun updateReview(
        id: String,
        state: ReviewState,
        patientValue: String?,
        ts: Long,
    ) {
        // CRITICAL: rawValue is NOT a parameter here — it is NEVER changed.
        _fields.update { current ->
            current.map { f ->
                if (f.id == id) {
                    f.copy(reviewState = state, patientValue = patientValue, updatedAt = ts)
                } else {
                    f
                }
            }
        }
    }

    override suspend fun deleteByPrescription(prescriptionId: String) {
        _fields.update { current -> current.filter { it.prescriptionId != prescriptionId } }
    }
}

private class FakePrescriptionDao : PrescriptionDao {

    private val patientIds = mutableMapOf<String, String>()

    fun seedPatientId(prescriptionId: String, patientId: String) {
        patientIds[prescriptionId] = patientId
    }

    override suspend fun getPatientId(id: String): String? = patientIds[id]

    override fun observeByPatient(patientId: String): Flow<List<com.medihistory.ai.data.model.Prescription>> =
        MutableStateFlow(emptyList())

    override suspend fun getById(id: String, patientId: String): com.medihistory.ai.data.model.Prescription? = null

    override suspend fun insert(prescription: com.medihistory.ai.data.model.Prescription) {}

    override suspend fun update(prescription: com.medihistory.ai.data.model.Prescription) {}

    override suspend fun delete(prescription: com.medihistory.ai.data.model.Prescription) {}

    override suspend fun updateOcrStatus(
        id: String,
        patientId: String,
        status: OcrStatus,
        updatedAt: Long,
    ) {}
}

/** No-op processor — tests verify ViewModel behaviour, not downstream DB writes. */
private class NoOpPostReviewProcessor : PostReviewProcessorInterface {
    override suspend fun process(prescriptionId: String, patientId: String) = Unit
}

private class FakeSecurePrefsStore : SecurePreferencesStore {
    private val map = mutableMapOf<String, String>()
    override fun getString(key: String): String? = map[key]
    override fun putString(key: String, value: String) { map[key] = value }
    override fun remove(key: String) { map.remove(key) }
    override fun clear() { map.clear() }
}

private class FakeSessionManager : SessionManager(
    securePreferences = object : SecurePreferencesStore {
        private val map = mutableMapOf("patient_id" to "test-patient-ocr")
        override fun getString(key: String): String? = map[key]
        override fun putString(key: String, value: String) { map[key] = value }
        override fun remove(key: String) { map.remove(key) }
        override fun clear() { map.clear() }
    }
)

private class FakeDoctorRepository : DoctorRepository {
    override fun observeByPatient(patientId: String): kotlinx.coroutines.flow.Flow<List<com.medihistory.ai.data.model.Doctor>> =
        MutableStateFlow(emptyList())
    override suspend fun getById(doctorId: String, patientId: String): com.medihistory.ai.data.model.Doctor? = null
    override suspend fun insert(doctor: com.medihistory.ai.data.model.Doctor) {}
    override suspend fun update(doctor: com.medihistory.ai.data.model.Doctor) {}
    override suspend fun delete(doctor: com.medihistory.ai.data.model.Doctor) {}
}
