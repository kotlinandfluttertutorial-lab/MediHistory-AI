package com.medihistory.ai

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Scaffold unit test — verifies the JVM test configuration is working.
 * Real tests for business logic are added per-feature ticket.
 *
 * Uses synthetic values only; no real patient or medicine data.
 */
class ExampleUnitTest {
    @Test
    fun `scaffold test configuration is working`() {
        // Verify that the unit test runtime is correctly set up.
        val syntheticPatientId = "synthetic-patient-001"
        val expected = "synthetic-patient-001"
        assertEquals(expected, syntheticPatientId)
    }
}
