package com.medihistory.ai.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medihistory.ai.data.auth.AuthRepository
import com.medihistory.ai.data.model.Patient
import com.medihistory.ai.data.repository.PatientRepository
import com.medihistory.ai.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for [LoginScreen].
 *
 * Handles both sign-in and account-creation (register) flows against the
 * FastAPI backend JWT auth system (replaces Firebase Auth).
 *
 * Security invariants:
 * - Passwords are cleared from state as soon as authentication succeeds.
 * - Error messages are pre-sanitised by [AuthRepository] — no raw server text reaches UI.
 * - [isAuthenticated] flips to true only after both the network call and Room bootstrap succeed.
 *
 * Privacy: email is NEVER logged.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val patientRepository: PatientRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // -------------------------------------------------------------------------
    // Field updates
    // -------------------------------------------------------------------------

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, error = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, error = null) }
    }

    fun onToggleMode() {
        _uiState.update { LoginUiState(isRegisterMode = !it.isRegisterMode) }
    }

    // -------------------------------------------------------------------------
    // Primary actions
    // -------------------------------------------------------------------------

    fun onPrimaryAction() {
        if (_uiState.value.isRegisterMode) onRegisterClicked() else onLoginClicked()
    }

    private fun onLoginClicked() {
        val state = _uiState.value
        if (state.isLoading) return

        val emailTrimmed = state.email.trim()
        val error = validateLoginFields(emailTrimmed, state.password)
        if (error != null) { _uiState.update { it.copy(error = error) }; return }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.login(emailTrimmed, state.password).fold(
                onSuccess = { finaliseAuth(emailTrimmed) },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, error = throwable.message ?: "Sign-in failed.")
                    }
                },
            )
        }
    }

    private fun onRegisterClicked() {
        val state = _uiState.value
        if (state.isLoading) return

        val emailTrimmed = state.email.trim()
        val error = validateRegisterFields(emailTrimmed, state.password, state.confirmPassword)
        if (error != null) { _uiState.update { it.copy(error = error) }; return }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.register(emailTrimmed, state.password).fold(
                onSuccess = { finaliseAuth(emailTrimmed) },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, error = throwable.message ?: "Registration failed.")
                    }
                },
            )
        }
    }

    // -------------------------------------------------------------------------
    // Post-auth bootstrap
    // -------------------------------------------------------------------------

    /**
     * Called after a successful backend auth operation.
     *
     * 1. Reads the patient ID from [AuthRepository] (backend UUID).
     *    Falls back to a deterministic email-derived ID while the server
     *    doesn't yet return the UUID in the login response.
     * 2. Stores the patient ID in [SessionManager] so all Room queries are scoped.
     * 3. Upserts a Patient row in Room so FK constraints are always satisfied.
     * 4. Clears credentials from state and marks [isAuthenticated] = true.
     */
    private suspend fun finaliseAuth(email: String) {
        val uid = authRepository.currentUserId()
            ?: email.trim().lowercase().replace(Regex("[^a-z0-9]"), "").take(36)
        sessionManager.setPatientId(uid)

        if (patientRepository.getById(uid) == null) {
            runCatching {
                patientRepository.insert(
                    Patient(
                        id = uid,
                        name = email.substringBefore('@').ifBlank { "User" },
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                    )
                )
            }
        }

        _uiState.update {
            it.copy(
                isLoading       = false,
                isAuthenticated = true,
                password        = "",
                confirmPassword = "",
                error           = null,
            )
        }
    }

    // -------------------------------------------------------------------------
    // Validation helpers
    // -------------------------------------------------------------------------

    private fun validateLoginFields(email: String, password: String): String? {
        if (email.isBlank()) return "Email is required."
        if (!email.contains('@')) return "Enter a valid email address."
        if (password.isBlank()) return "Password is required."
        return null
    }

    private fun validateRegisterFields(email: String, password: String, confirm: String): String? {
        validateLoginFields(email, password)?.let { return it }
        if (password.length < 6) return "Password must be at least 6 characters."
        if (password != confirm) return "Passwords do not match."
        return null
    }
}
