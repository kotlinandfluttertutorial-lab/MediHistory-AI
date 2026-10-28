package com.medihistory.ai.ui.navigation

object Routes {
    const val DASHBOARD = "dashboard"
    const val PRESCRIPTIONS = "prescriptions"
    const val MEDICINE_EXPLORER = "medicine_explorer"
    const val DUPLICATE_ALERTS = "duplicate_alerts"
    const val MEDICAL_HISTORY = "medical_history"
    const val REMINDERS = "reminders"
    const val IMPORT = "import"
    const val CAMERA = "camera"
    const val PRESCRIPTION_DETAIL = "prescription/{prescriptionId}"
    const val MEDICINE_DETAIL = "medicine/{medicineId}"
    const val DUPLICATE_DETAIL = "duplicate/{alertId}"
    const val OCR_REVIEW = "ocr_review/{prescriptionId}"

    /** New Doctor Mode — F07: record selection, consent, and PDF export. */
    const val NEW_DOCTOR_MODE = "new_doctor_mode"

    /** PDF preview shown after PDF generation (before share sheet is opened). */
    const val PDF_PREVIEW = "pdf_preview"
    const val SAFETY_CENTER = "safety_center/{patientId}"
    const val PRIVACY = "privacy"

    /** "More" disclosure hub — tab 5 of the bottom nav. */
    const val MORE = "more"

    /** Doctor-grouped prescription timeline. */
    const val DOCTOR_GROUP = "doctor_group/{doctorId}"

    fun prescriptionDetail(id: String) = "prescription/$id"
    fun medicineDetail(id: String) = "medicine/$id"
    fun duplicateDetail(alertId: String) = "duplicate/$alertId"
    fun ocrReview(id: String) = "ocr_review/$id"
    fun safetyCenter(patientId: String) = "safety_center/$patientId"
    fun doctorGroup(id: String) = "doctor_group/$id"
}
