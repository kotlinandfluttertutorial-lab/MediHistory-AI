package com.medihistory.ai.data.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMediHistoryDatabase(
        @ApplicationContext context: Context
    ): MediHistoryDatabase =
        Room.databaseBuilder(
            context,
            MediHistoryDatabase::class.java,
            MediHistoryDatabase.DATABASE_NAME
        ).build()
        // fallbackToDestructiveMigration() intentionally omitted — migrations are always explicit
}
