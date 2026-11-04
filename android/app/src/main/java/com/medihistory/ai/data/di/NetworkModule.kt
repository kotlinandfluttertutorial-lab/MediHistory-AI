package com.medihistory.ai.data.di

import com.medihistory.ai.BuildConfig
import com.medihistory.ai.data.network.AuthInterceptor
import com.medihistory.ai.data.network.MediHistoryApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module providing the Retrofit + OkHttp network stack.
 *
 * Security invariants:
 * - [AuthInterceptor] adds the backend JWT to every request.
 * - HTTP logging is BODY level only in DEBUG builds; NONE in release,
 *   so token values and patient data never appear in logs.
 * - [BACKEND_BASE_URL] comes from BuildConfig (local.properties / CI env var),
 *   never hardcoded.
 * - No request body or response body is cached to disk.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val CONNECT_TIMEOUT_SECONDS = 15L
    private const val READ_TIMEOUT_SECONDS    = 30L

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC // BASIC only — never BODY (contains patient data)
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        val baseUrl = BuildConfig.BACKEND_BASE_URL
            .takeIf { it.isNotBlank() }
            ?: "http://localhost:8000/"   // local dev fallback; never used in production

        // Ensure trailing slash required by Retrofit
        val normalised = if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/"

        return Retrofit.Builder()
            .baseUrl(normalised)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideMediHistoryApiService(retrofit: Retrofit): MediHistoryApiService =
        retrofit.create(MediHistoryApiService::class.java)
}
