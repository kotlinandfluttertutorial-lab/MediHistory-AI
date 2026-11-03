package com.medihistory.ai.data.auth

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that binds [AuthRepository] to [AuthRepositoryImpl].
 *
 * [AuthRepositoryImpl] is now backed by the FastAPI backend JWT system.
 * [MediHistoryApiService] is provided by [NetworkModule].
 * [SecurePreferencesStore] is provided by [SecurityModule].
 *
 * Firebase Auth has been removed — the backend handles authentication directly.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
