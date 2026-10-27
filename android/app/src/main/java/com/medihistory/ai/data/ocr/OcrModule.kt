package com.medihistory.ai.data.ocr

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds the [OcrService] implementation.
 *
 * Currently provides [StubOcrService] for development. Replace with a real
 * provider adapter when the OCR provider is selected (ADR-006). The interface
 * [OcrService] is provider-independent — no cloud imports leak into call sites.
 *
 * Security: no API keys, tokens, or secrets are initialised here.
 * All provider configuration must come from BuildConfig fields sourced from
 * local.properties or CI environment variables (never hardcoded).
 */
@Module
@InstallIn(SingletonComponent::class)
object OcrModule {

    @Provides
    @Singleton
    fun provideOcrService(): OcrService = StubOcrService()
    // TODO(ADR-006): replace StubOcrService with the selected provider adapter.
}
