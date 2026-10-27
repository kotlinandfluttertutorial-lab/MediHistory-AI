package com.medihistory.ai.data.auth

import android.util.Log
import com.medihistory.ai.BuildConfig
import com.medihistory.ai.data.security.SecurePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

/**
 * Production implementation of [AuthRepository].
 *
 * Security invariants:
 * - Access token stored exclusively via [SecurePreferences] (EncryptedSharedPreferences).
 * - Token value is NEVER written to any log at any level.
 * - HTTP calls use plain HttpURLConnection (Retrofit/OkHttp added in a later ticket).
 */
class AuthRepositoryImpl @Inject constructor(
    private val securePreferences: SecurePreferences,
) : AuthRepository {

    companion object {
        private const val TAG = "AuthRepository"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 15_000
    }

    override suspend fun login(email: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val baseUrl = BuildConfig.BACKEND_BASE_URL
            if (baseUrl.isBlank()) {
                Log.w(TAG, "Login failed: backend not configured")
                return@withContext Result.failure(Exception("Backend not configured"))
            }

            try {
                val url = URL("$baseUrl/auth/login")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.doOutput = true

                val body = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                }.toString()

                OutputStreamWriter(connection.outputStream).use { it.write(body) }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseBody = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(responseBody)
                    val token = json.getString("access_token")
                    // Token stored in EncryptedSharedPreferences — NEVER logged.
                    securePreferences.putString(SecurePreferences.KEY_ACCESS_TOKEN, token)
                    Log.i(TAG, "Login succeeded")
                    Result.success(Unit)
                } else {
                    Log.w(TAG, "Login failed")
                    Result.failure(Exception("Invalid credentials"))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Login failed")
                Result.failure(e)
            }
        }

    override suspend fun logout() {
        securePreferences.remove(SecurePreferences.KEY_ACCESS_TOKEN)
        Log.i(TAG, "Logout")
    }

    override fun isLoggedIn(): Boolean = !getAccessToken().isNullOrBlank()

    override fun getAccessToken(): String? =
        securePreferences.getString(SecurePreferences.KEY_ACCESS_TOKEN)
}
