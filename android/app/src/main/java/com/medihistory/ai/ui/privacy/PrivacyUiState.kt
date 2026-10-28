package com.medihistory.ai.ui.privacy

import com.medihistory.ai.data.privacy.DataDeletion
import java.io.File

/**
 * UI state for the Privacy and Data screen.
 *
 * Privacy invariants:
 * - No patient data (names, medicine names, prescription content) is stored here.
 * - [exportFile] holds only the output [File] reference; the file itself lives in
 *   app-private cache storage and is shared via FileProvider only.
 * - [deletionResult] exposes only counts, not data content.
 */
data class PrivacyUiState(
    /** True while an export operation is in progress. */
    val isExporting: Boolean = false,

    /** True while a deletion operation is in progress. */
    val isDeleting: Boolean = false,

    /**
     * The exported file ready to share, or null if no export has been performed.
     * Must be shared via a FileProvider URI — never expose the path directly to
     * other apps.
     */
    val exportFile: File? = null,

    /**
     * True when the first deletion confirmation dialog should be shown.
     * This dialog explains what will be deleted and the cloud backup retention note.
     */
    val showDeleteStep1: Boolean = false,

    /**
     * True when the second (final) deletion confirmation dialog should be shown.
     * The user must type "DELETE" to proceed.
     */
    val showDeleteStep2: Boolean = false,

    /** The result of a completed deletion, or null if deletion has not occurred. */
    val deletionResult: DataDeletion.DeletionResult? = null,

    /** Error message to display, or null when no error is present. */
    val error: String? = null,
)
