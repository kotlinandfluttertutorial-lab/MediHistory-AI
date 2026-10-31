package com.medihistory.ai.data.ocr

import android.content.Context
import com.medihistory.ai.data.security.SecurePreferencesStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds the [OcrService] implementation.
 *
 * Provides [GeminiEnhancedOcrService] which uses ML Kit for pixel→text OCR and
 * Gemini 1.5 Flash for structured field extraction, falling back to the ML Kit
 * parser when Gemini is unavailable, the patient has not consented, or the feature
 * flag is off.
 *
 * Security: no API keys are initialised here. [BuildConfig.GEMINI_API_KEY] is read
 * inside [GeminiEnhancedOcrService] at call time, not at injection time.
 */
@Module
@InstallIn(SingletonComponent::class)
object OcrModule {
    /**
     * Provides [MlKitOcrService] as a [Singleton] so the [TextRecognizer] inside
     * it is created once per process (ML Kit recommendation).
     */
    @Provides
    @Singleton
    internal fun provideMlKitOcrService(
        @ApplicationContext context: Context,
    ): MlKitOcrService = MlKitOcrService(context)

    /**
     * Provides the [OcrService] interface implementation.
     * [GeminiEnhancedOcrService] wraps [MlKitOcrService] and the consent store.
     */
    @Provides
    @Singleton
    internal fun provideOcrService(
        @ApplicationContext context: Context,
        mlKitOcrService: MlKitOcrService,
        securePrefs: SecurePreferencesStore,
    ): OcrService = GeminiEnhancedOcrService(context, mlKitOcrService, securePrefs)
}
