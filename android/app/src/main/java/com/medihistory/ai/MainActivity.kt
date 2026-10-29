package com.medihistory.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.medihistory.ai.data.auth.AuthRepository
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
 * Once authenticated, shows the main NavHost with bottom navigation.
 *
 * Auth gate logic:
 * - On cold launch, checks [AuthRepository.isLoggedIn] (FirebaseAuth.currentUser != null).
 * - If already signed in (e.g. app restarted), skips login immediately.
 * - After a successful login/register, transitions to the main app.
 *
 * Privacy: [patientId] is derived from [SessionManager] which reads from
 * EncryptedSharedPreferences — never logged.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MediHistoryTheme {
                // isAuthenticated drives the auth gate.
                // rememberSaveable survives configuration changes but not process death —
                // on cold start we re-check FirebaseAuth.currentUser below.
                var isAuthenticated by rememberSaveable {
                    mutableStateOf(authRepository.isLoggedIn())
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
