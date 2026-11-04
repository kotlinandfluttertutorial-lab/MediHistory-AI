package com.medihistory.ai.data.network

import com.medihistory.ai.data.security.SecurePreferences
import com.medihistory.ai.data.security.SecurePreferencesStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * OkHttp interceptor that adds the backend JWT Bearer token to every request.
 *
 * Security:
 * - The token is read from [SecurePreferencesStore] (EncryptedSharedPreferences).
 * - The token value is NEVER logged, printed, or serialised to any output.
 * - Requests without a token (auth endpoints) will not have the header — the
 *   backend returns 401 for protected endpoints, which surfaces as a network error.
 *
 * Privacy: intercept() never writes any request body or response body to a log sink.
 */
class AuthInterceptor @Inject constructor(
    private val securePreferences: SecurePreferencesStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = securePreferences.getString(SecurePreferences.KEY_ACCESS_TOKEN)

        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        return chain.proceed(request)
    }
}
