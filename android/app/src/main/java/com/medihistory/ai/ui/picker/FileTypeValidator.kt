package com.medihistory.ai.ui.picker

/**
 * Validates that a file is one of the supported types before import.
 *
 * Security notes:
 * - MIME type is checked first (most reliable when provided by the system).
 * - Extension is used only as a fallback when MIME type is absent.
 * - Error messages NEVER include the file name or path to avoid leaking
 *   user data or internal storage layout.
 */
object FileTypeValidator {
    /** MIME types accepted for prescription import. */
    val ALLOWED_MIME_TYPES = setOf("image/jpeg", "image/png", "application/pdf")

    /** Lower-case file extensions accepted as fallback when MIME type is absent. */
    val ALLOWED_EXTENSIONS = setOf("jpg", "jpeg", "png", "pdf")

    /**
     * Returns true when the file is allowed for import.
     *
     * Resolution order:
     * 1. If [mimeType] is non-null and non-blank: check [ALLOWED_MIME_TYPES].
     * 2. Otherwise fall back to the extension of [fileName].
     *
     * Returns false when both [mimeType] and [fileName] are null/blank.
     */
    fun isAllowed(
        mimeType: String?,
        fileName: String?,
    ): Boolean {
        if (!mimeType.isNullOrBlank()) {
            return mimeType.lowercase() in ALLOWED_MIME_TYPES
        }
        val ext = fileName?.substringAfterLast('.', "")?.lowercase() ?: return false
        return ext in ALLOWED_EXTENSIONS
    }

    /**
     * Returns a plain-language error message for an unsupported file.
     *
     * The message intentionally omits the file name and path so that no
     * user-supplied string is echoed back through the UI.
     */
    @Suppress("UNUSED_PARAMETER")
    fun userFriendlyError(
        mimeType: String?,
        fileName: String?,
    ): String = "This file type is not supported. Please use a JPG, PNG, or PDF."
}
