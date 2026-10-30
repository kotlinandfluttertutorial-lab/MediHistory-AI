package com.medihistory.ai.ui.picker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [FileTypeValidator].
 *
 * Verifies MIME-type gating, extension fallback, and that error messages
 * never leak file names or paths.
 *
 * Naming convention: `should_[outcome]_when_[condition]`
 */
class FileTypeValidatorTest {
    // -------------------------------------------------------------------------
    // isAllowed — accepted MIME types
    // -------------------------------------------------------------------------

    @Test
    fun `should allow image_jpeg MIME type`() {
        assertTrue(FileTypeValidator.isAllowed("image/jpeg", null))
    }

    @Test
    fun `should allow image_png MIME type`() {
        assertTrue(FileTypeValidator.isAllowed("image/png", null))
    }

    @Test
    fun `should allow application_pdf MIME type`() {
        assertTrue(FileTypeValidator.isAllowed("application/pdf", null))
    }

    // -------------------------------------------------------------------------
    // isAllowed — rejected MIME types
    // -------------------------------------------------------------------------

    @Test
    fun `should reject application_msword MIME type with docx filename`() {
        assertFalse(FileTypeValidator.isAllowed("application/msword", "file.docx"))
    }

    @Test
    fun `should reject application_vnd_ms_excel MIME type with xls filename`() {
        assertFalse(FileTypeValidator.isAllowed("application/vnd.ms-excel", "file.xls"))
    }

    // -------------------------------------------------------------------------
    // isAllowed — extension fallback (MIME type is null)
    // -------------------------------------------------------------------------

    @Test
    fun `should allow jpg extension when MIME type is null`() {
        assertTrue(FileTypeValidator.isAllowed(null, "photo.jpg"))
    }

    @Test
    fun `should allow jpeg extension when MIME type is null`() {
        assertTrue(FileTypeValidator.isAllowed(null, "scan.jpeg"))
    }

    @Test
    fun `should allow png extension when MIME type is null`() {
        assertTrue(FileTypeValidator.isAllowed(null, "prescription.png"))
    }

    @Test
    fun `should allow pdf extension when MIME type is null`() {
        assertTrue(FileTypeValidator.isAllowed(null, "doc.pdf"))
    }

    @Test
    fun `should reject docx extension when MIME type is null`() {
        assertFalse(FileTypeValidator.isAllowed(null, "doc.docx"))
    }

    @Test
    fun `should reject xlsx extension when MIME type is null`() {
        assertFalse(FileTypeValidator.isAllowed(null, "data.xlsx"))
    }

    // -------------------------------------------------------------------------
    // isAllowed — edge: both null
    // -------------------------------------------------------------------------

    @Test
    fun `should reject when both MIME type and fileName are null`() {
        assertFalse(FileTypeValidator.isAllowed(null, null))
    }

    @Test
    fun `should reject when both MIME type and fileName are blank`() {
        assertFalse(FileTypeValidator.isAllowed("", ""))
    }

    // -------------------------------------------------------------------------
    // isAllowed — case insensitivity
    // -------------------------------------------------------------------------

    @Test
    fun `should allow MIME type regardless of case`() {
        assertTrue(FileTypeValidator.isAllowed("IMAGE/JPEG", null))
    }

    @Test
    fun `should allow file extension regardless of case`() {
        assertTrue(FileTypeValidator.isAllowed(null, "PHOTO.JPG"))
    }

    // -------------------------------------------------------------------------
    // userFriendlyError — message does not contain file name or path
    // -------------------------------------------------------------------------

    @Test
    fun `should not include internal path in error message`() {
        val message =
            FileTypeValidator.userFriendlyError(
                "application/msword",
                "/data/user/0/com.medihistory.ai/files/prescriptions/some_file.docx",
            )
        assertFalse(
            "Error message must not contain a slash (internal path leaked)",
            message.contains("/"),
        )
    }

    @Test
    fun `should not include original file name in error message`() {
        val syntheticFileName = "synthetic_prescription_test.docx"
        val message = FileTypeValidator.userFriendlyError("application/msword", syntheticFileName)
        assertFalse(
            "Error message must not echo back the file name",
            message.contains(syntheticFileName),
        )
    }

    // -------------------------------------------------------------------------
    // userFriendlyError — message mentions supported formats
    // -------------------------------------------------------------------------

    @Test
    fun `should mention JPG in error message`() {
        val message = FileTypeValidator.userFriendlyError("application/msword", "file.docx")
        assertTrue(
            "Error message must mention JPG so users know what is accepted",
            message.contains("JPG", ignoreCase = true),
        )
    }

    @Test
    fun `should mention PNG in error message`() {
        val message = FileTypeValidator.userFriendlyError("application/msword", "file.docx")
        assertTrue(
            "Error message must mention PNG so users know what is accepted",
            message.contains("PNG", ignoreCase = true),
        )
    }

    @Test
    fun `should mention PDF in error message`() {
        val message = FileTypeValidator.userFriendlyError("application/msword", "file.docx")
        assertTrue(
            "Error message must mention PDF so users know what is accepted",
            message.contains("PDF", ignoreCase = true),
        )
    }
}
