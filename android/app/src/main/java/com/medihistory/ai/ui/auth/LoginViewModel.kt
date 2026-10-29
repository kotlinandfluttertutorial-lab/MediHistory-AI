package com.medihistory.ai.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medihistory.ai.data.auth.AuthRepository
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
 * Handles both sign-in and account-creation (register) flows via Firebase Auth.
 *
 * Security invariants:
 * - Passwords are never stored beyond the in-memory [LoginUiState] needed to display
 *   the field. They are cleared when [isAuthenticated] becomes true.
 * - Error messages are pre-sanitised by [AuthRepository] — no raw Firebase exceptions reach UI.
 * - [isAuthenticated] flips to true only after a successful Firebase call; the screen
 *   should immediately navigate away and dispose this ViewModel.
 *
 * Privacy: email is NEVER logged.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
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

    /** Toggles between sign-in and register modes, clearing all fields and errors. */
    fun onToggleMode() {
        _uiState.update {
            LoginUiState(isRegisterMode = !it.isRegisterMode)
        }
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
        if (error != null) {
            _uiState.update { it.copy(error = error) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.login(emailTrimmed, state.password).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(isLoading = false, isAuthenticated = true, password = "", error = null)
                    }
                },
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
        if (error != null) {
            _uiState.update { it.copy(error = error) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.register(emailTrimmed, state.password).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(isLoading = false, isAuthenticated = true, password = "", confirmPassword = "", error = null)
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, error = throwable.message ?: "Registration failed.")
                    }
                },
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
        val base = validateLoginFields(email, password)
        if (base != null) return base
        if (password.length < 6) return "Password must be at least 6 characters."
        if (password != confirm) return "Passwords do not match."
        return null
    }
}
