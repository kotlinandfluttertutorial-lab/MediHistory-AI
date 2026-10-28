package com.medihistory.ai.ui.camera

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class CameraViewModelTest {
    private lateinit var viewModel: CameraViewModel

    @Before
    fun setUp() {
        viewModel = CameraViewModel(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun `captureState starts as Idle`() {
        assertTrue(viewModel.captureState.value is CaptureState.Idle)
    }

    @Test
    fun `onImageCaptured sets Preview state`() {
        val tempFile = File(ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir, "test.jpg")
        tempFile.writeBytes(ByteArray(1024) { it.toByte() })
        viewModel.onImageCaptured(tempFile.absolutePath)
        assertTrue(viewModel.captureState.value is CaptureState.Preview)
        tempFile.delete()
    }

    @Test
    fun `onRetake deletes temp file and resets to Idle`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val tempFile = File(context.cacheDir, "test_retake.jpg")
        tempFile.writeBytes(ByteArray(512))
        viewModel.onImageCaptured(tempFile.absolutePath)
        viewModel.onRetake()
        assertFalse("Temp file should be deleted", tempFile.exists())
        assertTrue(viewModel.captureState.value is CaptureState.Idle)
    }

    @Test
    fun `onConfirm copies to app-private storage and returns SHA-256`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val tempFile = File(context.cacheDir, "test_confirm.jpg")
        val content = ByteArray(1024) { (it % 256).toByte() }
        tempFile.writeBytes(content)
        viewModel.onImageCaptured(tempFile.absolutePath)
        val result = viewModel.onConfirm()
        assertNotNull(result)
        val (finalPath, sha256) = result!!
        assertTrue("Final path must be inside filesDir", finalPath.startsWith(context.filesDir.absolutePath))
        assertTrue("SHA-256 must be non-empty", sha256.isNotEmpty())
        assertTrue("SHA-256 must be hex string", sha256.matches(Regex("[0-9a-fA-F]{64}")))
        File(finalPath).delete()
        tempFile.delete()
    }
}
