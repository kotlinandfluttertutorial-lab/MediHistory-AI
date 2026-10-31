package com.medihistory.ai.ui.privacy

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.medihistory.ai.data.database.MediHistoryDatabase
import com.medihistory.ai.data.security.SecurePreferencesStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [PrivacyViewModel].
 *
 * Uses Robolectric for Android context and an in-memory Room database.
 * Uses [FakeSecurePreferencesStore] to avoid the AndroidKeyStore dependency.
 * All test data is synthetic — no real patient names.
 *
 * Naming convention: `given_[state]_when_[action]_then_[outcome]`
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PrivacyViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: PrivacyViewModel
    private lateinit var db: MediHistoryDatabase
    private lateinit var fakePrefs: FakeSecurePreferencesStore
    private lateinit var context: Context

    private val syntheticPatientId = "test-patient-privacy-alpha"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        db =
            Room.inMemoryDatabaseBuilder(context, MediHistoryDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        fakePrefs = FakeSecurePreferencesStore()
        viewModel =
            PrivacyViewModel(
                database = db,
                securePreferences = fakePrefs,
                context = context,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    // ── Delete Step 1 ────────────────────────────────────────────────────────

    @Test
    fun `given initial state when onDeleteStep1Clicked then showDeleteStep1 is true`() {
        viewModel.onDeleteStep1Clicked()

        assertTrue(
            "showDeleteStep1 must be true after onDeleteStep1Clicked",
            viewModel.uiState.value.showDeleteStep1,
        )
        assertFalse(
            "showDeleteStep2 must remain false",
            viewModel.uiState.value.showDeleteStep2,
        )
    }

    // ── Delete Cancel ────────────────────────────────────────────────────────

    @Test
    fun `given step1 shown when onDeleteCancelled then all dialogs are hidden`() {
        viewModel.onDeleteStep1Clicked()
        viewModel.onDeleteCancelled()

        assertFalse(
            "showDeleteStep1 must be false after cancel",
            viewModel.uiState.value.showDeleteStep1,
        )
        assertFalse(
            "showDeleteStep2 must be false after cancel",
            viewModel.uiState.value.showDeleteStep2,
        )
    }

    @Test
    fun `given step2 shown when onDeleteCancelled then all dialogs are hidden`() {
        viewModel.onDeleteStep1Clicked()
        viewModel.onDeleteStep2Triggered()
        viewModel.onDeleteCancelled()

        assertFalse(
            "showDeleteStep1 must be false after cancel",
            viewModel.uiState.value.showDeleteStep1,
        )
        assertFalse(
            "showDeleteStep2 must be false after cancel",
            viewModel.uiState.value.showDeleteStep2,
        )
    }

    // ── Delete Step 1 → Step 2 ───────────────────────────────────────────────

    @Test
    fun `given step1 shown when onDeleteStep2Triggered then step2 shown and step1 dismissed`() {
        viewModel.onDeleteStep1Clicked()
        viewModel.onDeleteStep2Triggered()

        assertFalse(
            "showDeleteStep1 must be false after advancing to step 2",
            viewModel.uiState.value.showDeleteStep1,
        )
        assertTrue(
            "showDeleteStep2 must be true after advancing to step 2",
            viewModel.uiState.value.showDeleteStep2,
        )
    }

    // ── Delete Step 2 confirmed ──────────────────────────────────────────────

    @Test
    fun `given step2 when onDeleteStep2Confirmed then deletionResult is set`() =
        runTest {
            viewModel.onDeleteStep1Clicked()
            viewModel.onDeleteStep2Triggered()
            viewModel.onDeleteStep2Confirmed(syntheticPatientId)
            viewModel.uiState.first {
                !it.isDeleting && (it.deletionResult != null || it.error != null)
            }

            assertNotNull(
                "deletionResult must be set after confirmed deletion",
                viewModel.uiState.value.deletionResult,
            )
        }

    @Test
    fun `given step2 when onDeleteStep2Confirmed then dialogs are dismissed`() =
        runTest {
            viewModel.onDeleteStep1Clicked()
            viewModel.onDeleteStep2Triggered()
            viewModel.onDeleteStep2Confirmed(syntheticPatientId)

            assertFalse(
                "showDeleteStep1 must be false after deletion",
                viewModel.uiState.value.showDeleteStep1,
            )
            assertFalse(
                "showDeleteStep2 must be false after deletion",
                viewModel.uiState.value.showDeleteStep2,
            )
        }

    @Test
    fun `given initial state when onDeleteStep2Confirmed without step1 then deletionResult is set`() =
        runTest {
            // The ViewModel does not enforce dialog ordering — only the UI does
            viewModel.onDeleteStep2Confirmed(syntheticPatientId)
            viewModel.uiState.first {
                !it.isDeleting && (it.deletionResult != null || it.error != null)
            }

            assertNotNull(
                "deletionResult must be set after confirmed deletion",
                viewModel.uiState.value.deletionResult,
            )
        }

    // ── Cloud backup retention explanation ───────────────────────────────────

    @Test
    fun `cloud backup retention explanation text is present and non-blank`() {
        val explanation = viewModel.cloudBackupRetentionExplanation
        assertTrue(
            "cloudBackupRetentionExplanation must not be blank",
            explanation.isNotBlank(),
        )
    }

    @Test
    fun `cloud backup retention explanation mentions cloud backup`() {
        val explanation = PrivacyViewModel.CLOUD_BACKUP_RETENTION_EXPLANATION
        assertTrue(
            "Retention explanation must mention cloud backup",
            explanation.contains("cloud backup", ignoreCase = true),
        )
    }

    @Test
    fun `cloud backup retention explanation mentions contacting support`() {
        val explanation = PrivacyViewModel.CLOUD_BACKUP_RETENTION_EXPLANATION
        assertTrue(
            "Retention explanation must mention contacting support",
            explanation.contains("Contact support", ignoreCase = true),
        )
    }

    @Test
    fun `cloud backup retention explanation mentions retention policy`() {
        val explanation = PrivacyViewModel.CLOUD_BACKUP_RETENTION_EXPLANATION
        assertTrue(
            "Retention explanation must mention retention policy",
            explanation.contains("retention policy", ignoreCase = true),
        )
    }

    // ── Export ───────────────────────────────────────────────────────────────

    @Test
    fun `given initial state when onExportClicked then isExporting becomes true then false`() =
        runTest {
            viewModel.onExportClicked(syntheticPatientId)

            assertTrue(
                "isExporting must be true while export is in progress",
                viewModel.uiState.value.isExporting,
            )

            viewModel.uiState.first { !it.isExporting }

            assertFalse(
                "isExporting must be false after export completes",
                viewModel.uiState.value.isExporting,
            )
        }

    @Test
    fun `given initial state when onExportClicked then exportFile is set after completion`() =
        runTest {
            viewModel.onExportClicked(syntheticPatientId)
            viewModel.uiState.first { !it.isExporting }

            assertNotNull(
                "exportFile must be set after export completes",
                viewModel.uiState.value.exportFile,
            )
        }

    @Test
    fun `given exportFile set when onExportFileConsumed then exportFile is null`() =
        runTest {
            viewModel.onExportClicked(syntheticPatientId)
            viewModel.uiState.first { !it.isExporting }

            viewModel.onExportFileConsumed()

            assertNull(
                "exportFile must be null after it has been consumed",
                viewModel.uiState.value.exportFile,
            )
        }

    @Test
    fun `given initial state when no action then deletionResult is null`() {
        assertNull(
            "deletionResult must be null in initial state",
            viewModel.uiState.value.deletionResult,
        )
    }
}

// ── Fake SecurePreferencesStore ────────────────────────────────────────────────

/**
 * In-memory fake for [SecurePreferencesStore].
 *
 * Avoids the AndroidKeyStore dependency (unavailable in JVM unit tests).
 * Uses plain [HashMap] — not for production use.
 */
internal class FakeSecurePreferencesStore : SecurePreferencesStore {
    private val store = HashMap<String, String>()
    var isCleared = false
        private set

    override fun putString(
        key: String,
        value: String,
    ) {
        store[key] = value
    }

    override fun getString(key: String): String? = store[key]

    override fun remove(key: String) {
        store.remove(key)
    }

    override fun clear() {
        store.clear()
        isCleared = true
    }
}
