package com.medihistory.ai.data.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTOs for POST /patients/me/prescriptions/{id}/process-review.
 * Privacy: field values are never logged.
 */

@JsonClass(generateAdapter = true)
data class ReviewedFieldDto(
    @Json(name = "field_name")    val fieldName: String,
    @Json(name = "raw_value")     val rawValue: String?,
    @Json(name = "patient_value") val patientValue: String?,
    @Json(name = "review_state")  val reviewState: String,
)

@JsonClass(generateAdapter = true)
data class ProcessReviewRequest(
    @Json(name = "reviewed_fields") val reviewedFields: List<ReviewedFieldDto>,
)

@JsonClass(generateAdapter = true)
data class ProcessReviewResponse(
    @Json(name = "prescription_id")  val prescriptionId: String,
    @Json(name = "ocr_status")       val ocrStatus: String,
    @Json(name = "doctor_id")        val doctorId: String?,
    @Json(name = "medicines_created") val medicinesCreated: Int,
    @Json(name = "alerts_created")   val alertsCreated: Int,
)
