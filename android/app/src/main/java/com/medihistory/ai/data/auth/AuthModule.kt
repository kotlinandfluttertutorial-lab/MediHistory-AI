package com.medihistory.ai.data.auth

import com.google.firebase.auth.FirebaseAuth
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides Firebase Auth dependencies.
 *
 * [FirebaseAuth] is a singleton — FirebaseAuth.getInstance() returns the same object every
 * time, but wrapping it in a Hilt singleton ensures it is testable via injection.
 *
 * [AuthRepository] is bound to [AuthRepositoryImpl] which delegates to [FirebaseAuth].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    }
}
