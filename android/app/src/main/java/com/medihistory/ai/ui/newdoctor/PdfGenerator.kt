package com.medihistory.ai.ui.newdoctor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

/**
 * Generates a patient-compiled summary PDF using the Android [PdfDocument] API.
 *
 * Medical safety requirements enforced here:
 * - Every page shows the disclaimer: "Patient-compiled record. Not a clinical report."
 * - Page 1 shows a full disclaimer block that also instructs the patient to verify
 *   all information with their doctor.
 * - No AI-generated content is included. Only selected records as provided.
 * - Never infers or fabricates missing field values — absent values are shown as "Not recorded".
 *
 * Privacy: the generated file is written to app-private cache storage only.
 * It must never be written to shared external storage.
 * The caller is responsible for sharing via FileProvider (never direct file URI).
 */
object PdfGenerator {
    private const val PAGE_WIDTH = 595 // A4 points (72 dpi)
    private const val PAGE_HEIGHT = 842 // A4 points
    private const val MARGIN = 40f
    private const val LINE_HEIGHT = 18f
    private const val DISCLAIMER =
        "Patient-compiled record. Not a clinical report. Please verify all information with your doctor."
    private const val FOOTER_DISCLAIMER =
        "Patient-compiled record. Not a clinical report."

    /**
     * Generates a patient-compiled summary PDF and writes it to [Context.getCacheDir].
     *
     * @param context    Used to locate app-private cache storage. Never writes outside cache.
     * @param selections The records selected by the patient. Only selected records are included.
     * @param patientName Display name for the summary header (synthetic in tests).
     * @return The [File] written to cache. Caller must share via FileProvider.
     */
    fun generate(
        context: Context,
        selections: List<RecordSelection>,
        patientName: String,
    ): File {
        val timestamp = System.currentTimeMillis()
        val outputFile = File(context.cacheDir, "mha_summary_$timestamp.pdf")

        val document = PdfDocument()

        val titlePaint = buildPaint(18f, Typeface.BOLD, Color.BLACK)
        val headingPaint = buildPaint(13f, Typeface.BOLD, Color.BLACK)
        val bodyPaint = buildPaint(11f, Typeface.NORMAL, Color.BLACK)
        val disclaimerPaint = buildPaint(9f, Typeface.ITALIC, Color.DKGRAY)
        val errorPaint = buildPaint(10f, Typeface.BOLD, Color.RED)

        // -----------------------------------------------------------------------
        // Collect all lines to render, then paginate
        // -----------------------------------------------------------------------
        val contentLines = mutableListOf<PdfLine>()

        // Title block
        contentLines += PdfLine("MediHistory AI — Medical Summary", titlePaint)
        contentLines += PdfLine("Patient: $patientName", bodyPaint)
        contentLines += PdfLine("Generated: ${formatTimestamp(timestamp)}", bodyPaint)
        contentLines += PdfLine("", bodyPaint) // spacer

        // Full disclaimer block (page 1 area)
        contentLines += PdfLine(DISCLAIMER, disclaimerPaint)
        contentLines += PdfLine("", bodyPaint) // spacer
        contentLines += PdfLine("────────────────────────────────────────────────", bodyPaint)
        contentLines += PdfLine("", bodyPaint)

        // Selected records
        val selectedRecords = selections.filter { it.isSelected }
        if (selectedRecords.isEmpty()) {
            contentLines += PdfLine("No records selected.", bodyPaint)
        } else {
            selectedRecords.forEachIndexed { index, record ->
                contentLines += PdfLine("Prescription ${index + 1}", headingPaint)
                contentLines +=
                    PdfLine(
                        "Doctor: ${record.doctorName.ifBlank { "Not recorded" }}",
                        bodyPaint,
                    )
                contentLines +=
                    PdfLine(
                        "Date: ${record.prescriptionDate.ifBlank { "Not recorded" }}",
                        bodyPaint,
                    )
                val medLabel = if (record.medicineCount == 1) "1 medicine" else "${record.medicineCount} medicines"
                contentLines += PdfLine("Medicines: $medLabel", bodyPaint)
                contentLines += PdfLine("", bodyPaint) // spacer between records
            }
        }

        // -----------------------------------------------------------------------
        // Paginate and draw
        // -----------------------------------------------------------------------
        val usableHeight = PAGE_HEIGHT - (2 * MARGIN) - LINE_HEIGHT * 2 // reserve footer space
        var pageIndex = 0
        var yPosition = MARGIN + LINE_HEIGHT

        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
        var page = document.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        for (line in contentLines) {
            if (yPosition + LINE_HEIGHT > MARGIN + usableHeight) {
                // Draw footer and finish current page
                drawFooter(canvas, disclaimerPaint, pageIndex + 1)
                document.finishPage(page)

                // Start new page
                pageIndex++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                yPosition = MARGIN + LINE_HEIGHT
            }

            if (line.text.isNotEmpty()) {
                canvas.drawText(line.text.take(120), MARGIN, yPosition, line.paint)
            }
            yPosition += LINE_HEIGHT
        }

        // Draw footer and finish the last page
        drawFooter(canvas, disclaimerPaint, pageIndex + 1)
        document.finishPage(page)

        // Write to file
        FileOutputStream(outputFile).use { fos ->
            document.writeTo(fos)
        }
        document.close()

        return outputFile
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun drawFooter(
        canvas: Canvas,
        paint: Paint,
        pageNumber: Int,
    ) {
        val footerY = (PAGE_HEIGHT - MARGIN / 2).toFloat()
        canvas.drawText(FOOTER_DISCLAIMER, MARGIN, footerY, paint)
        canvas.drawText("Page $pageNumber", (PAGE_WIDTH - MARGIN - 40).toFloat(), footerY, paint)
    }

    private fun buildPaint(
        textSize: Float,
        style: Int,
        color: Int,
    ): Paint =
        Paint().apply {
            this.textSize = textSize
            this.typeface = Typeface.create(Typeface.DEFAULT, style)
            this.color = color
            isAntiAlias = true
        }

    private fun formatTimestamp(epochMs: Long): String {
        val date = java.util.Date(epochMs)
        return java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale.getDefault())
            .format(date)
    }

    /** Internal model for a single line of text with its associated [Paint]. */
    private data class PdfLine(val text: String, val paint: Paint)
}
