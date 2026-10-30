package com.medihistory.ai.data.session

import com.medihistory.ai.data.security.SecurePreferencesStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SessionModule {

    @Provides
    @Singleton
    fun provideSessionManager(securePreferences: SecurePreferencesStore): SessionManager =
        SessionManager(securePreferences)
}
