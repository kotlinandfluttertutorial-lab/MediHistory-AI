package com.medihistory.ai.data.auth

/**
 * Contract for authentication operations.
 *
 * Implementation bridges the Android app to the backend JWT auth system.
 * Tokens are stored exclusively in EncryptedSharedPreferences.
 * Passwords are NEVER stored or logged.
 * Email is used only as a display hint — never as an auth credential passed to third parties.
 */
interface AuthRepository {
    /** Signs in with the backend JWT system. */
    suspend fun login(email: String, password: String): Result<Unit>

    /** Creates a new backend account and signs in. */
    suspend fun register(email: String, password: String): Result<Unit>

    /** Clears the stored JWT and signs out. */
    suspend fun logout()

    /** Returns true when a valid backend JWT is stored. */
    fun isLoggedIn(): Boolean

    /**
     * Returns the stored patient ID (backend UUID), or null.
     * Set after successful login/register.
     */
    fun currentUserId(): String?

    /**
     * Returns the stored display email, or null.
     * Used only for greeting — never passed to the backend as a credential.
     */
    fun currentUserEmail(): String?
}
