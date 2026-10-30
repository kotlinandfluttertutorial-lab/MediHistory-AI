package com.medihistory.ai.data.di

import com.medihistory.ai.data.dao.PatientDao
import com.medihistory.ai.data.database.MediHistoryDatabase
import com.medihistory.ai.data.repository.PatientRepository
import com.medihistory.ai.data.repository.RoomPatientRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides patient-related dependencies.
 *
 * [PatientDao] is obtained from the singleton [MediHistoryDatabase] instance.
 * [PatientRepository] is bound to the Room-backed [RoomPatientRepository].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PatientModule {
    @Binds
    @Singleton
    abstract fun bindPatientRepository(impl: RoomPatientRepository): PatientRepository

    companion object {
        @Provides
        @Singleton
        fun providePatientDao(database: MediHistoryDatabase): PatientDao = database.patientDao()
    }
}
