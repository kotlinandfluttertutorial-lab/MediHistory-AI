package com.medihistory.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.medihistory.ai.ui.navigation.MediHistoryBottomBar
import com.medihistory.ai.ui.navigation.MediHistoryNavHost
import com.medihistory.ai.ui.navigation.NavDestination
import com.medihistory.ai.ui.theme.MediHistoryTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The single activity for MediHistory AI.
 *
 * Hosts the NavController, Scaffold, bottom navigation bar, and NavHost.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MediHistoryTheme {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                // Resolve the active NavDestination for the bottom bar selection state.
                val currentDestination = NavDestination.bottomNavDestinations
                    .firstOrNull { it.route == currentRoute }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        MediHistoryBottomBar(
                            currentDestination = currentDestination,
                            onDestinationSelected = { destination ->
                                navController.navigate(destination.route) {
                                    // Pop to the start destination to avoid a deep back stack.
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
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}
