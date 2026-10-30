package com.medihistory.ai.data.ocr

import kotlinx.coroutines.delay

/**
 * Stub [OcrService] for development and unit testing ONLY.
 *
 * NEVER use in production. This implementation:
 * - Returns synthetic field values with no real patient or medicine data.
 * - Uses only synthetic medicine names (e.g. "Synthamol 500mg") — never real brands.
 * - Returns LOW confidence on medicine fields to force the patient review flow.
 * - Returns absent fields (rawValue=null) for dose and frequency to verify the
 *   "record absent, do not fabricate" invariant.
 *
 * Replace with a real adapter when the OCR provider is selected (ADR-006).
 */
internal class StubOcrService : OcrService {
    override suspend fun extract(
        documentPath: String,
        mimeType: String,
        prescriptionId: String,
    ): OcrResult {
        // Simulate OCR processing latency without touching the document.
        // NEVER log documentPath, prescriptionId, or any field value here.
        delay(500L)

        return OcrResult(
            prescriptionId = prescriptionId,
            fields =
                listOf(
                    // High confidence — still PENDING_REVIEW until patient confirms.
                    ExtractedField(
                        fieldName = "doctor_name",
                        rawValue = "Dr. Synthetic",
                        patientValue = null,
                        confidence = 0.85f,
                        sourceReference = "page:1",
                    ),
                    ExtractedField(
                        fieldName = "prescription_date",
                        rawValue = "2025-07-01",
                        patientValue = null,
                        confidence = 0.90f,
                        sourceReference = "page:1",
                    ),
                    // LOW confidence — must be highlighted and reviewed before use.
                    ExtractedField(
                        fieldName = "medicine_name",
                        rawValue = "Synthamol 500mg",
                        patientValue = null,
                        confidence = 0.62f,
                        sourceReference = "page:1",
                    ),
                    // Absent fields — rawValue=null per spec. Never fabricate a value.
                    ExtractedField(
                        fieldName = "dose",
                        rawValue = null,
                        patientValue = null,
                        confidence = 0.0f,
                        sourceReference = null,
                    ),
                    ExtractedField(
                        fieldName = "frequency",
                        rawValue = null,
                        patientValue = null,
                        confidence = 0.0f,
                        sourceReference = null,
                    ),
                ),
            overallConfidence = 0.65f,
        )
    }
}
