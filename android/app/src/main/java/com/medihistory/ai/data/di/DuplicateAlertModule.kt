package com.medihistory.ai.data.di

import com.medihistory.ai.data.dao.DuplicateAlertDao
import com.medihistory.ai.data.database.MediHistoryDatabase
import com.medihistory.ai.data.repository.DuplicateAlertRepository
import com.medihistory.ai.data.repository.RoomDuplicateAlertRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides duplicate alert dependencies.
 *
 * [DuplicateAlertDao] is provided from the [MediHistoryDatabase] instance (version 6 adds
 * the duplicate_alerts table — see DatabaseModule for the MIGRATION_5_6 definition).
 * [DuplicateAlertRepository] is bound to the Room-backed [RoomDuplicateAlertRepository].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DuplicateAlertModule {

    @Binds
    @Singleton
    abstract fun bindDuplicateAlertRepository(
        impl: RoomDuplicateAlertRepository,
    ): DuplicateAlertRepository

    companion object {

        @Provides
        @Singleton
        fun provideDuplicateAlertDao(database: MediHistoryDatabase): DuplicateAlertDao =
            database.duplicateAlertDao()
    }
}
