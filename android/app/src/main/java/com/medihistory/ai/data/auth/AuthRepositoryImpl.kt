package com.medihistory.ai.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

/**
 * Firebase Authentication implementation of [AuthRepository].
 *
 * Security invariants:
 * - Email and password are NEVER written to Logcat at any level.
 * - Tokens are managed exclusively by FirebaseAuth — we never store them manually.
 * - Error messages exposed to the UI are user-friendly strings — no raw exception text.
 * - Cross-patient exposure is prevented at the data layer (every Room query is patientId-scoped).
 *
 * Firebase SDK handles token refresh, persistence (via FirebaseAuth.getInstance), and
 * secure credential storage automatically. No EncryptedSharedPreferences needed for tokens.
 */
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
            Result.success(Unit)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Timber.w(e, "Sign-in failed: invalid credentials")
            Result.failure(Exception("Incorrect email or password."))
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.e(e, "Sign-in failed with unexpected error")
            Result.failure(Exception("Sign-in failed. Please try again."))
        }
    }

    override suspend fun register(email: String, password: String): Result<Unit> {
        return try {
            firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).await()
            Result.success(Unit)
        } catch (e: FirebaseAuthWeakPasswordException) {
            Timber.w(e, "Registration failed: password too weak")
            Result.failure(Exception("Password is too weak. Use at least 6 characters."))
        } catch (e: FirebaseAuthUserCollisionException) {
            Timber.w(e, "Registration failed: account collision")
            Result.failure(Exception("An account already exists for this email."))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Timber.w(e, "Registration failed: invalid credentials")
            Result.failure(Exception("Invalid email address."))
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Timber.e(e, "Registration failed with unexpected error")
            Result.failure(Exception("Registration failed. Please try again."))
        }
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
    }

    override fun isLoggedIn(): Boolean = firebaseAuth.currentUser != null

    override fun currentUserEmail(): String? = firebaseAuth.currentUser?.email

    override fun currentUserId(): String? = firebaseAuth.currentUser?.uid
}
