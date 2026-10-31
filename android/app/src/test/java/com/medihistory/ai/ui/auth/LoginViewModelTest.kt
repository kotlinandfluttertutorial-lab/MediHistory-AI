package com.medihistory.ai.ui.auth

import com.medihistory.ai.data.auth.AuthRepository
import com.medihistory.ai.data.model.Patient
import com.medihistory.ai.data.repository.PatientRepository
import com.medihistory.ai.data.security.SecurePreferencesStore
import com.medihistory.ai.data.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
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

/**
 * Unit tests for [LoginViewModel].
 *
 * Uses a fake [AuthRepository] — no Firebase or network is accessed.
 * Synthetic credentials only (test@synthetic.medihistory / SyntheticTest123!).
 *
 * Naming convention: `given_[state]_when_[action]_then_[outcome]`
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var fakePatientRepository: FakePatientRepository
    private lateinit var fakeSessionManager: FakeSessionManager
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAuthRepository()
        fakePatientRepository = FakePatientRepository()
        fakeSessionManager = FakeSessionManager()
        viewModel = LoginViewModel(fakeRepository, fakePatientRepository, fakeSessionManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Sign-in flow
    // -------------------------------------------------------------------------

    @Test
    fun `given valid credentials when sign-in then isAuthenticated is true`() =
        runTest {
            viewModel.onEmailChange("test@synthetic.medihistory")
            viewModel.onPasswordChange("SyntheticTest123!")
            viewModel.onPrimaryAction()
            advanceUntilIdle()

            assertTrue(
                "isAuthenticated must be true after successful login",
                viewModel.uiState.value.isAuthenticated,
            )
        }

    @Test
    fun `given invalid credentials when sign-in then error is set and isAuthenticated is false`() =
        runTest {
            fakeRepository.loginResult = Result.failure(Exception("Incorrect email or password."))
            viewModel.onEmailChange("wrong@synthetic.medihistory")
            viewModel.onPasswordChange("WrongPassword!")
            viewModel.onPrimaryAction()
            advanceUntilIdle()

            assertFalse(
                "isAuthenticated must be false after failed login",
                viewModel.uiState.value.isAuthenticated,
            )
            assertNotNull(
                "error must not be null after failed login",
                viewModel.uiState.value.error,
            )
        }

    @Test
    fun `given login in progress when action triggered then isLoading is true then false`() =
        runTest {
            viewModel.onEmailChange("test@synthetic.medihistory")
            viewModel.onPasswordChange("SyntheticTest123!")
            viewModel.onPrimaryAction()

            assertTrue(
                "isLoading must be true while login coroutine is running",
                viewModel.uiState.value.isLoading,
            )

            advanceUntilIdle()

            assertFalse(
                "isLoading must be false after login completes",
                viewModel.uiState.value.isLoading,
            )
        }

    @Test
    fun `given blank email when sign-in then validation error is shown without network call`() =
        runTest {
            viewModel.onEmailChange("")
            viewModel.onPasswordChange("SyntheticTest123!")
            viewModel.onPrimaryAction()
            advanceUntilIdle()

            assertNotNull("error must be set for blank email", viewModel.uiState.value.error)
            assertFalse("isAuthenticated must stay false", viewModel.uiState.value.isAuthenticated)
            assertEquals("login must not be called", 0, fakeRepository.loginCallCount)
        }

    @Test
    fun `given blank password when sign-in then validation error is shown without network call`() =
        runTest {
            viewModel.onEmailChange("test@synthetic.medihistory")
            viewModel.onPasswordChange("")
            viewModel.onPrimaryAction()
            advanceUntilIdle()

            assertNotNull("error must be set for blank password", viewModel.uiState.value.error)
            assertEquals("login must not be called", 0, fakeRepository.loginCallCount)
        }

    @Test
    fun `given successful login then password field is cleared from state`() =
        runTest {
            viewModel.onEmailChange("test@synthetic.medihistory")
            viewModel.onPasswordChange("SyntheticTest123!")
            viewModel.onPrimaryAction()
            advanceUntilIdle()

            assertEquals(
                "Password must be cleared after successful login",
                "",
                viewModel.uiState.value.password,
            )
        }

    // -------------------------------------------------------------------------
    // Register flow
    // -------------------------------------------------------------------------

    @Test
    fun `given register mode when valid credentials then isAuthenticated is true`() =
        runTest {
            viewModel.onToggleMode() // switch to register
            viewModel.onEmailChange("new@synthetic.medihistory")
            viewModel.onPasswordChange("SyntheticTest123!")
            viewModel.onConfirmPasswordChange("SyntheticTest123!")
            viewModel.onPrimaryAction()
            advanceUntilIdle()

            assertTrue(
                "isAuthenticated must be true after successful registration",
                viewModel.uiState.value.isAuthenticated,
            )
        }

    @Test
    fun `given register mode when passwords do not match then error is shown`() =
        runTest {
            viewModel.onToggleMode()
            viewModel.onEmailChange("new@synthetic.medihistory")
            viewModel.onPasswordChange("SyntheticTest123!")
            viewModel.onConfirmPasswordChange("DifferentPassword!")
            viewModel.onPrimaryAction()
            advanceUntilIdle()

            assertNotNull("error must be set when passwords mismatch", viewModel.uiState.value.error)
            assertEquals("register must not be called", 0, fakeRepository.registerCallCount)
        }

    @Test
    fun `given register mode when password too short then error is shown`() =
        runTest {
            viewModel.onToggleMode()
            viewModel.onEmailChange("new@synthetic.medihistory")
            viewModel.onPasswordChange("abc")
            viewModel.onConfirmPasswordChange("abc")
            viewModel.onPrimaryAction()
            advanceUntilIdle()

            assertNotNull(
                "error must be set for password shorter than 6 chars",
                viewModel.uiState.value.error,
            )
            assertEquals("register must not be called", 0, fakeRepository.registerCallCount)
        }

    @Test
    fun `given toggle mode when called then fields are cleared`() {
        viewModel.onEmailChange("test@synthetic.medihistory")
        viewModel.onPasswordChange("SyntheticTest123!")
        viewModel.onToggleMode()

        assertEquals("email must be cleared on toggle", "", viewModel.uiState.value.email)
        assertEquals("password must be cleared on toggle", "", viewModel.uiState.value.password)
        assertNull("error must be null on toggle", viewModel.uiState.value.error)
    }

    // -------------------------------------------------------------------------
    // Security: LoginUiState must not contain token fields
    // -------------------------------------------------------------------------

    @Test
    fun `LoginUiState has no token field`() {
        val state = LoginUiState()
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertEquals("", state.confirmPassword)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertFalse(state.isAuthenticated)

        val fieldNames = LoginUiState::class.java.declaredFields.map { it.name }
        assertTrue(
            "LoginUiState must not contain a token field; found: $fieldNames",
            fieldNames.none { it.contains("token", ignoreCase = true) },
        )
    }
}

// ---------------------------------------------------------------------------
// Fake repository — synthetic data only; no Firebase or network access
// ---------------------------------------------------------------------------

private class FakeAuthRepository : AuthRepository {
    var loginResult: Result<Unit> = Result.success(Unit)
    var registerResult: Result<Unit> = Result.success(Unit)
    var loginCallCount = 0
    var registerCallCount = 0
    private var signedIn = false

    override suspend fun login(
        email: String,
        password: String,
    ): Result<Unit> {
        loginCallCount++
        if (loginResult.isSuccess) signedIn = true
        return loginResult
    }

    override suspend fun register(
        email: String,
        password: String,
    ): Result<Unit> {
        registerCallCount++
        if (registerResult.isSuccess) signedIn = true
        return registerResult
    }

    override suspend fun logout() {
        signedIn = false
    }

    override fun isLoggedIn(): Boolean = signedIn

    override fun currentUserEmail(): String? = if (signedIn) "test@synthetic.medihistory" else null

    override fun currentUserId(): String? = if (signedIn) "uid-synthetic-test-001" else null
}

private class FakePatientRepository : PatientRepository {
    private val _patients = MutableStateFlow<List<Patient>>(emptyList())

    override suspend fun getById(patientId: String): Patient? = _patients.value.find { it.id == patientId }

    override suspend fun insert(patient: Patient) {
        _patients.update { it + patient }
    }

    override suspend fun update(patient: Patient) {
        _patients.update { list -> list.map { if (it.id == patient.id) patient else it } }
    }

    override suspend fun delete(patient: Patient) {
        _patients.update { it.filter { p -> p.id != patient.id } }
    }

    override fun observeAll(): Flow<List<Patient>> = _patients
}

private class FakeSessionManager : SessionManager(
    securePreferences =
        object : SecurePreferencesStore {
            private val store = mutableMapOf<String, String>()

            override fun getString(key: String): String? = store[key]

            override fun putString(
                key: String,
                value: String,
            ) {
                store[key] = value
            }

            override fun remove(key: String) {
                store.remove(key)
            }

            override fun clear() {
                store.clear()
            }
        },
)
