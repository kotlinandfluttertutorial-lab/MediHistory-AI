package com.medihistory.ai.data.ocr

import com.medihistory.ai.data.model.Doctor
import com.medihistory.ai.data.model.DoctorSpecialty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [OcrCorrectionEngine].
 *
 * All fixtures use synthetic data only — no real patient, doctor, or medicine names.
 * Naming convention: `test_[outcome]_[condition]`
 */
class OcrCorrectionEngineTest {

    private lateinit var engine: OcrCorrectionEngine

    @Before
    fun setUp() {
        engine = OcrCorrectionEngine()
    }

    // -------------------------------------------------------------------------
    // Synthetic doctor fixture
    // -------------------------------------------------------------------------

    private fun syntheticDoctor(name: String) = Doctor(
        id           = "doc-synth-001",
        patientId    = "test-patient-alpha",
        name         = name,
        specialty    = DoctorSpecialty.OTHER,
        createdAt    = 1_700_000_000_000L,
        updatedAt    = 1_700_000_000_000L,
    )

    // -------------------------------------------------------------------------
    // Date field tests
    // -------------------------------------------------------------------------

    @Test
    fun test_date_format_iso_returns_iso_string() {
        // ISO date is already in canonical form — engine should return null (no change needed)
        val result = engine.suggest("prescription_date", "2025-07-01")
        // ISO already matches what we'd produce — null means "no change needed"
        assertNull(
            "ISO input that is already canonical should return null (no change needed)",
            result,
        )
    }

    @Test
    fun test_date_format_slash_dmy_parses_correctly() {
        val result = engine.suggest("prescription_date", "01/07/2025")
        assertEquals(
            "dd/MM/yyyy input must be converted to ISO format",
            "2025-07-01",
            result,
        )
    }

    @Test
    fun test_date_unrecognised_returns_null() {
        val result = engine.suggest("prescription_date", "not a date")
        assertNull(
            "Unrecognised date format must return null",
            result,
        )
    }

    // -------------------------------------------------------------------------
    // Clinical field safety tests
    // -------------------------------------------------------------------------

    @Test
    fun test_dose_field_always_returns_null() {
        val result = engine.suggest("dose", "500mg twice daily")
        assertNull(
            "dose field must always return null — clinical field safety",
            result,
        )
    }

    @Test
    fun test_frequency_field_always_returns_null() {
        val result = engine.suggest("frequency", "twice daily")
        assertNull(
            "frequency field must always return null — clinical field safety",
            result,
        )
    }

    // -------------------------------------------------------------------------
    // Doctor name tests
    // -------------------------------------------------------------------------

    @Test
    fun test_doctor_name_ocr_artefacts_cleaned() {
        // "Dr. Synthe3tic" has a digit OCR artefact — removing it yields "Dr. Synthetic"
        val result = engine.suggest("doctor_name", "Dr. Synthe3tic")
        assertEquals(
            "Doctor name OCR digit artefact should be stripped",
            "Dr. Synthetic",
            result,
        )
    }

    @Test
    fun test_doctor_name_levenshtein_match() {
        // rawValue has extra 'a' compared to known doctor name (distance = 1)
        val doctors = listOf(syntheticDoctor("Dr. Ananya"))
        val result = engine.suggest("doctor_name", "Dr. Ananyaa", knownDoctors = doctors)
        assertEquals(
            "Levenshtein distance 1 match should return the known doctor name",
            "Dr. Ananya",
            result,
        )
    }

    // -------------------------------------------------------------------------
    // Medicine name tests
    // -------------------------------------------------------------------------

    @Test
    fun test_medicine_name_trailing_punctuation_stripped() {
        val result = engine.suggest("medicine_name", "Synthamol.")
        assertEquals(
            "Trailing period should be stripped from medicine name",
            "Synthamol",
            result,
        )
    }

    @Test
    fun test_medicine_name_no_change_returns_null() {
        val result = engine.suggest("medicine_name", "Synthamol")
        assertNull(
            "Medicine name with no change needed should return null",
            result,
        )
    }
}
