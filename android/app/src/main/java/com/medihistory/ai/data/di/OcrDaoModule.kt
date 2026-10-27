package com.medihistory.ai.data.di

import com.medihistory.ai.data.dao.ExtractedFieldDao
import com.medihistory.ai.data.database.MediHistoryDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides Room DAOs consumed by the OCR worker. */
@Module
@InstallIn(SingletonComponent::class)
object OcrDaoModule {

    @Provides
    @Singleton
    fun provideExtractedFieldDao(database: MediHistoryDatabase): ExtractedFieldDao =
        database.extractedFieldDao()
}
