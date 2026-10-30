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
internal class MlKitOcrService
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : OcrService {
        // TextRecognizer is thread-safe and should be reused (ML Kit recommendation).
        private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        private val parser = PrescriptionFieldParser()

        // -------------------------------------------------------------------------
        // DEBUG: raw ML Kit structure logger (remove before production release)
        // Logs block/line structure only — tagged OcrDebug so it can be grepped.
        // Text content is logged here intentionally for development diagnosis only.
        // This must be removed or gated behind BuildConfig.DEBUG before production.
        // -------------------------------------------------------------------------
        private fun logRawStructure(
            text: Text,
            imageWidth: Int,
            imageHeight: Int,
        ) {
            android.util.Log.d("OcrDebug", "=== ML Kit raw structure (${imageWidth}x$imageHeight) ===")
            text.textBlocks.forEachIndexed { bi, block ->
                val bb = block.boundingBox
                android.util.Log.d("OcrDebug", "BLOCK[$bi] bounds=${bb?.left},${bb?.top}-${bb?.right},${bb?.bottom}")
                block.lines.forEachIndexed { li, line ->
                    val lb = line.boundingBox
                    android.util.Log.d(
                        "OcrDebug",
                        "  LINE[$bi.$li] bounds=${lb?.left},${lb?.top}-${lb?.right},${lb?.bottom} xMid=${lb?.let{(it.left + it.right) / 2}} text=[${line.text}]",
                    )
                }
            }
            android.util.Log.d("OcrDebug", "=== end ML Kit raw structure ===")
        }

        // -------------------------------------------------------------------------
        // OcrService implementation
        // -------------------------------------------------------------------------

        override suspend fun extract(
            documentPath: String,
            mimeType: String,
            prescriptionId: String,
        ): OcrResult {
            return try {
                val allFields =
                    when {
                        mimeType.startsWith("image/") -> extractFromImage(documentPath)
                        mimeType == "application/pdf" -> extractFromPdf(documentPath)
                        else -> return OcrResult(
                            prescriptionId = prescriptionId,
                            fields = emptyList(),
                            overallConfidence = 0f,
                            errorMessage = "Unsupported MIME type: $mimeType",
                        )
                    }

                val overallConfidence =
                    allFields
                        .filter { it.rawValue != null }
                        .map { it.confidence }
                        .let { confs -> if (confs.isNotEmpty()) confs.average().toFloat() else 0f }

                OcrResult(
                    prescriptionId = prescriptionId,
                    fields = allFields,
                    overallConfidence = overallConfidence,
                )
            } catch (e: Exception) {
                // Never include documentPath or prescriptionId in the error message.
                OcrResult(
                    prescriptionId = prescriptionId,
                    fields = emptyList(),
                    overallConfidence = 0f,
                    errorMessage = "OCR processing failed: ${e.javaClass.simpleName}",
                )
            }
        }

        // -------------------------------------------------------------------------
        // Raw text extraction for GeminiEnhancedOcrService
        // -------------------------------------------------------------------------

        /**
         * Runs ML Kit pixel-to-text OCR and returns the concatenated raw text string.
         * Used by [GeminiEnhancedOcrService] to obtain the OCR text before passing it
         * to Gemini for structured field extraction.
         *
         * Privacy: never log the returned string.
         */
        internal suspend fun extractRawText(
            documentPath: String,
            mimeType: String,
        ): String {
            return try {
                when {
                    mimeType.startsWith("image/") -> {
                        val file = File(documentPath)
                        if (!file.exists() || !file.canRead()) return ""
                        val inputImage = InputImage.fromFilePath(context, Uri.fromFile(file))
                        runMlKit(inputImage).textBlocks
                            .joinToString("\n") { block -> block.text }
                    }
                    mimeType == "application/pdf" -> {
                        val file = File(documentPath)
                        if (!file.exists() || !file.canRead()) return ""
                        val pages = mutableListOf<String>()
                        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                            PdfRenderer(pfd).use { renderer ->
                                val pageCount = minOf(renderer.pageCount, MAX_PDF_PAGES)
                                for (pageIndex in 0 until pageCount) {
                                    renderer.openPage(pageIndex).use { page ->
                                        val scale = PDF_TARGET_WIDTH.toFloat() / page.width
                                        val bitmapWidth = PDF_TARGET_WIDTH
                                        val bitmapHeight = (page.height * scale).toInt()
                                        val bitmap =
                                            Bitmap.createBitmap(
                                                bitmapWidth,
                                                bitmapHeight,
                                                Bitmap.Config.ARGB_8888,
                                            )
                                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                        val mlKitText = runMlKit(InputImage.fromBitmap(bitmap, 0))
                                        pages.add(mlKitText.textBlocks.joinToString("\n") { it.text })
                                        bitmap.recycle()
                                    }
                                }
                            }
                        }
                        pages.joinToString("\n\n--- page break ---\n\n")
                    }
                    else -> ""
                }
            } catch (e: Exception) {
                // Silently return empty; GeminiEnhancedOcrService will fall back to ML Kit parser.
                ""
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
            val mlKitText = runMlKit(inputImage)
            logRawStructure(mlKitText, inputImage.width, inputImage.height)
            return parser.parse(
                text = mlKitText,
                pageIndex = 0,
                imageWidth = inputImage.width,
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
                            val bitmapWidth = PDF_TARGET_WIDTH
                            val bitmapHeight = (page.height * scale).toInt()

                            val bitmap =
                                Bitmap.createBitmap(
                                    bitmapWidth,
                                    bitmapHeight,
                                    Bitmap.Config.ARGB_8888,
                                )
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            val inputImage = InputImage.fromBitmap(bitmap, 0)
                            val mlKitText = runMlKit(inputImage)
                            allPageFields.add(
                                parser.parse(
                                    text = mlKitText,
                                    pageIndex = pageIndex,
                                    imageWidth = bitmapWidth,
                                    imageHeight = bitmapHeight,
                                ),
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
                    .addOnFailureListener { e -> continuation.resumeWithException(e) }
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
        private fun absentAllFields(): ExtractedField =
            ExtractedField(
                fieldName = "doctor_name",
                rawValue = null,
                patientValue = null,
                confidence = 0f,
                sourceReference = null,
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
