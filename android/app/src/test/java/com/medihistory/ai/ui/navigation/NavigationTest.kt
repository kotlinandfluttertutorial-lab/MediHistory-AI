package com.medihistory.ai.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure logic unit tests for navigation routing constants and [NavDestination].
 *
 * No Compose test rule is required — these tests exercise only plain Kotlin values.
 */
class NavigationTest {
    // -------------------------------------------------------------------------
    // Routes constants
    // -------------------------------------------------------------------------

    @Test
    fun `should return non-empty string for DASHBOARD route`() {
        assertTrue(Routes.DASHBOARD.isNotEmpty())
    }

    @Test
    fun `should return non-empty string for PRESCRIPTIONS route`() {
        assertTrue(Routes.PRESCRIPTIONS.isNotEmpty())
    }

    @Test
    fun `should return non-empty string for MEDICINE_EXPLORER route`() {
        assertTrue(Routes.MEDICINE_EXPLORER.isNotEmpty())
    }

    @Test
    fun `should return non-empty string for DUPLICATE_ALERTS route`() {
        assertTrue(Routes.DUPLICATE_ALERTS.isNotEmpty())
    }

    @Test
    fun `should return non-empty string for MEDICAL_HISTORY route`() {
        assertTrue(Routes.MEDICAL_HISTORY.isNotEmpty())
    }

    // -------------------------------------------------------------------------
    // Route builder functions
    // -------------------------------------------------------------------------

    @Test
    fun `should build correct prescription detail route for numeric id`() {
        assertEquals("prescription/123", Routes.prescriptionDetail("123"))
    }

    @Test
    fun `should build correct medicine detail route for string id`() {
        assertEquals("medicine/abc", Routes.medicineDetail("abc"))
    }

    // -------------------------------------------------------------------------
    // NavDestination.bottomNavDestinations
    // -------------------------------------------------------------------------

    @Test
    fun `should contain exactly 5 bottom nav destinations`() {
        assertEquals(5, NavDestination.bottomNavDestinations.size)
    }

    @Test
    fun `should have Dashboard as first bottom nav destination`() {
        assertEquals(Routes.DASHBOARD, NavDestination.bottomNavDestinations[0].route)
    }

    @Test
    fun `should have Prescriptions as second bottom nav destination`() {
        assertEquals(Routes.PRESCRIPTIONS, NavDestination.bottomNavDestinations[1].route)
    }

    @Test
    fun `should have MedicineExplorer as third bottom nav destination`() {
        assertEquals(Routes.MEDICINE_EXPLORER, NavDestination.bottomNavDestinations[2].route)
    }

    @Test
    fun `should have History as fourth bottom nav destination`() {
        assertEquals(Routes.MEDICAL_HISTORY, NavDestination.bottomNavDestinations[3].route)
    }

    @Test
    fun `should have More as fifth bottom nav destination`() {
        assertEquals(Routes.MORE, NavDestination.bottomNavDestinations[4].route)
    }

    @Test
    fun `should match Dashboard object route to DASHBOARD constant`() {
        assertEquals(Routes.DASHBOARD, NavDestination.Dashboard.route)
    }

    @Test
    fun `should match Prescriptions object route to PRESCRIPTIONS constant`() {
        assertEquals(Routes.PRESCRIPTIONS, NavDestination.Prescriptions.route)
    }

    @Test
    fun `should match MedicineExplorer object route to MEDICINE_EXPLORER constant`() {
        assertEquals(Routes.MEDICINE_EXPLORER, NavDestination.MedicineExplorer.route)
    }

    @Test
    fun `should match History object route to MEDICAL_HISTORY constant`() {
        assertEquals(Routes.MEDICAL_HISTORY, NavDestination.History.route)
    }

    @Test
    fun `should match More object route to MORE constant`() {
        assertEquals(Routes.MORE, NavDestination.More.route)
    }

    @Test
    fun `should have no duplicate routes among bottom nav destinations`() {
        val routes = NavDestination.bottomNavDestinations.map { it.route }
        assertEquals(routes.size, routes.toSet().size)
    }

    @Test
    fun `should have non-empty contentDescription on every bottom nav destination`() {
        NavDestination.bottomNavDestinations.forEach { destination ->
            assertFalse(
                "contentDescription is empty for route ${destination.route}",
                destination.contentDescription.isEmpty(),
            )
        }
    }
}
