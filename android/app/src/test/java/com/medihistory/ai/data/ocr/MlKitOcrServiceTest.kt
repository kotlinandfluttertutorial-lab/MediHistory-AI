package com.medihistory.ai.data.ocr

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MlKitOcrServiceTest {
    private lateinit var context: Context
    private lateinit var service: MlKitOcrService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val unsafeField = unsafeClass.getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = unsafeField.get(null)
        service =
            unsafeClass.getMethod("allocateInstance", Class::class.java)
                .invoke(unsafe, MlKitOcrService::class.java) as MlKitOcrService
        MlKitOcrService::class.java.getDeclaredField("context").apply { isAccessible = true }
            .set(service, context)
        MlKitOcrService::class.java.getDeclaredField("parser").apply { isAccessible = true }
            .set(service, PrescriptionFieldParser())
    }

    @Test
    fun `unsupported mime returns non-sensitive failure`() =
        runTest {
            val result = service.extract("private-document", "text/plain", "rx-test")

            assertFalse(result.isSuccess)
            assertEquals("rx-test", result.prescriptionId)
            assertTrue(result.errorMessage!!.contains("text/plain"))
            assertFalse(result.errorMessage!!.contains("private-document"))
        }

    @Test
    fun `missing image and pdf return absent field rather than exception`() =
        runTest {
            for (mimeType in listOf("image/jpeg", "application/pdf")) {
                val result = service.extract("/missing/private-document", mimeType, "rx-test")

                assertTrue(result.isSuccess)
                assertEquals(1, result.fields.size)
                assertEquals("doctor_name", result.fields.single().fieldName)
                assertNull(result.fields.single().rawValue)
                assertEquals(0f, result.overallConfidence)
            }
        }

    @Test
    fun `raw text extraction returns blank for unsupported and missing files`() =
        runTest {
            assertEquals("", service.extractRawText("/missing/document", "text/plain"))
            assertEquals("", service.extractRawText("/missing/document", "image/jpeg"))
            assertEquals("", service.extractRawText("/missing/document", "application/pdf"))
        }

    @Test
    fun `invalid image returns safe processing error`() =
        runTest {
            val invalidImage = File.createTempFile("ocr-test", ".jpg", context.cacheDir)
            try {
                invalidImage.writeText("not an image")

                val result = service.extract(invalidImage.absolutePath, "image/jpeg", "rx-test")

                assertFalse(result.isSuccess)
                assertTrue(result.errorMessage!!.startsWith("OCR processing failed:"))
                assertFalse(result.errorMessage!!.contains(invalidImage.absolutePath))
            } finally {
                invalidImage.delete()
            }
        }
}
