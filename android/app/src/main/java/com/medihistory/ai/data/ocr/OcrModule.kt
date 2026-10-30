package com.medihistory.ai.data.ocr

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds the [OcrService] implementation.
 *
 * Provides [MlKitOcrService] — Google ML Kit Text Recognition v2, running fully
 * on-device with no API key or network access required (ADR-006).
 *
 * The [TextRecognizer] inside [MlKitOcrService] is thread-safe and reusable;
 * [Singleton] scope ensures it is created once per app process.
 *
 * Security: no API keys, tokens, or secrets are initialised here.
 */
@Module
@InstallIn(SingletonComponent::class)
object OcrModule {

    @Provides
    @Singleton
    fun provideOcrService(@ApplicationContext context: Context): OcrService =
        MlKitOcrService(context)
}
