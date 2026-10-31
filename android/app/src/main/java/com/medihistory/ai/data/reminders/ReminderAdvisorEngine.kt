package com.medihistory.ai.data.reminders

import java.time.LocalTime
import javax.inject.Inject

/**
 * Data class representing a suggested set of reminder times for a medicine.
 *
 * Medical safety: these are suggestions only — the patient must confirm before any
 * reminder is created. The engine never infers or fabricates clinical dosing.
 *
 * @param times         List of suggested [LocalTime] values (08:00, 14:00, etc.)
 * @param sourcePattern The dosage instruction pattern that was matched (for display).
 */
data class ReminderSuggestion(
    val times: List<LocalTime>,
    val sourcePattern: String,
)

/**
 * Parses a dosage instruction string and suggests appropriate reminder times.
 *
 * Medical safety invariants:
 * - Returns null when no pattern is recognised — never fabricates a suggestion.
 * - Suggestions are advisory only; the patient must confirm every time before use.
 * - No clinical interpretation is made. Timing suggestions are practical defaults
 *   (morning, afternoon, bedtime) — not medical advice.
 * - After-meal modifiers shift times slightly to post-meal windows.
 * - Before-meal modifiers shift times slightly to pre-meal windows.
 *
 * This is a pure, synchronous class. Inject via Hilt.
 */
class ReminderAdvisorEngine
    @Inject
    constructor() {
        /**
         * Suggests reminder times for [dosageInstructions].
         *
         * @param dosageInstructions The dosage instruction text (from a REVIEWED OCR field).
         * @return [ReminderSuggestion] with suggested times, or null if no pattern matches.
         */
        fun suggest(dosageInstructions: String?): ReminderSuggestion? {
            if (dosageInstructions.isNullOrBlank()) return null

            val lower = dosageInstructions.lowercase()

            // Detect meal modifier first
            val afterMeals =
                lower.contains("after meals") || lower.contains("after food") ||
                    lower.contains("pc") || lower.contains("post meal")
            val beforeMeals =
                lower.contains("before meals") || lower.contains("before food") ||
                    lower.contains("ac") || lower.contains("pre meal")

            // Detect frequency
            val frequency = detectFrequency(lower) ?: return null

            val times =
                when (frequency) {
                    Frequency.ONCE_DAILY -> {
                        when {
                            afterMeals -> listOf(LocalTime.of(8, 30))
                            beforeMeals -> listOf(LocalTime.of(8, 0))
                            else -> listOf(LocalTime.of(8, 0))
                        }
                    }
                    Frequency.TWICE_DAILY -> {
                        when {
                            afterMeals -> listOf(LocalTime.of(8, 30), LocalTime.of(20, 30))
                            beforeMeals -> listOf(LocalTime.of(7, 45), LocalTime.of(20, 0))
                            else -> listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))
                        }
                    }
                    Frequency.THREE_TIMES_DAILY -> {
                        when {
                            afterMeals ->
                                listOf(
                                    LocalTime.of(8, 30),
                                    LocalTime.of(13, 30),
                                    LocalTime.of(20, 30),
                                )
                            beforeMeals ->
                                listOf(
                                    LocalTime.of(7, 45),
                                    LocalTime.of(13, 0),
                                    LocalTime.of(20, 0),
                                )
                            else ->
                                listOf(
                                    LocalTime.of(8, 0),
                                    LocalTime.of(14, 0),
                                    LocalTime.of(20, 0),
                                )
                        }
                    }
                    Frequency.BEDTIME -> listOf(LocalTime.of(21, 0))
                }

            return ReminderSuggestion(times = times, sourcePattern = dosageInstructions)
        }

        // -----------------------------------------------------------------------
        // Private helpers
        // -----------------------------------------------------------------------

        private enum class Frequency {
            ONCE_DAILY,
            TWICE_DAILY,
            THREE_TIMES_DAILY,
            BEDTIME,
        }

        private fun detectFrequency(lower: String): Frequency? =
            when {
                // Bedtime patterns — check before generic "once daily" to avoid false match on "0-0-1"
                lower.contains("at bedtime") ||
                    lower.contains("bedtime") ||
                    lower.contains(" hs") ||
                    lower == "hs" ||
                    lower.contains("at night") ||
                    (lower.contains("night") && !lower.contains("overnight")) ||
                    lower.contains("0-0-1") ->
                    Frequency.BEDTIME

                // Three times daily
                lower.contains("three times daily") ||
                    lower.contains("three times a day") ||
                    lower.contains("thrice daily") ||
                    lower.contains(" tds") || lower.startsWith("tds") ||
                    lower.contains(" tid") || lower.startsWith("tid") ||
                    lower.contains("1-1-1") ->
                    Frequency.THREE_TIMES_DAILY

                // Twice daily
                lower.contains("twice daily") ||
                    lower.contains("twice a day") ||
                    lower.contains("two times daily") ||
                    lower.contains(" bd") || lower.startsWith("bd") ||
                    lower.contains(" bid") || lower.startsWith("bid") ||
                    lower.contains("1-0-1") ||
                    lower.contains("1-1-0") ->
                    Frequency.TWICE_DAILY

                // Once daily — check after twice/thrice to avoid substring collision
                lower.contains("once daily") ||
                    lower.contains("once a day") ||
                    lower.contains("one time daily") ||
                    lower.contains(" od") || lower.startsWith("od") ||
                    lower.contains("1-0-0") ||
                    (
                        lower.contains("morning") &&
                            !lower.contains("before meals") &&
                            !lower.contains("after meals")
                    ) ->
                    Frequency.ONCE_DAILY

                else -> null
            }
    }
