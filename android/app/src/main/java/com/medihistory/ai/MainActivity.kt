package com.medihistory.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.medihistory.ai.data.auth.AuthRepository
import com.medihistory.ai.data.model.Patient
import com.medihistory.ai.data.repository.PatientRepository
import com.medihistory.ai.data.session.SessionManager
import com.medihistory.ai.ui.auth.LoginScreen
import com.medihistory.ai.ui.navigation.MediHistoryBottomBar
import com.medihistory.ai.ui.navigation.MediHistoryNavHost
import com.medihistory.ai.ui.navigation.NavDestination
import com.medihistory.ai.ui.theme.MediHistoryTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The single activity for MediHistory AI.
 *
 * Shows [LoginScreen] until the user is authenticated via Firebase.
 * Once authenticated (either by signing in now or because Firebase session is still valid),
 * bootstraps the patient record then shows the main NavHost.
 *
 * Session bootstrap happens in two places:
 *  1. Fresh login/register  — [LoginViewModel.finaliseAuth] writes the UID + Patient row.
 *  2. App restart (Firebase session already valid) — [LaunchedEffect] below writes the UID +
 *     Patient row if they are missing.  This handles the case where EncryptedSharedPreferences
 *     was cleared but FirebaseAuth still holds a valid token.
 *
 * Privacy: patient ID is never logged.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var patientRepository: PatientRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MediHistoryTheme {
                var isAuthenticated by rememberSaveable {
                    mutableStateOf(authRepository.isLoggedIn())
                }

                // ── Bootstrap session when Firebase is already signed in ────────
                // Runs once per composition when isAuthenticated is already true
                // (app restart with a live Firebase token).  Ensures the patient ID
                // and Patient row are present even if EncryptedSharedPreferences
                // was wiped since the last launch.
                LaunchedEffect(isAuthenticated) {
                    if (isAuthenticated) {
                        val uid = authRepository.currentUserId() ?: return@LaunchedEffect
                        // Persist the UID so SessionManager.getPatientId() returns it.
                        sessionManager.setPatientId(uid)
                        // Upsert the Patient row so FK constraints are always satisfied.
                        if (patientRepository.getById(uid) == null) {
                            val email = authRepository.currentUserEmail() ?: ""
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
                }

                if (!isAuthenticated) {
                    // ── Auth gate ────────────────────────────────────────────
                    LoginScreen(
                        onAuthSuccess = { isAuthenticated = true },
                    )
                } else {
                    // ── Main app ─────────────────────────────────────────────
                    val navController = rememberNavController()
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = backStackEntry?.destination?.route
                    val patientId = sessionManager.getPatientId()

                    val currentDestination = NavDestination.bottomNavDestinations
                        .firstOrNull { it.route == currentRoute }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            MediHistoryBottomBar(
                                currentDestination = currentDestination,
                                onDestinationSelected = { destination ->
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            )
                        },
                    ) { innerPadding ->
                        MediHistoryNavHost(
                            navController = navController,
                            patientId = patientId,
                            modifier = Modifier.padding(innerPadding),
                        )
                    }
                }
            }
        }
    }
}
