package com.medihistory.ai.data.ocr

/**
 * Provider-independent OCR service interface.
 *
 * Design rules:
 * - No cloud-provider imports in this interface. Concrete adapters (Google ML Kit,
 *   Google Cloud Vision, etc.) implement this in separate modules. The provider choice
 *   is an ADR-006 decision deferred until after MVP.
 * - Document text is UNTRUSTED INPUT. Callers must not log [OcrResult.fields] values,
 *   rawValues, or document paths. See medical-safety steering.
 * - The implementation MUST NOT infer or fabricate missing fields (dose, duration,
 *   diagnosis, allergy, medicine status). Absent fields are recorded with rawValue=null.
 * - Every [ExtractedField] returned must carry: rawValue, confidence, reviewState,
 *   and sourceReference (if the provider supports region attribution).
 */
interface OcrService {

    /**
     * Extract structured fields from a prescription document stored in app-private storage.
     *
     * @param documentPath Absolute path to the document file inside app-private storage
     *   (getFilesDir() or getExternalFilesDir()). Never a shared-storage path.
     * @param mimeType MIME type of the document: "image/jpeg", "image/png", or
     *   "application/pdf". Used to select the appropriate extraction strategy.
     * @param prescriptionId ID of the [com.medihistory.ai.data.model.Prescription]
     *   entity this document belongs to, for traceability in the returned [OcrResult].
     * @return [OcrResult] with all detected fields. [OcrResult.isSuccess] is false only
     *   on a total service failure; partial or low-confidence results are still a success.
     */
    suspend fun extract(
        documentPath: String,
        mimeType: String,
        prescriptionId: String,
    ): OcrResult
}
