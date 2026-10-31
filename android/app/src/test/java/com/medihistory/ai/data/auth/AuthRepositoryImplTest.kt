package com.medihistory.ai.data.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.inject.Provider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledThreadPoolExecutor

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AuthRepositoryImplTest {
    private lateinit var auth: FakeFirebaseAuth
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        auth = FakeFirebaseAuth()
        repository = AuthRepositoryImpl(auth)
    }

    @Test
    fun `login trims email and reports success`() =
        runTest {
            val result = repository.login("  user@example.test  ", "secret")

            assertTrue(result.isSuccess)
            assertEquals("user@example.test", auth.lastEmail)
            assertEquals("secret", auth.lastPassword)
        }

    @Test
    fun `login maps invalid credentials to friendly error`() =
        runTest {
            auth.signInTask =
                Tasks.forException(
                    FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "private detail"),
                )

            val result = repository.login("user@example.test", "wrong")

            assertEquals("Incorrect email or password.", result.exceptionOrNull()?.message)
        }

    @Test
    fun `login hides unexpected errors`() =
        runTest {
            auth.signInTask = Tasks.forException(IllegalStateException("private detail"))

            val result = repository.login("user@example.test", "secret")

            assertEquals("Sign-in failed. Please try again.", result.exceptionOrNull()?.message)
        }

    @Test
    fun `register trims email and reports success`() =
        runTest {
            val result = repository.register("  user@example.test  ", "secret")

            assertTrue(result.isSuccess)
            assertEquals("user@example.test", auth.lastEmail)
            assertEquals("secret", auth.lastPassword)
        }

    @Test
    fun `register maps known errors to friendly messages`() =
        runTest {
            auth.createUserTask =
                Tasks.forException(FirebaseAuthWeakPasswordException("ERROR_WEAK_PASSWORD", "private detail", ""))
            assertEquals(
                "Password is too weak. Use at least 6 characters.",
                repository.register("user@example.test", "short").exceptionOrNull()?.message,
            )

            auth.createUserTask =
                Tasks.forException(FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "private detail"))
            assertEquals(
                "An account already exists for this email.",
                repository.register("user@example.test", "secret").exceptionOrNull()?.message,
            )

            auth.createUserTask =
                Tasks.forException(
                    FirebaseAuthInvalidCredentialsException("ERROR_INVALID_EMAIL", "private detail"),
                )
            assertEquals(
                "Invalid email address.",
                repository.register("not-an-email", "secret").exceptionOrNull()?.message,
            )
        }

    @Test
    fun `register hides unexpected errors`() =
        runTest {
            auth.createUserTask = Tasks.forException(IllegalStateException("private detail"))

            val result = repository.register("user@example.test", "secret")

            assertEquals("Registration failed. Please try again.", result.exceptionOrNull()?.message)
        }

    @Test
    fun `logout signs out and current user values reflect auth state`() =
        runTest {
            assertFalse(repository.isLoggedIn())
            assertNull(repository.currentUserEmail())
            assertNull(repository.currentUserId())

            repository.logout()

            assertTrue(auth.signedOut)
        }

    @Test
    fun `auth module provides the initialized Firebase auth instance`() {
        assertNotNull(AuthModule.provideFirebaseAuth())
    }

    private class FakeFirebaseAuth :
        FirebaseAuth(
            firebaseApp(),
            Provider { null },
            Provider { null },
            DIRECT_EXECUTOR,
            DIRECT_EXECUTOR,
            DIRECT_EXECUTOR,
            ScheduledThreadPoolExecutor(0),
            DIRECT_EXECUTOR,
        ) {
        private var user: FirebaseUser? = null
        var signInTask: Task<AuthResult> = Tasks.forResult(null)
        var createUserTask: Task<AuthResult> = Tasks.forResult(null)
        var lastEmail: String? = null
        var lastPassword: String? = null
        var signedOut = false

        override fun signInWithEmailAndPassword(
            email: String,
            password: String,
        ): Task<AuthResult> {
            lastEmail = email
            lastPassword = password
            return signInTask
        }

        override fun createUserWithEmailAndPassword(
            email: String,
            password: String,
        ): Task<AuthResult> {
            lastEmail = email
            lastPassword = password
            return createUserTask
        }

        override fun getCurrentUser(): FirebaseUser? = user

        override fun signOut() {
            signedOut = true
            user = null
        }

        companion object {
            private val DIRECT_EXECUTOR = Executor { command -> command.run() }

            private fun firebaseApp(): FirebaseApp {
                val context: Context = ApplicationProvider.getApplicationContext()
                val existing = FirebaseApp.getApps(context).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
                return existing ?: FirebaseApp.initializeApp(
                    context,
                    FirebaseOptions.Builder()
                        .setApplicationId("1:1234567890:android:unittest")
                        .build(),
                )!!
            }
        }
    }
}
