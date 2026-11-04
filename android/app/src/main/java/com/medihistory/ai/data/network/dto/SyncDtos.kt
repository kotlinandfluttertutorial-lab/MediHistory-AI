package com.medihistory.ai.data.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTOs for sync endpoints.
 * Privacy: entity IDs and field values are never logged.
 */

@JsonClass(generateAdapter = true)
data class SyncPrescriptionDto(
    @Json(name = "id")             val id: String,
    @Json(name = "doctor_id")      val doctorId: String?,
    @Json(name = "prescription_date") val prescriptionDate: String?,
    @Json(name = "notes")          val notes: String?,
    @Json(name = "ocr_status")     val ocrStatus: String,
    @Json(name = "created_at")     val createdAt: Long,
    @Json(name = "updated_at")     val updatedAt: Long,
)

@JsonClass(generateAdapter = true)
data class SyncDoctorDto(
    @Json(name = "id")              val id: String,
    @Json(name = "name")            val name: String,
    @Json(name = "specialty")       val specialty: String,
    @Json(name = "specialty_other") val specialtyOther: String?,
    @Json(name = "clinic_name")     val clinicName: String?,
    @Json(name = "created_at")      val createdAt: Long,
    @Json(name = "updated_at")      val updatedAt: Long,
)

@JsonClass(generateAdapter = true)
data class SyncMedicineDto(
    @Json(name = "id")                   val id: String,
    @Json(name = "prescription_id")      val prescriptionId: String,
    @Json(name = "medicine_name")        val medicineName: String,
    @Json(name = "active_ingredient")    val activeIngredient: String?,
    @Json(name = "strength")             val strength: String?,
    @Json(name = "dosage_instructions")  val dosageInstructions: String?,
    @Json(name = "status")               val status: String,
    @Json(name = "created_at")           val createdAt: Long,
    @Json(name = "updated_at")           val updatedAt: Long,
)

@JsonClass(generateAdapter = true)
data class SyncPrescriptionsRequest(
    @Json(name = "prescriptions") val prescriptions: List<SyncPrescriptionDto>,
)

@JsonClass(generateAdapter = true)
data class SyncDoctorsRequest(
    @Json(name = "doctors") val doctors: List<SyncDoctorDto>,
)

@JsonClass(generateAdapter = true)
data class SyncMedicinesRequest(
    @Json(name = "medicines") val medicines: List<SyncMedicineDto>,
)

@JsonClass(generateAdapter = true)
data class SyncResponse(
    @Json(name = "synced")  val synced: Int,
    @Json(name = "skipped") val skipped: Int = 0,
)
