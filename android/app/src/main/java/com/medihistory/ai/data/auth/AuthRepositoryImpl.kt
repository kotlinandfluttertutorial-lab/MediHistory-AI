package com.medihistory.ai.data.auth

import com.medihistory.ai.data.network.MediHistoryApiService
import com.medihistory.ai.data.network.dto.LoginRequest
import com.medihistory.ai.data.network.dto.RegisterRequest
import com.medihistory.ai.data.security.SecurePreferences
import com.medihistory.ai.data.security.SecurePreferencesStore
import javax.inject.Inject

/**
 * Backend JWT implementation of [AuthRepository].
 *
 * Replaces the previous Firebase Auth implementation.
 * Authenticates against the FastAPI backend (`POST /api/v1/auth/register`, `/login`).
 * The backend returns a JWT that is stored in [EncryptedSharedPreferences] via
 * [SecurePreferencesStore] so [SessionManager.isSessionActive] becomes real.
 *
 * Security invariants:
 * - Passwords are NEVER stored — only the access token and patient ID are persisted.
 * - Tokens are stored exclusively in EncryptedSharedPreferences (KEY_ACCESS_TOKEN).
 * - Email is stored only as a display hint (KEY_DISPLAY_EMAIL) — never used as a credential.
 * - Error messages exposed to the UI never contain raw exception text or server details.
 * - Nothing is logged here — not the token, not the email, not the response body.
 *
 * Privacy: this class never writes patient data, email, or token to Logcat.
 */
class AuthRepositoryImpl @Inject constructor(
    private val apiService: MediHistoryApiService,
    private val securePreferences: SecurePreferencesStore,
) : AuthRepository {

    companion object {
        /** Display-only email — never a credential. */
        private const val KEY_DISPLAY_EMAIL = "display_email"
        /** Backend UUID assigned at registration. */
        private const val KEY_PATIENT_UUID  = "patient_uuid"
    }

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val response = apiService.login(LoginRequest(email = email.trim(), password = password))
            if (response.isSuccessful) {
                val body = response.body() ?: return Result.failure(Exception("Sign-in failed. Please try again."))
                storeSession(accessToken = body.accessToken, email = email.trim())
                Result.success(Unit)
            } else {
                when (response.code()) {
                    401 -> Result.failure(Exception("Incorrect email or password."))
                    429 -> Result.failure(Exception("Too many attempts. Please wait and try again."))
                    else -> Result.failure(Exception("Sign-in failed. Please try again."))
                }
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Result.failure(Exception("Could not reach the server. Check your connection and try again."))
        }
    }

    override suspend fun register(email: String, password: String): Result<Unit> {
        return try {
            val displayName = email.trim().substringBefore('@').ifBlank { "User" }
            val response = apiService.register(
                RegisterRequest(email = email.trim(), password = password, displayName = displayName)
            )
            if (response.isSuccessful) {
                val body = response.body() ?: return Result.failure(Exception("Registration failed. Please try again."))
                // After registration, log in to get a token
                return login(email, password)
            } else {
                when (response.code()) {
                    409 -> Result.failure(Exception("An account already exists for this email."))
                    422 -> Result.failure(Exception("Invalid email address."))
                    else -> Result.failure(Exception("Registration failed. Please try again."))
                }
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Result.failure(Exception("Could not reach the server. Check your connection and try again."))
        }
    }

    override suspend fun logout() {
        securePreferences.remove(SecurePreferences.KEY_ACCESS_TOKEN)
        securePreferences.remove(KEY_DISPLAY_EMAIL)
        securePreferences.remove(KEY_PATIENT_UUID)
    }

    override fun isLoggedIn(): Boolean =
        securePreferences.getString(SecurePreferences.KEY_ACCESS_TOKEN)?.isNotBlank() == true

    override fun currentUserId(): String? =
        securePreferences.getString(SecurePreferences.KEY_PATIENT_ID)?.takeIf { it.isNotBlank() }

    override fun currentUserEmail(): String? =
        securePreferences.getString(KEY_DISPLAY_EMAIL)?.takeIf { it.isNotBlank() }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Stores the backend JWT and display email.
     * KEY_PATIENT_ID is stored by [LoginViewModel.finaliseAuth] after this returns.
     */
    private fun storeSession(accessToken: String, email: String) {
        securePreferences.putString(SecurePreferences.KEY_ACCESS_TOKEN, accessToken)
        securePreferences.putString(KEY_DISPLAY_EMAIL, email)
    }
}
