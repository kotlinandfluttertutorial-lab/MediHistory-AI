package com.medihistory.ai.ui.picker

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.medihistory.ai.data.model.OcrStatus
import com.medihistory.ai.data.model.Patient
import com.medihistory.ai.data.model.Prescription
import com.medihistory.ai.data.repository.PatientRepository
import com.medihistory.ai.data.repository.PrescriptionRepository
import com.medihistory.ai.data.session.SessionManager
import com.medihistory.ai.worker.OcrWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

// ---------------------------------------------------------------------------
// State
// ---------------------------------------------------------------------------

/**
 * Represents the lifecycle of a single file-import operation.
 *
 * States are mutually exclusive — only one is active at a time.
 */
sealed class ImportState {
    /** No import in progress; initial state. */
    object Idle : ImportState()

    /** File is being copied and hashed; UI should show a progress indicator. */
    object Processing : ImportState()

    /**
     * Copy completed successfully.
     *
     * @param filePath       Absolute path inside app-private storage (filesDir).
     * @param sha256         Hex-encoded SHA-256 digest of the copied file.
     * @param mimeType       Resolved MIME type of the imported file.
     * @param prescriptionId Room record ID — populated after the Prescription row is inserted.
     *                       Empty string while the DB write is still in flight.
     */
    data class Success(
        val filePath: String,
        val sha256: String,
        val mimeType: String,
        val prescriptionId: String = "",
    ) : ImportState()

    /**
     * Import failed.
     *
     * @param message Plain-language message suitable for display in a Snackbar.
     *   NEVER includes the original URI, internal path, or raw exception text.
     */
    data class Error(val message: String) : ImportState()
}

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

/**
 * Handles photo-picker and file-picker import for prescription documents.
 *
 * Security invariants:
 * - All files are copied to app-private storage (filesDir); shared external
 *   storage is never written.
 * - Originals are read via [android.content.ContentResolver] and never modified.
 * - A SHA-256 digest is computed during the copy; callers should verify it.
 * - Supported types: image/jpeg, image/png, application/pdf.
 * - Error messages never expose internal paths, URIs, or raw stack-trace text.
 * - Nothing relating to document content is written to Logcat.
 */
@HiltViewModel
class ImportViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val prescriptionRepository: PrescriptionRepository,
        private val patientRepository: PatientRepository,
        private val sessionManager: SessionManager,
        private val workManager: WorkManager,
    ) : ViewModel() {
        private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
        val importState: StateFlow<ImportState> = _importState.asStateFlow()

        // -------------------------------------------------------------------------
        // Public API
        // -------------------------------------------------------------------------

        /**
         * Called when the Android Photo Picker returns an image [Uri].
         *
         * Validates the MIME type, copies the file to app-private storage, and
         * computes a SHA-256 hash in one streaming pass.
         */
        fun onImageSelected(uri: Uri) {
            val resolvedMime = context.contentResolver.getType(uri)?.lowercase()
            handleUri(uri = uri, resolvedMimeType = resolvedMime, expectedCategory = "image")
        }

        /**
         * Called when the system file picker returns a [Uri].
         *
         * Validates the MIME type (must be application/pdf), copies to app-private
         * storage, and computes SHA-256.
         */
        fun onFileSelected(uri: Uri) {
            val resolvedMime = context.contentResolver.getType(uri)?.lowercase()
            handleUri(uri = uri, resolvedMimeType = resolvedMime, expectedCategory = "file")
        }

        /**
         * Called when the camera capture flow returns a confirmed image.
         *
         * [CameraViewModel] already wrote the file to app-private storage and computed its
         * SHA-256; this method only creates the Prescription row and enqueues the OCR worker.
         * It does NOT copy the file.
         *
         * Privacy: the file path is never written to Logcat.
         */
        fun onCameraImageConfirmed(
            filePath: String,
            sha256: String,
        ) {
            _importState.value = ImportState.Processing
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    val patientId = sessionManager.getPatientId()
                    ensurePatientRowExists(patientId)

                    val prescription =
                        Prescription(
                            id = UUID.randomUUID().toString(),
                            patientId = patientId,
                            documentPath = filePath,
                            documentMimeType = "image/jpeg",
                            documentHashSha256 = sha256,
                            ocrStatus = OcrStatus.PENDING,
                        )
                    try {
                        prescriptionRepository.insert(prescription)
                        workManager.enqueue(
                            OcrWorker.buildRequest(
                                prescriptionId = prescription.id,
                                patientId = patientId,
                                documentPath = filePath,
                                mimeType = "image/jpeg",
                            ),
                        )
                        _importState.value =
                            ImportState.Success(
                                filePath = filePath,
                                sha256 = sha256,
                                mimeType = "image/jpeg",
                                prescriptionId = prescription.id,
                            )
                    } catch (
                        @Suppress("TooGenericExceptionCaught") e: Exception,
                    ) {
                        Timber.tag("ImportViewModel").e(e.javaClass.simpleName)
                        _importState.value = ImportState.Error("Could not save prescription. Please try again.")
                    }
                }
            }
        }

        /** Resets state back to [ImportState.Idle]. */
        fun reset() {
            _importState.value = ImportState.Idle
        }

        // -------------------------------------------------------------------------
        // Private helpers
        // -------------------------------------------------------------------------

        private fun handleUri(
            uri: Uri,
            resolvedMimeType: String?,
            expectedCategory: String,
        ) {
            // Derive a display name without exposing paths
            val displayName = uri.lastPathSegment?.substringAfterLast('/')

            if (!FileTypeValidator.isAllowed(resolvedMimeType, displayName)) {
                _importState.value =
                    ImportState.Error(
                        FileTypeValidator.userFriendlyError(resolvedMimeType, displayName),
                    )
                return
            }

            val mimeType =
                resolvedMimeType
                    ?: extensionToMime(displayName?.substringAfterLast('.', ""))
                    ?: "application/octet-stream"

            _importState.value = ImportState.Processing

            viewModelScope.launch {
                val copyResult =
                    withContext(Dispatchers.IO) {
                        copyToPrivateStorage(uri, mimeType)
                    }
                if (copyResult is ImportState.Success) {
                    val patientId = sessionManager.getPatientId()

                    // Defensive guard: ensure a Patient row exists before inserting a Prescription.
                    // This covers the rare case where MainActivity's bootstrap LaunchedEffect
                    // hasn't completed yet (e.g. first app launch before login screen finishes).
                    ensurePatientRowExists(patientId)

                    val prescription =
                        Prescription(
                            id = UUID.randomUUID().toString(),
                            patientId = patientId,
                            documentPath = copyResult.filePath,
                            documentMimeType = copyResult.mimeType,
                            documentHashSha256 = copyResult.sha256,
                            ocrStatus = OcrStatus.PENDING,
                        )
                    try {
                        prescriptionRepository.insert(prescription)
                        // Enqueue the OCR worker immediately so extraction starts in the background
                        // while the patient is navigated to the review screen.
                        workManager.enqueue(
                            OcrWorker.buildRequest(
                                prescriptionId = prescription.id,
                                patientId = patientId,
                                documentPath = copyResult.filePath,
                                mimeType = copyResult.mimeType,
                            ),
                        )
                        _importState.value = copyResult.copy(prescriptionId = prescription.id)
                    } catch (
                        @Suppress("TooGenericExceptionCaught") e: Exception,
                    ) {
                        Timber.tag("ImportViewModel").e(e, "DB insert failed: ${e.javaClass.simpleName} — ${e.message}")
                        e.cause?.let { Timber.tag("ImportViewModel").e(it, "Caused by: ${it.javaClass.simpleName} — ${it.message}") }
                        _importState.value = ImportState.Error("Could not save prescription. Please try again.")
                    }
                } else {
                    val errorCause = (copyResult as? ImportState.Error)?.message ?: "Unknown copy failure"
                    Timber.tag("ImportViewModel").w("Import file copy failed, cause: $errorCause")
                    _importState.value = copyResult
                }
            }
        }

        /**
         * Ensures a Patient row exists for [patientId].
         *
         * This is a defensive guard against FK constraint failures. In normal operation
         * [LoginViewModel] and [MainActivity] already create this row; this call is a
         * no-op in those cases. It handles edge cases like:
         * - EncryptedSharedPreferences wiped between launches
         * - Very first launch before the auth bootstrap coroutine completes
         * - Test environments where patientId is a stub value
         */
        private suspend fun ensurePatientRowExists(patientId: String) {
            try {
                if (patientRepository.getById(patientId) == null) {
                    patientRepository.insert(
                        Patient(
                            id = patientId,
                            name = "User",
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                        ),
                    )
                }
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                // Non-fatal — if the insert fails the prescription insert will surface its own error.
                Timber.tag("ImportViewModel").w(e, "Could not ensure patient row: ${e.message}")
                e.cause?.let { Timber.tag("ImportViewModel").w(it, "Caused by: ${it.javaClass.simpleName} — ${it.message}") }
            }
        }

        /**
         * Copies the content at [uri] into [Context.getFilesDir]/prescriptions/,
         * computing SHA-256 during the read in a single pass.
         *
         * Returns [ImportState.Success] on success or [ImportState.Error] on any
         * failure.  Internal paths and exception messages are never surfaced to the
         * caller.
         */
        private fun copyToPrivateStorage(
            uri: Uri,
            mimeType: String,
        ): ImportState {
            return try {
                val ext = mimeTypeToExtension(mimeType)
                val destDir = File(context.filesDir, "prescriptions").also { it.mkdirs() }
                val destFile = File(destDir, "${UUID.randomUUID()}.$ext")

                val digest = MessageDigest.getInstance("SHA-256")

                context.contentResolver.openInputStream(uri)?.use { input ->
                    DigestInputStream(input, digest).use { digestInput ->
                        FileOutputStream(destFile).use { output ->
                            digestInput.copyTo(output)
                        }
                    }
                } ?: run {
                    Timber.tag("ImportViewModel").e("Could not open input stream for URI")
                    return ImportState.Error("Could not open the selected file. Please try again.")
                }

                val sha256 = digest.digest().joinToString("") { "%02x".format(it) }
                ImportState.Success(
                    filePath = destFile.absolutePath,
                    sha256 = sha256,
                    mimeType = mimeType,
                )
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                Timber.tag("ImportViewModel").e(e, "Copy to private storage failed: ${e.javaClass.simpleName}")
                e.cause?.let { Timber.tag("ImportViewModel").e(it, "Caused by: ${it.javaClass.simpleName} — ${it.message}") }
                ImportState.Error("Import failed. Please try again.")
            }
        }

        // -------------------------------------------------------------------------
        // MIME / extension helpers
        // -------------------------------------------------------------------------

        private fun mimeTypeToExtension(mimeType: String): String =
            when (mimeType) {
                "image/jpeg" -> "jpg"
                "image/png" -> "png"
                "application/pdf" -> "pdf"
                else -> "bin"
            }

        private fun extensionToMime(ext: String?): String? =
            when (ext?.lowercase()) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "pdf" -> "application/pdf"
                else -> null
            }
    }
