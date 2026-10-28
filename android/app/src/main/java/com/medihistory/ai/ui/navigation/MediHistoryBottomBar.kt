package com.medihistory.ai.ui.navigation

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
 * The Prescriptions tab shows a badge when [badgeCounts] contains a positive value for
 * [Routes.PRESCRIPTIONS] (e.g. unreviewed prescription count).
 *
 * @param currentDestination      The currently active [NavDestination], or null if unknown.
 * @param onDestinationSelected   Callback invoked when the user taps a nav item.
 * @param badgeCounts             Map of route → badge count. Only counts > 0 show a badge.
 */
@Composable
fun MediHistoryBottomBar(
    currentDestination: NavDestination?,
    onDestinationSelected: (NavDestination) -> Unit,
    badgeCounts: Map<String, Int> = emptyMap(),
) {
    NavigationBar {
        NavDestination.bottomNavDestinations.forEach { destination ->
            val selected = destination.route == currentDestination?.route
            val badgeCount = badgeCounts[destination.route] ?: 0
            NavigationBarItem(
                selected = selected,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (badgeCount > 0) {
                                Badge { Text(text = "$badgeCount") }
                            }
                        },
                    ) {
                        Icon(
                            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = destination.contentDescription,
                        )
                    }
                },
                label = {
                    Text(text = stringResource(destination.labelRes))
                },
            )
        }
    }
}
