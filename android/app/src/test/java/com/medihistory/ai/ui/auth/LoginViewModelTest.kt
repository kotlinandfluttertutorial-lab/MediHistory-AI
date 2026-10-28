package com.medihistory.ai.ui.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.medihistory.ai.data.auth.AuthRepository

/**
 * Unit tests for [LoginViewModel].
 *
 * Uses a fake [AuthRepository] — no real network or encrypted storage is accessed.
 * Synthetic credentials only (test@synthetic.medihistory / SyntheticTest123!).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAuthRepository()
        viewModel = LoginViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given valid credentials when login clicked then isLoggedIn is true`() = runTest {
        viewModel.onEmailChange("test@synthetic.medihistory")
        viewModel.onPasswordChange("SyntheticTest123!")
        viewModel.onLoginClicked()
        advanceUntilIdle()

        assertTrue(
            "isLoggedIn must be true after successful login",
            viewModel.uiState.value.isLoggedIn,
        )
    }

    @Test
    fun `given invalid credentials when login clicked then error is not null and isLoggedIn is false`() =
        runTest {
            fakeRepository.shouldSucceed = false
            viewModel.onEmailChange("wrong@synthetic.medihistory")
            viewModel.onPasswordChange("WrongPassword!")
            viewModel.onLoginClicked()
            advanceUntilIdle()

            assertFalse(
                "isLoggedIn must be false after failed login",
                viewModel.uiState.value.isLoggedIn,
            )
            assertNotNull(
                "error must not be null after failed login",
                viewModel.uiState.value.error,
            )
        }

    @Test
    fun `given login in progress when login clicked then isLoading goes true then false`() =
        runTest {
            viewModel.onEmailChange("test@synthetic.medihistory")
            viewModel.onPasswordChange("SyntheticTest123!")
            viewModel.onLoginClicked()

            // Before advancing: isLoading should be true
            assertTrue(
                "isLoading must be true while login is in progress",
                viewModel.uiState.value.isLoading,
            )

            advanceUntilIdle()

            // After completion: isLoading should be false
            assertFalse(
                "isLoading must be false after login completes",
                viewModel.uiState.value.isLoading,
            )
        }

    /**
     * Compile-time verification: [LoginUiState] has no token field.
     * If a token field is ever added, this test will fail to compile.
     */
    @Test
    fun `LoginUiState has no token field`() {
        val state = LoginUiState()
        // Accessing all declared properties — if a "token" property existed the
        // compiler would have added it here, causing a compile error.
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertFalse(state.isLoggedIn)

        // Verify via reflection that no token-related field exists.
        val fieldNames = LoginUiState::class.java.declaredFields.map { it.name }
        assertTrue(
            "LoginUiState must not contain a token field; found: $fieldNames",
            fieldNames.none { it.contains("token", ignoreCase = true) },
        )
    }
}

// ---------------------------------------------------------------------------
// Fake repository — synthetic data only; no network or disk access
// ---------------------------------------------------------------------------

private class FakeAuthRepository : AuthRepository {
    var shouldSucceed = true
    private var storedToken: String? = null

    override suspend fun login(email: String, password: String): Result<Unit> {
        return if (shouldSucceed) {
            storedToken = "synthetic-test-token"
            Result.success(Unit)
        } else {
            Result.failure(Exception("Invalid credentials"))
        }
    }

    override suspend fun logout() {
        storedToken = null
    }

    override fun isLoggedIn(): Boolean = storedToken != null

    override fun getAccessToken(): String? = storedToken
}
