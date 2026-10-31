package com.medihistory.ai.data.ocr

import android.graphics.Matrix
import android.graphics.Rect
import com.google.mlkit.vision.text.Text
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PrescriptionFieldParserTest {
    private val parser = PrescriptionFieldParser()

    @Test
    fun parse_extracts_reviewable_fields_and_preserves_source_location() {
        val text =
            text(
                line("Dr. Synthetic", confidence = 0.91f),
                line("Date: 01/07/2025"),
                line("Rx:"),
                line("Synthamol", confidence = 0.87f),
                line("1-0-1 BD"),
            )

        val fields = parser.parse(text, pageIndex = 1, imageWidth = 1000)

        assertEquals("Dr. Synthetic", fields.first { it.fieldName == "doctor_name" }.rawValue)
        assertEquals(0.91f, fields.first { it.fieldName == "doctor_name" }.confidence)
        assertEquals("page:2,block:0", fields.first { it.fieldName == "doctor_name" }.sourceReference)
        assertEquals("01/07/2025", fields.first { it.fieldName == "prescription_date" }.rawValue)
        assertEquals("Synthamol", fields.first { it.fieldName == "medicine_name" }.rawValue)
        assertEquals("1-0-1", fields.first { it.fieldName == "dose" }.rawValue)
        assertEquals("BD", fields.first { it.fieldName == "frequency" }.rawValue)
        assertTrue(fields.all { it.patientValue == null && it.reviewState == ReviewState.PENDING_REVIEW })
    }

    @Test
    fun parse_ignores_right_column_and_returns_absent_values_without_fabrication() {
        val text =
            text(
                line("Synthamol 500mg", left = 800, right = 980),
                line("quantity 1 strip", left = 800, right = 980),
            )

        val fields = parser.parse(text, imageWidth = 1000)

        assertNull(fields.first { it.fieldName == "doctor_name" }.rawValue)
        assertNull(fields.first { it.fieldName == "prescription_date" }.rawValue)
        assertNull(fields.first { it.fieldName == "medicine_name" }.rawValue)
        assertNull(fields.first { it.fieldName == "dose" }.rawValue)
        assertNull(fields.first { it.fieldName == "frequency" }.rawValue)
        assertTrue(fields.all { it.confidence == 0f })
    }

    @Test
    fun parse_merges_dosage_continuation_and_keeps_medicine_name_clean() {
        val text =
            text(
                line("Rx:"),
                line("1. Synthamol (4"),
                line("MG ) (1 Strip)"),
            )

        val fields = parser.parse(text)

        assertEquals("Synthamol", fields.first { it.fieldName == "medicine_name" }.rawValue)
        assertNull(fields.first { it.fieldName == "dose" }.rawValue)
    }

    private fun text(vararg lines: Text.Line): Text {
        val block =
            Text.TextBlock(
                lines.joinToString("\n") { it.text },
                Rect(0, 0, 1000, 1000),
                emptyList<Any>(),
                "en",
                Matrix(),
                lines.toList(),
            )
        return Text(block.text, listOf(block))
    }

    private fun line(
        value: String,
        confidence: Float = 0f,
        left: Int = 0,
        right: Int = 600,
        top: Int = 40,
    ): Text.Line {
        val bounds = Rect(left, top, right, top + 20)
        val element =
            Text.Element(
                value,
                bounds,
                emptyList<Any>(),
                "en",
                Matrix(),
                confidence,
                0f,
                emptyList<Any>(),
            )
        return Text.Line(
            value,
            bounds,
            emptyList<Any>(),
            "en",
            Matrix(),
            listOf(element),
            confidence,
            0f,
        )
    }
}
