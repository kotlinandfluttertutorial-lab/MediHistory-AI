package com.medihistory.ai.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Login / Register screen backed by Firebase Email+Password authentication.
 *
 * The same screen handles both sign-in and account creation — the patient switches
 * modes by tapping the toggle link at the bottom.
 *
 * Security:
 * - Passwords are cleared from state as soon as authentication succeeds.
 * - Error messages come from [AuthRepository] and never contain raw exception text.
 * - No credential is logged at any level.
 *
 * Accessibility:
 * - All fields and buttons have a minimum 48×48 dp touch target.
 * - Password fields carry a content description; the visibility toggle is labelled.
 * - Error text is part of the layout (not a snackbar) so TalkBack reads it in order.
 *
 * @param onAuthSuccess Called once the user is authenticated — navigate to the main app.
 * @param viewModel     Injected by Hilt; override in tests.
 */
@Composable
fun LoginScreen(
    onAuthSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    // Navigate as soon as Firebase reports success.
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) onAuthSuccess()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
        ) {
            // ── App icon + title ───────────────────────────────────────────────
            Icon(
                imageVector = Icons.Outlined.MedicalServices,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )
            Text(
                text = "MediHistory AI",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = if (uiState.isRegisterMode) "Create your account" else "Sign in to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Email field ────────────────────────────────────────────────────
            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("Email") },
                leadingIcon = {
                    Icon(Icons.Outlined.Email, contentDescription = null)
                },
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                keyboardActions =
                    KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                enabled = !uiState.isLoading,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Email address" },
            )

            // ── Password field ─────────────────────────────────────────────────
            PasswordField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = "Password",
                imeAction = if (uiState.isRegisterMode) ImeAction.Next else ImeAction.Done,
                onImeAction = {
                    if (uiState.isRegisterMode) {
                        focusManager.moveFocus(FocusDirection.Down)
                    } else {
                        focusManager.clearFocus()
                        viewModel.onPrimaryAction()
                    }
                },
                enabled = !uiState.isLoading,
            )

            // ── Confirm password (register mode only) ──────────────────────────
            AnimatedVisibility(
                visible = uiState.isRegisterMode,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                PasswordField(
                    value = uiState.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChange,
                    label = "Confirm password",
                    imeAction = ImeAction.Done,
                    onImeAction = {
                        focusManager.clearFocus()
                        viewModel.onPrimaryAction()
                    },
                    enabled = !uiState.isLoading,
                )
            }

            // ── Error message ──────────────────────────────────────────────────
            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Error: ${uiState.error}" },
                )
            }

            // ── Primary action button ─────────────────────────────────────────
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.onPrimaryAction()
                },
                enabled = !uiState.isLoading,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        text = if (uiState.isRegisterMode) "Create account" else "Sign in",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            // ── Toggle sign-in / register ─────────────────────────────────────
            TextButton(
                onClick = viewModel::onToggleMode,
                enabled = !uiState.isLoading,
                modifier =
                    Modifier.semantics {
                        contentDescription =
                            if (uiState.isRegisterMode) {
                                "Already have an account? Sign in"
                            } else {
                                "Don't have an account? Create one"
                            }
                    },
            ) {
                Text(
                    text =
                        if (uiState.isRegisterMode) {
                            "Already have an account? Sign in"
                        } else {
                            "Don't have an account? Create account"
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable password field with visibility toggle
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Password [OutlinedTextField] with a show/hide trailing icon.
 *
 * The visibility state is local — never persisted to the ViewModel.
 */
@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(Icons.Outlined.Lock, contentDescription = null)
        },
        trailingIcon = {
            val icon = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility
            val desc = if (passwordVisible) "Hide password" else "Show password"
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(icon, contentDescription = desc)
            }
        },
        singleLine = true,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = imeAction,
            ),
        keyboardActions =
            KeyboardActions(
                onNext = { onImeAction() },
                onDone = { onImeAction() },
            ),
        enabled = enabled,
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { contentDescription = label },
    )
}
