package com.medihistory.ai.data.di

import com.medihistory.ai.data.dao.SyncQueueDao
import com.medihistory.ai.data.database.MediHistoryDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides the [SyncQueueDao] singleton. */
@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideSyncQueueDao(database: MediHistoryDatabase): SyncQueueDao =
        database.syncQueueDao()
}
