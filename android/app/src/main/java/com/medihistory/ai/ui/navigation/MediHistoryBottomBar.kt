package com.medihistory.ai.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Bottom navigation bar for MediHistory AI.
 *
 * Renders one [NavigationBarItem] per entry in [NavDestination.bottomNavDestinations].
 * The selected item shows its filled icon; unselected items show their outlined icon.
 *
 * @param currentDestination  The currently active [NavDestination], or null if unknown.
 * @param onDestinationSelected Callback invoked when the user taps a nav item.
 */
@Composable
fun MediHistoryBottomBar(
    currentDestination: NavDestination?,
    onDestinationSelected: (NavDestination) -> Unit,
) {
    NavigationBar {
        NavDestination.bottomNavDestinations.forEach { destination ->
            val selected = destination.route == currentDestination?.route
            NavigationBarItem(
                selected = selected,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = destination.contentDescription,
                    )
                },
                label = {
                    Text(text = stringResource(destination.labelRes))
                },
            )
        }
    }
}
