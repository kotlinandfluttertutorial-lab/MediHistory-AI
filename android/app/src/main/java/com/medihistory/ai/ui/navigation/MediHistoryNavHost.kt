package com.medihistory.ai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.medihistory.ai.ui.camera.CameraScreen
import com.medihistory.ai.ui.dashboard.DashboardScreen
import com.medihistory.ai.ui.duplicate.DuplicateReviewScreen
import com.medihistory.ai.ui.history.MedicalHistoryScreen
import com.medihistory.ai.ui.medicine.MedicineDetailScreen
import com.medihistory.ai.ui.medicine.MedicineListScreen
import com.medihistory.ai.ui.more.MoreScreen
import com.medihistory.ai.ui.newdoctor.NewDoctorModeScreen
import com.medihistory.ai.ui.ocr.OcrReviewScreen
import com.medihistory.ai.ui.picker.ImportScreen
import com.medihistory.ai.ui.prescription.DoctorGroupScreen
import com.medihistory.ai.ui.prescription.PrescriptionDetailScreen
import com.medihistory.ai.ui.prescription.PrescriptionListScreen
import com.medihistory.ai.ui.privacy.PrivacyScreen
import com.medihistory.ai.ui.reminders.ReminderScreen
import com.medihistory.ai.ui.safety.SafetyCenterScreen

/**
 * Root NavHost for MediHistory AI.
 *
 * Wires all routes to their real screen composables. The bottom nav exposes
 * Dashboard, Prescriptions, Medicines, History, and More per the Figma design.
 * Secondary destinations (Safety Center, New Doctor Mode, Reminders, Privacy)
 * are accessible via the More hub.
 */
@Composable
fun MediHistoryNavHost(
    navController: NavHostController,
    patientId: String,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD,
        modifier = modifier,
    ) {
        // ── P1-01: Dashboard (previously a placeholder) ──────────────────────
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                patientId = patientId,
                onAddPrescription = { navController.navigate(Routes.IMPORT) },
                onViewDuplicates = { navController.navigate(Routes.DUPLICATE_ALERTS) },
                onViewHistory = { navController.navigate(Routes.MEDICAL_HISTORY) },
                onNewDoctorMode = { navController.navigate(Routes.NEW_DOCTOR_MODE) },
                onViewPrescriptions = { navController.navigate(Routes.PRESCRIPTIONS) },
            )
        }

        // ── Prescriptions list (was already wired) ───────────────────────────
        composable(Routes.PRESCRIPTIONS) {
            PrescriptionListScreen(
                onPrescriptionClick = { id -> navController.navigate(Routes.prescriptionDetail(id)) },
                onAddPrescription = { navController.navigate(Routes.IMPORT) },
            )
        }

        // ── P1-03: Prescription detail (was a placeholder) ───────────────────
        composable(
            route = Routes.PRESCRIPTION_DETAIL,
            arguments = listOf(navArgument("prescriptionId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("prescriptionId").orEmpty()
            PrescriptionDetailScreen(
                prescriptionId = id,
                patientId = patientId,
                onBack = { navController.navigateUp() },
            )
        }

        // ── Medicine explorer (was already wired) ────────────────────────────
        composable(Routes.MEDICINE_EXPLORER) {
            MedicineListScreen(
                onMedicineClick = { id -> navController.navigate(Routes.medicineDetail(id)) },
            )
        }

        // ── P1-04: Medicine detail (was a placeholder) ───────────────────────
        composable(
            route = Routes.MEDICINE_DETAIL,
            arguments = listOf(navArgument("medicineId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("medicineId").orEmpty()
            MedicineDetailScreen(
                medicineId = id,
                patientId = patientId,
                onBack = { navController.navigateUp() },
                onViewPrescription = { rxId -> navController.navigate(Routes.prescriptionDetail(rxId)) },
            )
        }

        // ── Duplicate alerts (was already wired) ─────────────────────────────
        composable(Routes.DUPLICATE_ALERTS) {
            DuplicateReviewScreen(
                patientId = patientId,
                onViewPrescription = { id -> navController.navigate(Routes.prescriptionDetail(id)) },
                onBack = { navController.navigateUp() },
            )
        }

        // ── P1-02: Medical History (was a placeholder) ───────────────────────
        composable(Routes.MEDICAL_HISTORY) {
            MedicalHistoryScreen(
                patientId = patientId,
                onViewPrescription = { rxId -> navController.navigate(Routes.prescriptionDetail(rxId)) },
                onBack = { navController.navigateUp() },
            )
        }

        // ── P1-05: OCR review (route existed but composable was absent) ──────
        composable(
            route = Routes.OCR_REVIEW,
            arguments = listOf(navArgument("prescriptionId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("prescriptionId").orEmpty()
            OcrReviewScreen(
                prescriptionId = id,
                onBack = { navController.navigateUp() },
                onAllReviewed = {
                    // All fields confirmed — go to the prescriptions list so the
                    // patient can see the updated card (doctor, date, medicines).
                    navController.navigate(Routes.PRESCRIPTIONS) {
                        popUpTo(Routes.PRESCRIPTIONS) { inclusive = true }
                    }
                },
            )
        }

        // ── New Doctor Mode ──────────────────────────────────────────────────
        composable(Routes.NEW_DOCTOR_MODE) {
            NewDoctorModeScreen(
                patientId = patientId,
                onBack = { navController.navigateUp() },
            )
        }

        // ── Camera ────────────────────────────────────────────────────────────
        composable(Routes.CAMERA) {
            CameraScreen(
                onImageConfirmed = { _, _ -> navController.navigateUp() },
                onDismiss = { navController.navigateUp() },
            )
        }

        // ── Import (file/photo picker) ────────────────────────────────────────
        composable(Routes.IMPORT) {
            ImportScreen(
                onFileImported = { prescriptionId ->
                    // OCR worker is already enqueued by ImportViewModel.
                    // Navigate directly to the review screen so the patient can
                    // confirm/edit the extracted fields as soon as they appear.
                    navController.navigate(Routes.ocrReview(prescriptionId)) {
                        popUpTo(Routes.IMPORT) { inclusive = true }
                    }
                },
                onDismiss = { navController.navigateUp() },
                onOpenCamera = { navController.navigate(Routes.CAMERA) },
            )
        }

        // ── Safety Center ────────────────────────────────────────────────────
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

        // ── Reminders ────────────────────────────────────────────────────────
        composable(Routes.REMINDERS) {
            ReminderScreen(
                patientId = patientId,
            )
        }

        // ── Privacy & Data ────────────────────────────────────────────────────
        composable(Routes.PRIVACY) {
            PrivacyScreen(
                patientId = patientId,
                onDataDeleted = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.navigateUp() },
            )
        }

        // ── P1-07: More hub ───────────────────────────────────────────────────
        composable(Routes.MORE) {
            MoreScreen(
                onNavigate = { route -> navController.navigate(route) },
                patientId = patientId,
            )
        }

        // ── P1-06: Doctor group screen (new) ─────────────────────────────────
        composable(
            route = Routes.DOCTOR_GROUP,
            arguments = listOf(navArgument("doctorId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId").orEmpty()
            DoctorGroupScreen(
                doctorId = doctorId,
                patientId = patientId,
                onBack = { navController.navigateUp() },
                onViewPrescription = { rxId -> navController.navigate(Routes.prescriptionDetail(rxId)) },
            )
        }
    }
}
