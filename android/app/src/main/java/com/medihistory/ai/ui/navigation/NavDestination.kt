package com.medihistory.ai.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import com.medihistory.ai.R

/**
 * Represents each top-level destination that appears in the bottom navigation bar.
 *
 * @property route            NavHost route string from [Routes].
 * @property labelRes         String resource ID for the bottom nav label.
 * @property selectedIcon     Filled icon shown when this destination is selected.
 * @property unselectedIcon   Outlined icon shown when this destination is not selected.
 * @property contentDescription Accessibility description for the icon, including tab position.
 */
sealed class NavDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val contentDescription: String,
) {
    object Dashboard : NavDestination(
        route = Routes.DASHBOARD,
        labelRes = R.string.nav_dashboard,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        contentDescription = "Dashboard, tab 1 of 5",
    )

    object Prescriptions : NavDestination(
        route = Routes.PRESCRIPTIONS,
        labelRes = R.string.nav_prescriptions,
        selectedIcon = Icons.Filled.Description,
        unselectedIcon = Icons.Outlined.Description,
        contentDescription = "Prescriptions, tab 2 of 5",
    )

    object MedicineExplorer : NavDestination(
        route = Routes.MEDICINE_EXPLORER,
        labelRes = R.string.nav_medicines,
        selectedIcon = Icons.Filled.Medication,
        unselectedIcon = Icons.Outlined.Medication,
        contentDescription = "Medicine explorer, tab 3 of 5",
    )

    object DuplicateAlerts : NavDestination(
        route = Routes.DUPLICATE_ALERTS,
        labelRes = R.string.nav_alerts,
        selectedIcon = Icons.Filled.Warning,
        unselectedIcon = Icons.Outlined.Warning,
        contentDescription = "Duplicate alerts, tab 4 of 5",
    )

    object MedicalHistory : NavDestination(
        route = Routes.MEDICAL_HISTORY,
        labelRes = R.string.nav_history,
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        contentDescription = "Medical history, tab 5 of 5",
    )

    companion object {
        val bottomNavDestinations: List<NavDestination> = listOf(
            Dashboard,
            Prescriptions,
            MedicineExplorer,
            DuplicateAlerts,
            MedicalHistory,
        )
    }
}
