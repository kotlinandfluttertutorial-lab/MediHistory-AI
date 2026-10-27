package com.medihistory.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Scaffold instrumented test — verifies the test runner is correctly configured.
 * Real UI/integration tests are added per-feature ticket.
 *
 * Requires a connected device or emulator.
 * Run with: ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @Test
    fun applicationPackageNameIsCorrect() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.medihistory.ai", appContext.packageName)
    }
}
