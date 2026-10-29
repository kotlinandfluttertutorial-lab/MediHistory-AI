package com.medihistory.ai.data.auth

/**
 * Contract for Firebase authentication operations.
 *
 * Implementations must NEVER log email addresses, passwords, or tokens.
 * Tokens are managed exclusively by FirebaseAuth — never stored manually.
 */
interface AuthRepository {
    /** Signs in an existing user with email and password. */
    suspend fun login(email: String, password: String): Result<Unit>

    /** Creates a new account with email and password. */
    suspend fun register(email: String, password: String): Result<Unit>

    /** Signs out the current user. */
    suspend fun logout()

    /** Returns true when a Firebase user is currently signed in. */
    fun isLoggedIn(): Boolean

    /**
     * Returns the email address of the signed-in user, or null.
     * Used only for display — never passed to backend as an auth credential.
     */
    fun currentUserEmail(): String?
}
