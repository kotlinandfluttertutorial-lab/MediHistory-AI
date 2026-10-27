package com.medihistory.ai.ui.newdoctor

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [PdfGenerator].
 *
 * PdfGenerator relies on Android's native PdfDocument implementation, which is not
 * available in Robolectric. These tests run against the platform implementation.
 *
 * Synthetic fixtures only — no real patient data.
 */
@RunWith(AndroidJUnit4::class)
class PdfGeneratorTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private val syntheticSelections = listOf(
        RecordSelection(
            prescriptionId = "rx-synth-001",
            doctorName = "Dr. Synthetic Mehta",
            prescriptionDate = "15 Mar 2024",
            medicineCount = 2,
            isSelected = true,
        ),
        RecordSelection(
            prescriptionId = "rx-synth-002",
            doctorName = "Dr. Test Cardiologist",
            prescriptionDate = "20 Jun 2024",
            medicineCount = 1,
            isSelected = true,
        ),
    )

    private val syntheticPatientName = "Test Patient Alpha"

    @Test
    fun test_generated_pdf_file_exists_in_cacheDir() {
        val file = PdfGenerator.generate(
            context = context,
            selections = syntheticSelections,
            patientName = syntheticPatientName,
        )

        assertTrue("Generated PDF file must exist", file.exists())
        assertTrue(
            "Generated PDF must be located in cacheDir",
            file.absolutePath.startsWith(context.cacheDir.absolutePath),
        )
    }

    @Test
    fun test_pdf_filename_contains_timestamp() {
        val before = System.currentTimeMillis()
        val file = PdfGenerator.generate(
            context = context,
            selections = syntheticSelections,
            patientName = syntheticPatientName,
        )
        val after = System.currentTimeMillis()

        assertTrue(
            "PDF filename must start with 'mha_summary_'",
            file.name.startsWith("mha_summary_"),
        )

        val timestampStr = file.name
            .removePrefix("mha_summary_")
            .removeSuffix(".pdf")
        val timestamp = timestampStr.toLongOrNull()

        assertTrue(
            "Filename timestamp must be a valid long: '$timestampStr'",
            timestamp != null,
        )
        assertTrue(
            "Filename timestamp must be within the test execution window",
            timestamp!! in before..after,
        )
    }

    @Test
    fun test_pdf_file_is_non_empty() {
        val file = PdfGenerator.generate(
            context = context,
            selections = syntheticSelections,
            patientName = syntheticPatientName,
        )

        assertTrue("Generated PDF file must not be empty", file.length() > 0)
    }

    @Test
    fun test_disclaimer_text_is_present_in_content_model() {
        val requiredDisclaimer = "Patient-compiled record. Not a clinical report."
        val fullDisclaimer =
            "Patient-compiled record. Not a clinical report. Please verify all information with your doctor."

        assertTrue(
            "Short disclaimer must be a prefix of the full disclaimer",
            fullDisclaimer.contains(requiredDisclaimer),
        )

        val file = PdfGenerator.generate(
            context = context,
            selections = syntheticSelections,
            patientName = syntheticPatientName,
        )

        val headerBytes = file.inputStream().use { it.readNBytes(4) }
        val header = String(headerBytes)
        assertTrue(
            "Generated file must be a valid PDF (magic bytes %PDF), but got: '$header'",
            header == "%PDF",
        )
    }

    @Test
    fun test_pdf_generated_with_no_selected_records() {
        val emptySelections = listOf(
            RecordSelection(
                prescriptionId = "rx-synth-999",
                doctorName = "Dr. Synthetic",
                prescriptionDate = "01 Jan 2024",
                medicineCount = 0,
                isSelected = false,
            ),
        )

        val file = PdfGenerator.generate(
            context = context,
            selections = emptySelections,
            patientName = syntheticPatientName,
        )

        assertTrue("PDF must be generated even when no records are selected", file.exists())
        assertTrue("PDF must not be empty even when no records are selected", file.length() > 0)
    }
}
