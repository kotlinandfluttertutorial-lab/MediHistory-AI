package com.medihistory.ai.data.di

import com.medihistory.ai.data.dao.ExtractedFieldDao
import com.medihistory.ai.data.database.MediHistoryDatabase
import com.medihistory.ai.data.ocr.PostReviewProcessor
import com.medihistory.ai.data.ocr.PostReviewProcessorInterface
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides Room DAOs and use-cases consumed by the OCR worker and review flow. */
@Module
@InstallIn(SingletonComponent::class)
abstract class OcrDaoModule {

    @Binds
    @Singleton
    abstract fun bindPostReviewProcessor(impl: PostReviewProcessor): PostReviewProcessorInterface

    companion object {
        @Provides
        @Singleton
        fun provideExtractedFieldDao(database: MediHistoryDatabase): ExtractedFieldDao =
            database.extractedFieldDao()
    }
}
