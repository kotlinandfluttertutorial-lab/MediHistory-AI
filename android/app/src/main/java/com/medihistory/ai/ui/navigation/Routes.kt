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
    const val DUPLICATE_DETAIL = "duplicate/{alertId}"
    const val OCR_REVIEW = "ocr_review/{prescriptionId}"

    fun prescriptionDetail(id: String) = "prescription/$id"
    fun medicineDetail(id: String) = "medicine/$id"
    fun duplicateDetail(alertId: String) = "duplicate/$alertId"
    fun ocrReview(id: String) = "ocr_review/$id"
}
