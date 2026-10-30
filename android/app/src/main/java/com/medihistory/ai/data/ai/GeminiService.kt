package com.medihistory.ai.data.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.medihistory.ai.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around the Google Generative AI SDK.
 *
 * Provides a single [generate] method used by all AI agent features (F11, F15).
 *
 * Security invariants:
 * - The API key is read from [BuildConfig.GEMINI_API_KEY], which is sourced from
 *   `local.properties` (git-ignored) or a CI env var. It is NEVER hardcoded.
 * - Prompts and responses are NEVER written to Logcat at any log level.
 * - Document content, medicine names, and patient identifiers are never included
 *   in any error message that surfaces to Logcat.
 *
 * When [BuildConfig.GEMINI_API_KEY] is blank (CI without key, or missing local.properties),
 * [generate] returns null so callers can fall back gracefully.
 *
 * Privacy: this class must not log any patient data — not the prompt, not the response,
 * not any exception message that could contain patient content.
 */
@Singleton
class GeminiService
    @Inject
    constructor() {
        private val model: GenerativeModel? by lazy {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isBlank()) {
                null
            } else {
                GenerativeModel(
                    modelName = "gemini-1.5-flash",
                    apiKey = key,
                    generationConfig =
                        generationConfig {
                            temperature = 0.3f // lower = more deterministic, less hallucination
                            maxOutputTokens = 512
                        },
                )
            }
        }

        /**
         * Sends [prompt] to the Gemini API and returns the text response, or null on any failure.
         *
         * Callers must never include raw OCR values, prescription text, or patient identifiers
         * in [prompt] unless they have been sanitised and the patient has reviewed them.
         *
         * Returns null when:
         * - The API key is blank (key not configured)
         * - The network request fails
         * - The model returns an empty or blocked response
         *
         * Failures are silent — never propagated as exceptions, never logged with content.
         */
        suspend fun generate(prompt: String): String? {
            val m = model ?: return null
            return try {
                val response = m.generateContent(prompt)
                response.text?.trim()?.takeIf { it.isNotBlank() }
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                // Do NOT log e.message — it may echo prompt text containing patient data.
                null
            }
        }
    }
