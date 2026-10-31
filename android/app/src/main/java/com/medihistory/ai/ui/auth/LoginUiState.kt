package com.medihistory.ai.ui.auth

/**
 * UI state for the login / register screen.
 *
 * Security invariant: this data class MUST NOT contain any token, credential, or
 * Firebase user object. Auth state is managed exclusively by [FirebaseAuth].
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "", // only shown in register mode
    val isRegisterMode: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
)
