package com.medihistory.ai.data.auth

/**
 * Contract for authentication operations.
 *
 * Implementations must store tokens exclusively in EncryptedSharedPreferences.
 * Tokens must never be exposed in UiState or logs.
 */
interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun logout()
    fun isLoggedIn(): Boolean
    fun getAccessToken(): String?
}
