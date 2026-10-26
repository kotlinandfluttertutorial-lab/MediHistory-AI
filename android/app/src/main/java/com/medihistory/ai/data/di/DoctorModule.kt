package com.medihistory.ai.data.di

import com.medihistory.ai.data.dao.DoctorDao
import com.medihistory.ai.data.database.MediHistoryDatabase
import com.medihistory.ai.data.repository.DoctorRepository
import com.medihistory.ai.data.repository.RoomDoctorRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides doctor-related dependencies.
 *
 * [DoctorDao] is provided from the [MediHistoryDatabase] instance (version 2 adds the
 * doctors table — see DatabaseModule for the MIGRATION_1_2 definition).
 * [DoctorRepository] is bound to the Room-backed [RoomDoctorRepository].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DoctorModule {

    @Binds
    @Singleton
    abstract fun bindDoctorRepository(impl: RoomDoctorRepository): DoctorRepository

    companion object {

        @Provides
        @Singleton
        fun provideDoctorDao(database: MediHistoryDatabase): DoctorDao =
            database.doctorDao()
    }
}
