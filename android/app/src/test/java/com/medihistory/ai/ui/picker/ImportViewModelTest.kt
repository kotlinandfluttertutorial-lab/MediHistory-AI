package com.medihistory.ai.ui.picker

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Unit tests for [ImportViewModel].
 *
 * Uses Robolectric so that [ApplicationProvider.getApplicationContext] returns a real
 * Android context, enabling [ContentResolver] and app-private storage operations.
 * All file content is synthetic — no real images or PII.
 *
 * Naming convention: `should_[outcome]_when_[condition]`
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ImportViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Application
    private lateinit var viewModel: ImportViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        viewModel = ImportViewModel(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    fun `importState starts as Idle`() {
        assertTrue(
            "Initial importState must be Idle",
            viewModel.importState.value is ImportState.Idle,
        )
    }

    // -------------------------------------------------------------------------
    // Unsupported MIME type → Error state (synchronous path — no IO needed)
    // -------------------------------------------------------------------------

    @Test
    fun `should set Error state when onFileSelected receives unsupported MIME type`() = runTest {
        // A file:// URI referencing a synthetic .docx exercises the extension fallback path.
        // FileTypeValidator rejects .docx synchronously; no IO coroutine is launched.
        val syntheticDocx = File(context.cacheDir, "synthetic_test_import.docx")
        syntheticDocx.writeBytes(ByteArray(64) { it.toByte() })
        val fileUri = Uri.fromFile(syntheticDocx)

        viewModel.onFileSelected(fileUri)
        advanceUntilIdle()

        val state = viewModel.importState.value
        assertTrue(
            "State should be Error for an unsupported file type, but was: $state",
            state is ImportState.Error,
        )
        syntheticDocx.delete()
    }

    @Test
    fun `should set Error state when onImageSelected receives unsupported MIME type`() = runTest {
        val syntheticDocx = File(context.cacheDir, "synthetic_test_image_import.docx")
        syntheticDocx.writeBytes(ByteArray(64) { it.toByte() })
        val fileUri = Uri.fromFile(syntheticDocx)

        viewModel.onImageSelected(fileUri)
        advanceUntilIdle()

        val state = viewModel.importState.value
        assertTrue(
            "State should be Error for an unsupported file type, but was: $state",
            state is ImportState.Error,
        )
        syntheticDocx.delete()
    }

    // -------------------------------------------------------------------------
    // Error message must not expose internal paths
    // -------------------------------------------------------------------------

    @Test
    fun `should not expose internal path in error message`() = runTest {
        val syntheticDocx = File(context.cacheDir, "synthetic_path_test.docx")
        syntheticDocx.writeBytes(ByteArray(64) { it.toByte() })
        val fileUri = Uri.fromFile(syntheticDocx)

        viewModel.onFileSelected(fileUri)
        advanceUntilIdle()

        val state = viewModel.importState.value
        assertTrue("State should be Error", state is ImportState.Error)
        val message = (state as ImportState.Error).message

        assertFalse(
            "Error message must not contain a slash (internal path leaked): \"$message\"",
            message.contains("/"),
        )
        syntheticDocx.delete()
    }

    // -------------------------------------------------------------------------
    // reset() → back to Idle
    // -------------------------------------------------------------------------

    @Test
    fun `should return to Idle when reset is called after Error`() = runTest {
        val syntheticDocx = File(context.cacheDir, "synthetic_reset_test.docx")
        syntheticDocx.writeBytes(ByteArray(64) { it.toByte() })
        val fileUri = Uri.fromFile(syntheticDocx)

        viewModel.onFileSelected(fileUri)
        advanceUntilIdle()

        assertTrue(
            "Precondition: state should be Error before reset",
            viewModel.importState.value is ImportState.Error,
        )

        viewModel.reset()

        assertEquals(
            "After reset(), importState must be Idle",
            ImportState.Idle,
            viewModel.importState.value,
        )
        syntheticDocx.delete()
    }

    @Test
    fun `should remain Idle when reset is called on Idle state`() {
        viewModel.reset()
        assertEquals(
            "reset() on Idle state must remain Idle",
            ImportState.Idle,
            viewModel.importState.value,
        )
    }

    // -------------------------------------------------------------------------
    // Successful import path (file:// URI with valid jpg extension)
    //
    // These tests use runBlocking because the ViewModel dispatches the file-copy
    // work to Dispatchers.IO (a real thread pool). runBlocking allows withTimeout
    // to use real wall-clock time so the test waits for the IO thread to finish.
    // -------------------------------------------------------------------------

    @Test
    fun `should produce Success with non-blank filePath when importing a valid jpg`() {
        runBlocking {
        val syntheticJpg = File(context.cacheDir, "synthetic_prescription_test.jpg")
        syntheticJpg.writeBytes(ByteArray(1024) { it.toByte() })
        val fileUri = Uri.fromFile(syntheticJpg)

        viewModel.onImageSelected(fileUri)

        // Wait up to 10 s for the IO coroutine to emit Success or Error.
        val finalState = withTimeout(10_000) {
            viewModel.importState.first { it is ImportState.Success || it is ImportState.Error }
        }

        assertTrue(
            "State should be Success for a valid jpg, but was: $finalState",
            finalState is ImportState.Success,
        )
        val success = finalState as ImportState.Success
        assertTrue("filePath must not be blank", success.filePath.isNotBlank())
        syntheticJpg.delete()
        }
    }

    @Test
    fun `should produce Success with 64-char hex sha256 when importing a valid jpg`() {
        runBlocking {
        val syntheticJpg = File(context.cacheDir, "synthetic_sha256_test.jpg")
        syntheticJpg.writeBytes(ByteArray(1024) { it.toByte() })
        val fileUri = Uri.fromFile(syntheticJpg)

        viewModel.onImageSelected(fileUri)

        val finalState = withTimeout(10_000) {
            viewModel.importState.first { it is ImportState.Success || it is ImportState.Error }
        }

        assertTrue("State should be Success", finalState is ImportState.Success)
        val sha256 = (finalState as ImportState.Success).sha256
        assertTrue(
            "SHA-256 must be exactly 64 lowercase hex characters, but was: \"$sha256\"",
            sha256.matches(Regex("[0-9a-f]{64}")),
        )
        syntheticJpg.delete()
        }
    }

    @Test
    fun `should produce Success with correct mimeType when importing a valid jpg`() {
        runBlocking {
        val syntheticJpg = File(context.cacheDir, "synthetic_mime_test.jpg")
        syntheticJpg.writeBytes(ByteArray(512) { it.toByte() })
        val fileUri = Uri.fromFile(syntheticJpg)

        viewModel.onImageSelected(fileUri)

        val finalState = withTimeout(10_000) {
            viewModel.importState.first { it is ImportState.Success || it is ImportState.Error }
        }

        assertTrue("State should be Success", finalState is ImportState.Success)
        assertEquals(
            "MIME type must be image/jpeg for a .jpg file",
            "image/jpeg",
            (finalState as ImportState.Success).mimeType,
        )
        syntheticJpg.delete()
        }
    }

    @Test
    fun `should store imported file inside filesDir prescriptions directory`() {
        runBlocking {
        val syntheticJpg = File(context.cacheDir, "synthetic_storage_test.jpg")
        syntheticJpg.writeBytes(ByteArray(512) { it.toByte() })
        val fileUri = Uri.fromFile(syntheticJpg)

        viewModel.onImageSelected(fileUri)

        val finalState = withTimeout(10_000) {
            viewModel.importState.first { it is ImportState.Success || it is ImportState.Error }
        }

        assertTrue("State should be Success", finalState is ImportState.Success)
        val finalPath = (finalState as ImportState.Success).filePath
        val expectedDir = File(context.filesDir, "prescriptions").absolutePath
        assertTrue(
            "Imported file ($finalPath) must be inside $expectedDir",
            finalPath.startsWith(expectedDir),
        )
        syntheticJpg.delete()
        }
    }
}
