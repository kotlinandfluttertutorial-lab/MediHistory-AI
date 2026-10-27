package com.medihistory.ai.ui.navigation

object Routes {
    const val DASHBOARD = "dashboard"
    const val PRESCRIPTIONS = "prescriptions"
    const val MEDICINE_EXPLORER = "medicine_explorer"
    const val DUPLICATE_ALERTS = "duplicate_alerts"
    const val MEDICAL_HISTORY = "medical_history"
    const val IMPORT = "import"
    const val CAMERA = "camera"
    const val PRESCRIPTION_DETAIL = "prescription/{prescriptionId}"
    const val MEDICINE_DETAIL = "medicine/{medicineId}"
    const val CAMERA = "camera"

    fun prescriptionDetail(id: String) = "prescription/$id"
    fun medicineDetail(id: String) = "medicine/$id"
}
