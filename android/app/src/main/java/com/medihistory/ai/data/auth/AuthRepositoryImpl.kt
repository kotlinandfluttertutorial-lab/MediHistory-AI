package com.medihistory.ai.data.auth

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.medihistory.ai.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
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
 * - Access token stored exclusively in EncryptedSharedPreferences.
 * - Token value is NEVER written to any log at any level.
 * - HTTP calls use plain HttpURLConnection (Retrofit/OkHttp added in a later ticket).
 */
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AuthRepository {

    companion object {
        private const val TAG = "AuthRepository"
        private const val PREFS_FILE = "medihistory_auth_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 15_000
    }

    private val prefs by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            PREFS_FILE,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override suspend fun login(email: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val baseUrl = BuildConfig.BACKEND_BASE_URL
            if (baseUrl.isBlank()) {
                Log.w(TAG, "Login failed")
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
                    prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
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
        prefs.edit().remove(KEY_ACCESS_TOKEN).apply()
        Log.i(TAG, "Logout")
    }

    override fun isLoggedIn(): Boolean = !getAccessToken().isNullOrBlank()

    override fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
}
