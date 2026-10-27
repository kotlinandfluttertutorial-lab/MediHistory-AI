package com.medihistory.ai.data.di

import com.medihistory.ai.data.dao.DoseRecordDao
import com.medihistory.ai.data.dao.ReminderDao
import com.medihistory.ai.data.database.MediHistoryDatabase
import com.medihistory.ai.data.repository.ReminderRepository
import com.medihistory.ai.data.repository.RoomReminderRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides reminder and dose record dependencies.
 *
 * [ReminderDao] and [DoseRecordDao] are provided from the [MediHistoryDatabase] instance
 * (version 7 adds the reminders and dose_records tables — see DatabaseModule for
 * MIGRATION_6_7 definition).
 * [ReminderRepository] is bound to the Room-backed [RoomReminderRepository].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ReminderModule {

    @Binds
    @Singleton
    abstract fun bindReminderRepository(
        impl: RoomReminderRepository,
    ): ReminderRepository

    companion object {

        @Provides
        @Singleton
        fun provideReminderDao(database: MediHistoryDatabase): ReminderDao =
            database.reminderDao()

        @Provides
        @Singleton
        fun provideDoseRecordDao(database: MediHistoryDatabase): DoseRecordDao =
            database.doseRecordDao()
    }
}
