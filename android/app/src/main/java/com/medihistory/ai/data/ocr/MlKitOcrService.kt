package com.medihistory.ai.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * [OcrService] backed by Google ML Kit Text Recognition v2 (bundled, on-device).
 *
 * Privacy invariants enforced here:
 * - [documentPath] and [prescriptionId] are NEVER logged.
 * - Extracted field values ([ExtractedField.rawValue]) are NEVER logged.
 * - All processing runs entirely on-device — no data leaves the device.
 *
 * PDF support: Android [PdfRenderer] (API 21+, guaranteed at minSdk 26) renders
 * each page to a [Bitmap] at a fixed resolution, then ML Kit processes each page.
 * Results from all pages are merged; sourceReference = "page:N" for traceability.
 *
 * Failure policy:
 * - If the file cannot be read or ML Kit throws, returns [OcrResult] with
 *   [OcrResult.isSuccess] == false ([OcrResult.errorMessage] set to a non-sensitive
 *   description of the error class — never the document path or patient content).
 * - Low-confidence or absent fields are NOT failures; they are returned with
 *   [ExtractedField.rawValue] == null or a low confidence score.
 *
 * Thread-safety: [TextRecognizer] is thread-safe and reused across calls.
 */
internal class MlKitOcrService @Inject constructor(
    @ApplicationContext private val context: Context,
) : OcrService {

    // TextRecognizer is thread-safe and should be reused (ML Kit recommendation).
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val parser = PrescriptionFieldParser()

    // -------------------------------------------------------------------------
    // OcrService implementation
    // -------------------------------------------------------------------------

    override suspend fun extract(
        documentPath: String,
        mimeType: String,
        prescriptionId: String,
    ): OcrResult {
        return try {
            val allFields = when {
                mimeType.startsWith("image/") -> extractFromImage(documentPath)
                mimeType == "application/pdf"  -> extractFromPdf(documentPath)
                else -> return OcrResult(
                    prescriptionId = prescriptionId,
                    fields         = emptyList(),
                    overallConfidence = 0f,
                    errorMessage   = "Unsupported MIME type: $mimeType",
                )
            }

            val overallConfidence = allFields
                .filter { it.rawValue != null }
                .map { it.confidence }
                .let { confs -> if (confs.isNotEmpty()) confs.average().toFloat() else 0f }

            OcrResult(
                prescriptionId    = prescriptionId,
                fields            = allFields,
                overallConfidence = overallConfidence,
            )
        } catch (e: Exception) {
            // Never include documentPath or prescriptionId in the error message.
            OcrResult(
                prescriptionId    = prescriptionId,
                fields            = emptyList(),
                overallConfidence = 0f,
                errorMessage      = "OCR processing failed: ${e.javaClass.simpleName}",
            )
        }
    }

    // -------------------------------------------------------------------------
    // Image extraction (JPEG / PNG)
    // -------------------------------------------------------------------------

    private suspend fun extractFromImage(documentPath: String): List<ExtractedField> {
        val file = File(documentPath)
        if (!file.exists() || !file.canRead()) {
            return listOf(absentAllFields())
        }
        val inputImage = InputImage.fromFilePath(context, Uri.fromFile(file))
        val mlKitText  = runMlKit(inputImage)
        return parser.parse(
            text        = mlKitText,
            pageIndex   = 0,
            imageWidth  = inputImage.width,
            imageHeight = inputImage.height,
        )
    }

    // -------------------------------------------------------------------------
    // PDF extraction (PdfRenderer — API 21+)
    // -------------------------------------------------------------------------

    private suspend fun extractFromPdf(documentPath: String): List<ExtractedField> {
        val file = File(documentPath)
        if (!file.exists() || !file.canRead()) {
            return listOf(absentAllFields())
        }

        val allPageFields = mutableListOf<List<ExtractedField>>()

        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                val pageCount = minOf(renderer.pageCount, MAX_PDF_PAGES)
                for (pageIndex in 0 until pageCount) {
                    renderer.openPage(pageIndex).use { page ->
                        // Scale to ~300 DPI for a standard A4 page (2480×3508 at 300 DPI).
                        val scale = PDF_TARGET_WIDTH.toFloat() / page.width
                        val bitmapWidth  = PDF_TARGET_WIDTH
                        val bitmapHeight = (page.height * scale).toInt()

                        val bitmap = Bitmap.createBitmap(
                            bitmapWidth,
                            bitmapHeight,
                            Bitmap.Config.ARGB_8888,
                        )
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        val inputImage = InputImage.fromBitmap(bitmap, 0)
                        val mlKitText  = runMlKit(inputImage)
                        allPageFields.add(
                            parser.parse(
                                text        = mlKitText,
                                pageIndex   = pageIndex,
                                imageWidth  = bitmapWidth,
                                imageHeight = bitmapHeight,
                            )
                        )

                        bitmap.recycle()
                    }
                }
            }
        }

        return mergePageFields(allPageFields)
    }

    // -------------------------------------------------------------------------
    // ML Kit runner — wraps Task<Text> in a coroutine
    // -------------------------------------------------------------------------

    private suspend fun runMlKit(inputImage: InputImage): Text =
        suspendCoroutine { continuation ->
            recognizer.process(inputImage)
                .addOnSuccessListener { text -> continuation.resume(text) }
                .addOnFailureListener { e   -> continuation.resumeWithException(e) }
        }

    // -------------------------------------------------------------------------
    // Multi-page field merging
    // -------------------------------------------------------------------------

    /**
     * Merges field lists from multiple pages, preferring the highest-confidence
     * non-null rawValue per fieldName across all pages.
     */
    private fun mergePageFields(pageFields: List<List<ExtractedField>>): List<ExtractedField> {
        if (pageFields.isEmpty()) return emptyList()
        if (pageFields.size == 1) return pageFields[0]

        // Build a map: fieldName → best ExtractedField (highest confidence with non-null rawValue)
        val best = mutableMapOf<String, ExtractedField>()
        for (page in pageFields) {
            for (field in page) {
                val existing = best[field.fieldName]
                if (existing == null || (field.rawValue != null && field.confidence > existing.confidence)) {
                    best[field.fieldName] = field
                }
            }
        }
        return best.values.toList()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Returns a single absent sentinel field (used when the file cannot be read). */
    private fun absentAllFields(): ExtractedField = ExtractedField(
        fieldName        = "doctor_name",
        rawValue         = null,
        patientValue     = null,
        confidence       = 0f,
        sourceReference  = null,
    )

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    companion object {
        /** Process at most 2 pages of a PDF (a prescription is rarely more than 2 pages). */
        private const val MAX_PDF_PAGES = 2

        /**
         * Target bitmap width for PDF rendering in pixels.
         * At ~300 DPI on a standard A4 width (8.27 in) ≈ 2480 px.
         * Wider bitmaps improve OCR accuracy but increase memory use.
         */
        private const val PDF_TARGET_WIDTH = 2480
    }
}
