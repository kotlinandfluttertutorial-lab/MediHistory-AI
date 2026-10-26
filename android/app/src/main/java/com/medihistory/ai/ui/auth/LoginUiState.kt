package com.medihistory.ai.ui.auth

/**
 * UI state for the login screen.
 *
 * Security invariant: this data class MUST NOT contain any token field.
 * Tokens are managed exclusively by [com.medihistory.ai.data.auth.AuthRepository].
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
)
