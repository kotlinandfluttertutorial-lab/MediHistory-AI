package com.medihistory.ai.ui.camera

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject

/** Sealed hierarchy representing the camera capture lifecycle. */
sealed class CaptureState {
    object Idle : CaptureState()

    data class Preview(val tempFilePath: String) : CaptureState()

    data class Error(val message: String) : CaptureState()
}

/**
 * ViewModel for [CameraScreen].
 *
 * Manages the capture lifecycle: Idle → Preview → (Idle on retake | confirmed on accept).
 * File operations are synchronous and small enough to run on the calling thread (test and
 * production use short-lived byte copies). A production upgrade could offload to Dispatchers.IO.
 */
@HiltViewModel
class CameraViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _captureState = MutableStateFlow<CaptureState>(CaptureState.Idle)
        val captureState: StateFlow<CaptureState> = _captureState.asStateFlow()

        /**
         * Called by [CameraPreviewContent] after CameraX writes the JPEG to a temp path.
         * Transitions from Idle → Preview.
         */
        fun onImageCaptured(tempFilePath: String) {
            _captureState.value = CaptureState.Preview(tempFilePath)
        }

        /**
         * Patient chose to retake. Deletes the temp file and returns to Idle.
         */
        fun onRetake() {
            val state = _captureState.value
            if (state is CaptureState.Preview) {
                File(state.tempFilePath).delete()
            }
            _captureState.value = CaptureState.Idle
        }

        /**
         * Patient confirmed the captured image. Copies the temp file to app-private storage,
         * computes its SHA-256 hex digest, and returns (finalPath, sha256).
         * Returns null and transitions to Error if the copy fails.
         */
        fun onConfirm(): Pair<String, String>? {
            val state = _captureState.value as? CaptureState.Preview ?: return null
            return try {
                val source = File(state.tempFilePath)
                val destDir = File(context.filesDir, "prescriptions").also { it.mkdirs() }
                val dest = File(destDir, "prescription_${System.currentTimeMillis()}.jpg")
                source.copyTo(dest, overwrite = true)
                source.delete()
                val sha256 = computeSha256(dest)
                _captureState.value = CaptureState.Idle
                Pair(dest.absolutePath, sha256)
            } catch (e: Exception) {
                Timber.e(e, "Failed to persist captured image")
                _captureState.value = CaptureState.Error(e.message ?: "Failed to save image")
                null
            }
        }

        /** Resets from any error state back to Idle so the patient can try again. */
        fun reset() {
            _captureState.value = CaptureState.Idle
        }

        // -------------------------------------------------------------------------
        // Private helpers
        // -------------------------------------------------------------------------

        private fun computeSha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { stream ->
                val buffer = ByteArray(8192)
                var read: Int
                while (stream.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
