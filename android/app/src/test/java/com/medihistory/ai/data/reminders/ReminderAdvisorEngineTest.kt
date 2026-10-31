package com.medihistory.ai.data.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalTime

/**
 * Unit tests for [ReminderAdvisorEngine].
 *
 * Tests cover pattern recognition, meal modifiers, abbreviations, and null/empty inputs.
 * Naming convention: `test_[outcome]_[condition]`
 */
class ReminderAdvisorEngineTest {
    private lateinit var engine: ReminderAdvisorEngine

    @Before
    fun setUp() {
        engine = ReminderAdvisorEngine()
    }

    // -------------------------------------------------------------------------
    // Null / empty inputs
    // -------------------------------------------------------------------------

    @Test
    fun test_null_input_returns_null() {
        val result = engine.suggest(null)
        assertNull(
            "Null dosage instructions must return null",
            result,
        )
    }

    @Test
    fun test_empty_input_returns_null() {
        val result = engine.suggest("   ")
        assertNull(
            "Blank dosage instructions must return null",
            result,
        )
    }

    @Test
    fun test_unrecognised_pattern_returns_null() {
        val result = engine.suggest("take as directed")
        assertNull(
            "Unrecognised pattern must return null — never fabricate",
            result,
        )
    }

    // -------------------------------------------------------------------------
    // Abbreviation tests
    // -------------------------------------------------------------------------

    @Test
    fun test_od_abbreviation_returns_single_time_at_08_00() {
        val result = engine.suggest("OD")
        assertNotNull("OD must produce a suggestion", result)
        assertEquals("OD must suggest 1 time", 1, result!!.times.size)
        assertEquals("OD must suggest 08:00", LocalTime.of(8, 0), result.times[0])
    }

    @Test
    fun test_bid_returns_two_times() {
        val result = engine.suggest("BD")
        assertNotNull("BD must produce a suggestion", result)
        assertEquals("BD must suggest 2 times", 2, result!!.times.size)
        assertEquals("First BD time must be 08:00", LocalTime.of(8, 0), result.times[0])
        assertEquals("Second BD time must be 20:00", LocalTime.of(20, 0), result.times[1])
    }

    @Test
    fun test_tds_returns_three_times() {
        val result = engine.suggest("TDS")
        assertNotNull("TDS must produce a suggestion", result)
        assertEquals("TDS must suggest 3 times", 3, result!!.times.size)
        assertEquals("First TDS time must be 08:00", LocalTime.of(8, 0), result.times[0])
        assertEquals("Second TDS time must be 14:00", LocalTime.of(14, 0), result.times[1])
        assertEquals("Third TDS time must be 20:00", LocalTime.of(20, 0), result.times[2])
    }

    // -------------------------------------------------------------------------
    // Bedtime test
    // -------------------------------------------------------------------------

    @Test
    fun test_at_bedtime_returns_single_time_at_21_00() {
        val result = engine.suggest("at bedtime")
        assertNotNull("'at bedtime' must produce a suggestion", result)
        assertEquals("'at bedtime' must suggest 1 time", 1, result!!.times.size)
        assertEquals("Bedtime time must be 21:00", LocalTime.of(21, 0), result.times[0])
    }

    // -------------------------------------------------------------------------
    // Meal modifier tests
    // -------------------------------------------------------------------------

    @Test
    fun test_twice_daily_after_meals_returns_two_times_at_08_30_and_20_30() {
        val result = engine.suggest("twice daily after meals")
        assertNotNull("Twice daily after meals must produce a suggestion", result)
        assertEquals("After meals twice daily must suggest 2 times", 2, result!!.times.size)
        assertEquals(
            "First after-meals twice-daily time must be 08:30",
            LocalTime.of(8, 30),
            result.times[0],
        )
        assertEquals(
            "Second after-meals twice-daily time must be 20:30",
            LocalTime.of(20, 30),
            result.times[1],
        )
    }
}
