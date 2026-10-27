package com.medihistory.ai.data.security

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides security-related singletons.
 *
 * [SecurePreferences] is a singleton so that a single EncryptedSharedPreferences
 * instance is shared across the app, avoiding duplicate MasterKey initialisation.
 */
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideSecurePreferences(
        @ApplicationContext context: Context,
    ): SecurePreferences = SecurePreferences(context)
}
