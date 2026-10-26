package com.medihistory.ai.data.di

import com.medihistory.ai.data.dao.PrescriptionDao
import com.medihistory.ai.data.database.MediHistoryDatabase
import com.medihistory.ai.data.repository.PrescriptionRepository
import com.medihistory.ai.data.repository.RoomPrescriptionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides prescription-related dependencies.
 *
 * [PrescriptionDao] is provided from the [MediHistoryDatabase] instance (version 4 adds the
 * prescriptions table — see DatabaseModule for the MIGRATION_3_4 definition).
 * [PrescriptionRepository] is bound to the Room-backed [RoomPrescriptionRepository].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PrescriptionModule {

    @Binds
    @Singleton
    abstract fun bindPrescriptionRepository(
        impl: RoomPrescriptionRepository,
    ): PrescriptionRepository

    companion object {

        @Provides
        @Singleton
        fun providePrescriptionDao(database: MediHistoryDatabase): PrescriptionDao =
            database.prescriptionDao()
    }
}
