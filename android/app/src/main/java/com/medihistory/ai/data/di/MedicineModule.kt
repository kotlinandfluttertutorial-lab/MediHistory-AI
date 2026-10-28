package com.medihistory.ai.data.di

import com.medihistory.ai.data.dao.MedicineDao
import com.medihistory.ai.data.database.MediHistoryDatabase
import com.medihistory.ai.data.repository.MedicineRepository
import com.medihistory.ai.data.repository.RoomMedicineRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides medicine-related dependencies.
 *
 * [MedicineDao] is provided from the [MediHistoryDatabase] instance (version 4 adds the
 * medicines table — see DatabaseModule for the MIGRATION_3_4 definition).
 * [MedicineRepository] is bound to the Room-backed [RoomMedicineRepository].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MedicineModule {

    @Binds
    @Singleton
    abstract fun bindMedicineRepository(impl: RoomMedicineRepository): MedicineRepository

    companion object {

        @Provides
        @Singleton
        fun provideMedicineDao(database: MediHistoryDatabase): MedicineDao =
            database.medicineDao()
    }
}
