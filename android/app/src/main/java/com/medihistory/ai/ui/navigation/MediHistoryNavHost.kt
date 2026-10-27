package com.medihistory.ai.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.medihistory.ai.ui.duplicate.DuplicateReviewScreen
import com.medihistory.ai.ui.medicine.MedicineListScreen
import com.medihistory.ai.ui.picker.ImportScreen
import com.medihistory.ai.ui.prescription.PrescriptionListScreen
import com.medihistory.ai.ui.camera.CameraScreen
import com.medihistory.ai.ui.privacy.PrivacyScreen
import com.medihistory.ai.ui.safety.SafetyCenterScreen

/**
 * Root NavHost for MediHistory AI.
 *
 * Wires all 5 top-level destinations and the two detail routes. Each route
 * currently shows a [PlaceholderScreen] until the real screen composable is
 * implemented in its feature ticket.
 */
@Composable
fun MediHistoryNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD,
        modifier = modifier,
    ) {
        composable(Routes.DASHBOARD) {
            PlaceholderScreen(name = "Dashboard")
        }
        composable(Routes.PRESCRIPTIONS) {
            PrescriptionListScreen(
                onPrescriptionClick = { id -> navController.navigate(Routes.prescriptionDetail(id)) },
                onAddPrescription = {},
            )
        }
        composable(Routes.MEDICINE_EXPLORER) {
            MedicineListScreen(
                onMedicineClick = { id -> navController.navigate(Routes.medicineDetail(id)) },
            )
        }
        composable(Routes.DUPLICATE_ALERTS) {
            DuplicateReviewScreen(
                patientId = "current-patient",
                onViewPrescription = { id -> navController.navigate(Routes.prescriptionDetail(id)) },
                onBack = { navController.navigateUp() },
            )
        }
        composable(Routes.MEDICAL_HISTORY) {
            PlaceholderScreen(name = "Medical History")
        }
        composable(
            route = Routes.PRESCRIPTION_DETAIL,
            arguments = listOf(navArgument("prescriptionId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("prescriptionId").orEmpty()
            PlaceholderScreen(name = "Prescription Detail ($id)")
        }
        composable(
            route = Routes.MEDICINE_DETAIL,
            arguments = listOf(navArgument("medicineId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("medicineId").orEmpty()
            PlaceholderScreen(name = "Medicine Detail ($id)")
        }
        composable(Routes.CAMERA) {
            CameraScreen(
                onImageConfirmed = { _, _ -> navController.navigateUp() },
                onDismiss = { navController.navigateUp() },
            )
        }
        composable(Routes.IMPORT) {
            ImportScreen(
                onFileImported = { _, _, _ -> navController.navigateUp() },
                onDismiss = { navController.navigateUp() },
            )
        }
        composable(
            route = Routes.SAFETY_CENTER,
            arguments = listOf(navArgument("patientId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getString("patientId").orEmpty()
            SafetyCenterScreen(
                patientId = patientId,
                onViewDuplicates = { navController.navigate(Routes.DUPLICATE_ALERTS) },
                onBack = { navController.navigateUp() },
            )
        }
        composable(Routes.PRIVACY) {
            PrivacyScreen(
                patientId = "current-patient",
                onDataDeleted = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.navigateUp() },
            )
        }
    }
}

/** Temporary placeholder shown for routes not yet implemented. */
@Composable
private fun PlaceholderScreen(name: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        Text(text = name)
    }
}
