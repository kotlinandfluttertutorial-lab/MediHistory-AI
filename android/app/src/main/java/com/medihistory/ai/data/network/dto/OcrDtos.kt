package com.medihistory.ai.data.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTOs for the server-side OCR endpoint.
 * Privacy: raw_value may contain prescription text — never log these objects.
 */

@JsonClass(generateAdapter = true)
data class OcrFieldDto(
    @Json(name = "field_name")       val fieldName: String,
    @Json(name = "raw_value")        val rawValue: String?,
    @Json(name = "confidence")       val confidence: Float,
    @Json(name = "source_reference") val sourceReference: String?,
)

@JsonClass(generateAdapter = true)
data class OcrFieldsResponse(
    @Json(name = "prescription_id") val prescriptionId: String,
    @Json(name = "fields")          val fields: List<OcrFieldDto>,
)
